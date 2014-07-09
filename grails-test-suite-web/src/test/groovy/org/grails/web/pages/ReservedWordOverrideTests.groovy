package org.grails.web.pages

import org.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ReservedWordOverrideTests extends AbstractGrailsTagTests{

    void testCannotOverrideReservedWords() {
        request.setAttribute "foo", "bar"

        assertOutputEquals "bar", '${request.getAttribute("foo")}', [request:"bad"]
    }
}