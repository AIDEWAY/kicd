package org.aideway.kicd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeRangeTest {

    @Test
    fun test() {

    }

    @Test
    fun testIsBetween() {
        val primaryCancerCodes = CodeRanges("C00", "C96")
        assertTrue(primaryCancerCodes.isCodeBetween("C45.0"))
        assertFalse(primaryCancerCodes.isCodeBetween("H05.011"))
    }

    @Test
    fun testFromToMatch() {
        assertEquals(MatchType.MATCH, CodeRanges.matchFrom("B50.01", "B50.0"))
        assertEquals(MatchType.MATCH, CodeRanges.matchTo("B50.01", "B54"))

        assertEquals(MatchType.CANDIDATE, CodeRanges.matchFrom("B5", "B50.0"))
        assertEquals(MatchType.CANDIDATE, CodeRanges.matchTo("B5", "B54"))

        assertEquals(MatchType.NOT_CANDIDATE, CodeRanges.matchTo("B55", "B54"))
        assertEquals(MatchType.NOT_CANDIDATE, CodeRanges.matchTo("BA5", "B54"))

        assertEquals(MatchType.MATCH, CodeRanges.matchTo("B95.61", "B95.8"))
        assertEquals(MatchType.CANDIDATE, CodeRanges.matchFrom("B95.6", "B95.61"))
        assertEquals(MatchType.MATCH, CodeRanges.matchTo("B95.6", "B95.8"))
    }

    @Test
    fun testParse() {
        var range = CodeRanges.parse("P28.3- - P28.4-")
        assertNotNull(range)
        assertEquals(1, range.values.size)
        assertEquals("P28.3", range.values[0].first)
        assertEquals("P28.4", range.values[0].second)
    }

    @Test
    fun testExpression() {
        assertTrue(CodeRangesRegex.matches("Z16.-"))
        assertTrue(CodeRangesRegex.matches("T80.22-, T80.29-"))
        assertTrue(CodeRangesRegex.matches("C92.0-"))
    }

    @Test
    fun testCodeRanges_singleCode() {
        val range = CodeRanges.parse("Z16")
        assertNotNull(range)
        assertEquals(1, range.values.size)
        assertEquals("Z16", range.values[0].first)
        assertEquals("Z16", range.values[0].second)
    }

    @Test
    fun testCodeRanges_multipleRanges() {
        val range = CodeRanges.parse("C00-C96, D00-D09")
        assertNotNull(range)
        assertEquals(2, range.values.size)
        assertTrue(range.isCodeBetween("C45.0"))
        assertTrue(range.isCodeBetween("D05.1"))
        assertFalse(range.isCodeBetween("E10.0"))
    }

    @Test
    fun testCodeRanges_boundaryValues() {
        val range = CodeRanges("C00", "C96")
        assertTrue(range.isCodeBetween("C00"), "Start of range should match")
        assertTrue(range.isCodeBetween("C96"), "End of range should match")
        assertTrue(range.isCodeBetween("C00.0"), "Code extending start should match")
        assertTrue(range.isCodeBetween("C96.9"), "Code extending end should match")
        assertFalse(range.isCodeBetween("C97"), "Code after end should not match")
        assertFalse(range.isCodeBetween("B99"), "Code before start should not match")
    }

    @Test
    fun testGetBillableMatches_returnsOnlyBillable() {
        val root = TestHelp.db.root
        val range = CodeRanges("B90", "B94")
        val chapter = root.findNode("B90", IcdNodeType.CODE)
        assertNotNull(chapter, "B90 should exist in the database")

        val section = chapter.parent
        assertNotNull(section)

        val matches = range.getBillableMatches(section!!)
        assertTrue(matches.isNotEmpty(), "Should find billable codes in B90-B94 range")
        for (code in matches) {
            assertTrue(code.billable, "All matches should be billable")
            assertTrue(range.isCodeBetween(code.name),
                "Match ${code.name} should be within range B90-B94")
        }
    }

}
