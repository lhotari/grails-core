package org.grails.web.servlet.filter

import grails.util.GrailsWebUtil
import grails.util.Holders

import javax.servlet.Filter
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import grails.core.ApplicationAttributes
import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.plugins.GrailsPluginManager
import org.grails.plugins.MockGrailsPluginManager
import org.grails.support.MockApplicationContext
import org.grails.web.servlet.context.GrailsConfigUtils
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext

 /**
 * Abstract test case to make testing servlet filters easier.
 */
abstract class AbstractServletFilterTests extends GroovyTestCase {
    GroovyClassLoader     gcl
    ServletContext        servletContext
    GrailsWebRequest      webRequest
    HttpServletRequest    request
    HttpServletResponse   response
    WebApplicationContext appCtx
    GrailsApplication     application
    GrailsPluginManager   pluginManager
    def evaluator
    def filter

    protected void setUp() {
        super.setUp()

        servletContext = new MockServletContext()
        webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        response = webRequest.currentResponse
        appCtx = new MockApplicationContext()

        evaluator = new DefaultUrlMappingEvaluator()

        // Mimic AbstractGrailsPluginTests: create a new class loader
        // and allow sub-classes to use for parsing and loading classes.
        gcl = new GroovyClassLoader()
        onSetup()
    }

    void tearDown() {
        Holders.clear()
    }

    protected void onSetup() {}

    /**
     * Initialise the given filter so that it can tested.
     */
    protected final void initFilter(Filter filter) {
        filter.init(new MockFilterConfig(servletContext))
    }

    /**
     * Set up the mock parent application context and bind it to the
     * servlet context.
     */
    protected final void bindApplicationContext() {
        servletContext.setAttribute(ApplicationAttributes.PARENT_APPLICATION_CONTEXT, appCtx)
    }

    /**
     * Set up the Grails application and bind it to the servlet context.
     */
    protected final void bindGrailsApplication() {
        // Create a new Grails application with the stored Groovy class loader.
        application = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        pluginManager = new MockGrailsPluginManager(application)

        // Register the application instance and plugin manager with
        // the mock application context.
        appCtx.registerMockBean(GrailsApplication.APPLICATION_ID, application)
        appCtx.registerMockBean(GrailsPluginManager.BEAN_NAME, pluginManager)

        // Configure everything as if it's a running app.
        GrailsConfigUtils.configureWebApplicationContext(servletContext, appCtx)
    }
}
