package org.grails.web.pages

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.grails.web.pages.GroovyPagesMetaUtils
import org.springframework.web.context.request.RequestContextHolder

class GroovyPageMethodDispatchWithNamespaceTests extends AbstractGrailsControllerTests {

    void onSetUp() {
        gcl.parseClass(
"""
import org.codehaus.groovy.grails.web.taglib.*
import grails.gsp.*

class TestController {
    def index = {}
}

@TagLib
class Test1TagLib {
    static namespace = "t1"
    Closure tag = { attrs, body ->

        out << "print"

        def result = g.tag2(test:'blah')
        out << result

        result = body()

        out << result
    }
}

@TagLib
class Test2TagLib {
    Closure tag2 = { attrs, body -> out << attrs.test }
}
class MyPage extends GroovyPage {
    String getGroovyPageFileName() { "test" }
    def run() {
        setBodyClosure(1) {
            out << "foo"
            ""
        }
        invokeTag("tag", "t1", -1, [attr1:"test"], 1)
        out << "hello" + tag2(test:"test2", new GroovyPageTagBody(this, webRequest, {

        }))
    }
}
""")
    }

    void testGroovyPage() {
        runTest {
            def webRequest = RequestContextHolder.currentRequestAttributes()
            def script = gcl.loadClass("MyPage").newInstance()
            GroovyPagesMetaUtils.registerMethodMissingForGSP(script.getClass(), appCtx.getBean('gspTagLibraryLookup'))
            script.setJspTagLibraryResolver(appCtx.getBean('jspTagLibraryResolver'))
            script.setGspTagLibraryLookup(appCtx.getBean('gspTagLibraryLookup'))

            def controller = ga.getControllerClass("TestController").newInstance()
            def sw = new StringWriter()
            webRequest.out =  new PrintWriter(sw)
            def b = new Binding(application:controller.servletContext,
                                request:controller.request,
                                response:controller.response,
                                flash:controller.flash,
                                out: webRequest.out ,
                                webRequest:webRequest)
            script.binding = b
            script.initRun(webRequest.out, webRequest, null)
            script.run()
            script.cleanup()

            assertEquals "printblahfoohellotest2",sw.toString()
        }
    }
}
