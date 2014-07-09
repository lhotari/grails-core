package org.grails.web.pages

import org.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TagLibWithNullValuesTests extends AbstractGrailsTagTests {

    protected void onSetUp() {
        gcl.parseClass('''
import grails.gsp.*

@TagLib
class MyTagLib {
  static namespace = 'my'

  Closure tag1 = { attrs ->
    out << out.getClass().name << ": [" << attrs.p1 << "] [" << attrs.p2 << "]"
  }

  Closure tag2 = { attrs ->
    out << my.tag1(p1: "abc")
  }
}
''')
    }

    void testNullValueHandling() {
        def template = '<p>This is tag1: <my:tag1 p1="abc"/></p>'
        assertOutputEquals '<p>This is tag1: GroovyPageOutputStack$GroovyPageProxyWriter: [abc] []</p>', template

        template = '<p>This is tag2: <my:tag2/></p>'
        assertOutputEquals '<p>This is tag2: GroovyPageOutputStack$GroovyPageProxyWriter: [abc] []</p>', template
    }
}
