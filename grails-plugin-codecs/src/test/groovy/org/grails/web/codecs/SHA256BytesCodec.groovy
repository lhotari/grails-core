package org.grails.web.codecs

import org.grails.plugins.codecs.SHA256BytesCodec

class SHA256BytesCodecTests extends GroovyTestCase{

    def codec = new SHA256BytesCodec()

    void testEncode() {

        def expectedResult = [127, -125, -79, 101, 127, -15, -4, 83, -71, 45, -63, -127, 72, -95,
            -42, 93, -4, 45, 75, 31, -93, -42, 119, 40, 74, -35, -46, 0, 18, 109, -112, 105]

        // we want to verify that both array/list and String inputs work
        def primitiveResult = codec.encode([72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33])
        def toStringResult = codec.encode('Hello World!')

        assertEquals(expectedResult,primitiveResult.toList())
        assertEquals(expectedResult,toStringResult.toList())

        //make sure encoding null returns null
        assertNull codec.encode(null)
    }

    void testDecode() {
        shouldFail {
            codec.decode [1,2,3,4,5]
        }
    }
}
