package org.codehaus.groovy.grails.plugins;

import grails.plugins.DefaultGrailsPluginManager;
import grails.plugins.GrailsPlugin;
import groovy.lang.GroovyClassLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import grails.core.DefaultGrailsApplication;
import grails.core.GrailsApplication;
import org.grails.plugins.*;
import org.grails.web.servlet.context.support.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests;
import org.grails.support.MockApplicationContext;

@SuppressWarnings({"unchecked","rawtypes"})
public class PluginFilterTests extends AbstractGrailsMockTests {

    public void testIncluding() {
        List pluginList = getPluginList();

        HashSet set = new HashSet();
        set.add("one");
        set.add("four");
        IncludingPluginFilter filter = new IncludingPluginFilter(set);

        List filtered = filter.filterPluginList(pluginList);
        assertEquals(4, filtered.size());
        assertEquals("one", nameFor(filtered, 0));
        assertEquals("four", nameFor(filtered, 1));
        assertEquals("two", nameFor(filtered, 2));
        assertEquals("three", nameFor(filtered, 3));

        set = new HashSet();
        set.add("five");
        set.add("one");
        filter = new IncludingPluginFilter(set);

        filtered = filter.filterPluginList(pluginList);
        assertEquals(3, filtered.size());
        assertEquals("one", nameFor(filtered, 0));
        assertEquals("five", nameFor(filtered, 1));
        assertEquals("three", nameFor(filtered, 2));
    }

    public void testExcluding() {
        List pluginList = getPluginList();

        HashSet set = new HashSet();
        set.add("three");
        ExcludingPluginFilter filter = new ExcludingPluginFilter(set);

        List filtered = filter.filterPluginList(pluginList);
        assertEquals(2, filtered.size());

        //three supports four and five, so they are gone too
        assertEquals("one", nameFor(filtered, 0));
        assertEquals("two", nameFor(filtered, 1));

        set = new HashSet();
        set.add("one");
        set.add("two");
        filter = new ExcludingPluginFilter(set);

        filtered = filter.filterPluginList(pluginList);
        assertEquals(2, filtered.size());

        //four depends on two, so that's gone too
        assertEquals("three", nameFor(filtered, 0));
        assertEquals("five", nameFor(filtered, 1));
    }

    public void testIdentity() {
        List pluginList = getPluginList();
        assertEquals(new IdentityPluginFilter().filterPluginList(pluginList), pluginList);
    }

    private String nameFor(List filtered, int index) {
        GrailsPlugin object = (GrailsPlugin) filtered.get(index);
        return object.getName();
    }

    private List getPluginList() {

        GroovyClassLoader gcl = new GroovyClassLoader();

        GrailsApplication app = new DefaultGrailsApplication(new Class[0], gcl);
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, app);

        GrailsRuntimeConfigurator conf = new GrailsRuntimeConfigurator(app,
                parent);
        DefaultGrailsPluginManager manager = new DefaultGrailsPluginManager(
                new Class[0], app);
        manager.setParentApplicationContext(parent);
        parent.registerMockBean("manager", manager);
        conf.setPluginManager(manager);

        List pluginList = new ArrayList();

        addPlugin(gcl, app, pluginList, "OneGrailsPlugin", null);
        addPlugin(gcl, app, pluginList, "TwoGrailsPlugin", "[one: 1.1]");
        addPlugin(gcl, app, pluginList, "ThreeGrailsPlugin", null);
        addPlugin(gcl, app, pluginList, "FourGrailsPlugin",
                "[two: 1.1, three: 1.1]");
        addPlugin(gcl, app, pluginList, "FiveGrailsPlugin", "[three: 1.1]");
        return pluginList;
    }

    private void addPlugin(GroovyClassLoader gcl, GrailsApplication app,
            List pluginList, String className, String dependencies) {
        Class c = gcl.parseClass("class "
                + className
                + " {\n"
                + "def version = 1.1;\n"
                + (dependencies != null ? "def dependsOn = " + dependencies
                        + ";\n" : "")
                + "def doWithSpring = {" + "}\n"
                + "def doWithApplicationContext = { ctx ->" + "}" + "}");

        DefaultGrailsPlugin p = new DefaultGrailsPlugin(c, app);
        pluginList.add(p);
    }
}
