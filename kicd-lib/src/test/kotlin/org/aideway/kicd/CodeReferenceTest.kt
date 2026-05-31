package org.aideway.kicd

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeReferenceTest {
    @Test
    fun test() {
        val range = CodeReference("open skull fracture (S02.- with 7th character B)")
        assertNotNull(range)
    }

    @Test
    fun testCodeReference_rangeWithQualifier_rangeIsTruncated() {
        val ref = CodeReference("Excludes1 (C00-C96 with 7th character D)")

        assertNotNull(ref.ranges, "Should have parsed code ranges from parenthesized text")
        assertNotNull(ref.qualifier, "Should have parsed qualifier from 'with' clause")

        assertEquals(1, ref.ranges!!.values.size, "Should have exactly one range pair")
        assertEquals("C00", ref.ranges!!.values[0].first)
        assertEquals("C96", ref.ranges!!.values[0].second)
    }

    @Test
    fun testCodeReference_multipleParentheses_findsCorrectRange() {
        // lastIndexOf('(') finds '(see note)' instead of '(C00-C96)', but falls through
        // to the regex-based path which still finds C00-C96 in the full text.
        val ref = CodeReference("Excludes1 (C00-C96) additional info (see note)")

        assertNotNull(ref.ranges, "Should parse C00-C96 as a range")
        assertTrue(ref.ranges!!.isCodeBetween("C45.0"),
            "C45.0 should be within the C00-C96 range")
    }
}
