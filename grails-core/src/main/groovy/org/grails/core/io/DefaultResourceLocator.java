/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.core.io;

import grails.util.Environment;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.grails.io.support.GrailsResourceUtils;
import org.grails.plugins.BinaryGrailsPlugin;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.plugins.PluginManagerAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Default ResourceLocator implementation that doesn't take into account servlet loading.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class DefaultResourceLocator implements ResourceLocator, ResourceLoaderAware, PluginManagerAware {

    public static final String WILDCARD = "*";
    public static final String FILE_SEPARATOR = File.separator;
    public static final String CLOSURE_MARKER = "$";
    public static final String WEB_APP_DIR = "web-app";

    protected static final Resource NULL_RESOURCE = new ByteArrayResource("null".getBytes());

    protected PathMatchingResourcePatternResolver patchMatchingResolver;
    protected List<String> classSearchDirectories = new ArrayList<String>();
    protected List<String> resourceSearchDirectories = new ArrayList<String>();
    protected Map<String, Resource> classNameToResourceCache = new ConcurrentHashMap<String, Resource>();
    protected Map<String, Resource> uriToResourceCache = new ConcurrentHashMap<String, Resource>();
    protected ResourceLoader defaultResourceLoader =  new FileSystemResourceLoader();
    protected GrailsPluginManager pluginManager;
    protected boolean warDeployed = Environment.isWarDeployed();

    public void setSearchLocation(String searchLocation) {
        ResourceLoader resourceLoader = getDefaultResourceLoader();
        patchMatchingResolver = new PathMatchingResourcePatternResolver(resourceLoader);
        initializeForSearchLocation(searchLocation);
    }

    protected ResourceLoader getDefaultResourceLoader() {
        return defaultResourceLoader;
    }

    public void setSearchLocations(Collection<String> searchLocations) {
        patchMatchingResolver = new PathMatchingResourcePatternResolver(getDefaultResourceLoader());
        for (String searchLocation : searchLocations) {
            initializeForSearchLocation(searchLocation);
        }
    }

    private void initializeForSearchLocation(String searchLocation) {
        String searchLocationPlusSlash = searchLocation.endsWith("/") ? searchLocation : searchLocation + FILE_SEPARATOR;
        try {
            File[] directories = new File(searchLocationPlusSlash + GrailsResourceUtils.GRAILS_APP_DIR).listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory() && !file.isHidden();
                }
            });
            if (directories != null) {
                for (File directory : directories) {
                    classSearchDirectories.add(directory.getCanonicalPath());
                }
            }
        } catch (IOException e) {
            // ignore
        }

        classSearchDirectories.add(searchLocationPlusSlash + "src/java");
        classSearchDirectories.add(searchLocationPlusSlash + "src/groovy");
        resourceSearchDirectories.add(searchLocationPlusSlash);
    }

    public Resource findResourceForURI(String uri) {
        Resource resource = uriToResourceCache.get(uri);
        if (resource == null) {

            PluginResourceInfo info = inferPluginNameFromURI(uri);
            if (warDeployed) {
                Resource defaultResource = defaultResourceLoader.getResource(uri);
                if (defaultResource != null && defaultResource.exists()) {
                    resource = defaultResource;
                }
            }
            else {
                String uriWebAppRelative = WEB_APP_DIR + uri;

                for (String resourceSearchDirectory : resourceSearchDirectories) {
                    Resource res = resolveExceptionSafe(resourceSearchDirectory + uriWebAppRelative);
                    if (res.exists()) {
                        resource = res;
                    }
                    else if (!warDeployed) {
                        Resource dir = resolveExceptionSafe(resourceSearchDirectory);
                        if (dir.exists() && info != null) {
                            try {
                                String filename = dir.getFilename();
                                if (filename != null && filename.equals(info.pluginName)) {
                                    Resource pluginFile = dir.createRelative(WEB_APP_DIR + info.uri);
                                    if (pluginFile.exists()) {
                                        resource = pluginFile;
                                    }
                                }
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                }
            }

            if (resource == null && info != null) {
                resource = findResourceInBinaryPlugins(info);
            }

            if (resource == null || !resource.exists()) {
                Resource tmp = defaultResourceLoader != null ? defaultResourceLoader.getResource(uri) : null;
                if (tmp != null && tmp.exists()) {
                    resource = tmp;
                }
            }

            if (resource != null) {
                uriToResourceCache.put(uri, resource);
            }
            else if (warDeployed) {
                uriToResourceCache.put(uri, NULL_RESOURCE);
            }
        }
        return resource == NULL_RESOURCE ? null : resource;
    }

    protected Resource findResourceInBinaryPlugins(PluginResourceInfo info) {
        if (pluginManager != null) {
            String fullPluginName = info.pluginName;
            for (GrailsPlugin plugin : pluginManager.getAllPlugins()) {
                if (plugin.getFileSystemName().equals(fullPluginName) && (plugin instanceof BinaryGrailsPlugin)) {
                    return ((BinaryGrailsPlugin)plugin).getResource(info.uri);
                }
            }
        }
        return null;
    }

    private PluginResourceInfo inferPluginNameFromURI(String uri) {
        if (uri.startsWith("/plugins/")) {
            String withoutPluginsPath = uri.substring("/plugins/".length(), uri.length());
            int i = withoutPluginsPath.indexOf('/');
            if (i > -1) {
                PluginResourceInfo info = new PluginResourceInfo();
                info.pluginName = withoutPluginsPath.substring(0, i);
                info.uri = withoutPluginsPath.substring(i, withoutPluginsPath.length());
                return info;
            }
        }
        return null;
    }

    public Resource findResourceForClassName(String className) {

        if (className.contains(CLOSURE_MARKER)) {
            className = className.substring(0, className.indexOf(CLOSURE_MARKER));
        }
        Resource resource = classNameToResourceCache.get(className);
        if (resource == null) {
            String classNameWithPathSeparator = className.replace(".", FILE_SEPARATOR);
            for (String pathPattern : getSearchPatternForExtension(classNameWithPathSeparator, ".groovy", ".java")) {
                resource = resolveExceptionSafe(pathPattern);
                if (resource != null && resource.exists()) {
                    classNameToResourceCache.put(className, resource);
                    break;
                }
            }
        }
        return resource != null && resource.exists() ? resource : null;
    }

    private List<String> getSearchPatternForExtension(String classNameWithPathSeparator, String... extensions) {

        List<String> searchPatterns = new ArrayList<String>();
        for (String extension : extensions) {
            String filename = classNameWithPathSeparator + extension;
            for (String classSearchDirectory : classSearchDirectories) {
                searchPatterns.add(classSearchDirectory + FILE_SEPARATOR + filename);
            }
        }

        return searchPatterns;
    }

    private Resource resolveExceptionSafe(String pathPattern) {
        try {
            Resource[] resources = patchMatchingResolver.getResources("file:" + pathPattern);
            if (resources != null && resources.length > 0) {
                return resources[0];
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        defaultResourceLoader = resourceLoader;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    class PluginResourceInfo {
        String pluginName;
        String uri;
    }
}
