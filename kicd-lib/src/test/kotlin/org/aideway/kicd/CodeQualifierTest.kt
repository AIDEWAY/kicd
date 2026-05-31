package org.aideway.kicd

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CodeQualifierTest {

    @Test
    fun testWithValuesCreator() {
        var qualifier = ContainsQualifierCreator.createQualifierWith("with .14, .24, .94")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.141", "G45.243", "K23.941"), qualifier.filter(listOf("S34.141", "S34.16", "G45.243", "I23.201", "K23.941")))

        assertNull(ContainsQualifierCreator.createQualifierWith("with final characters .00 or .01"))

        qualifier = ContainsQualifierCreator.createQualifierWith("with .15, .25, .95")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.151", "G45.253", "K23.951"), qualifier.filter(listOf("S34.151", "S34.16", "G45.253", "I23.201", "K23.951")))

        qualifier = ContainsQualifierCreator.createQualifierWith("with .51-.52")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.514", "G45.521"), qualifier.filter(listOf("S34.514", "S34.16", "G45.521", "I23.201", "K23.951")))

        qualifier = ContainsQualifierCreator.createQualifierWith("with .62-")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.624"), qualifier.filter(listOf("S34.624")))

        qualifier = ContainsQualifierCreator.createQualifierWith("with .62-,.51-.52")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.624", "G45.521"), qualifier.filter(listOf("S34.624", "S34.16", "G45.521", "I23.201", "K23.951")))

    }

    @Test
    fun testWithFinalCharacters() {
        var qualifier = FinalCharactersQualifierCreator.createQualifierWith("with final characters -23")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.123", "G45.23"), qualifier.filter(listOf("S34.123", "S34.16", "G45.23", "I23.201", "K23.941")))

        qualifier = FinalCharactersQualifierCreator.createQualifierWith("with final characters .00 or .01")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.00", "G01.01"), qualifier.filter(listOf("S34.00", "S34.16", "G45.23", "I23.201", "G01.01")))

        qualifier = FinalCharactersQualifierCreator.createQualifierWith("with final character 6")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.06", "S34.16"), qualifier.filter(listOf("S34.06", "G45.23", "I23.201", "G01.01", "S34.16")))



        assertNull(FinalCharactersQualifierCreator.createQualifierWith("with .00, 01"))
    }

    @Test
    fun testWithNthCharacterCreator() {
        var qualifier = NthCharacterQualifierCreator.createQualifierWith("with seventh character A")
        assertNotNull(qualifier)
        assertEquals(listOf("G45.233A5", "K23.941A"), qualifier.filter(listOf("S34.123", "S34.16", "G45.233A5", "I23.201", "K23.941A")))

        qualifier = NthCharacterQualifierCreator.createQualifierWith("with fifth or sixth character 1-4 or 6")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.123", "G45.884", "G45.886A"), qualifier.filter(listOf("S34.123", "S34.1", "G45.884", "G45.885", "G45.886A")))

        qualifier = NthCharacterQualifierCreator.createQualifierWith("with 7th character D")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.123D"), qualifier.filter(listOf("S34.123D", "S34.1", "G45.884", "G45.885", "G45.886A")))

    }

    @Test
    fun testWithNthCharacterSequenceQualifier() {
        var qualifier = NthCharacterSequenceQualifierCreator.createQualifierWith("with fifth to sixth characters 51")
        assertNotNull(qualifier)
        assertEquals(listOf("S34.151"), qualifier.filter(listOf("S34.1251", "S34.151")))
    }

    @Test
    fun testInvalid() {
        assertNull(CodeQualifier.parse("with appropriate 7th character for subsequent encounter"))
    }

    @Test
    fun testPatterns() {
        assertTrue(CodeQualifierRangesRegex.matches("1-4 or 6"))
        assertTrue(CodeQualifierRangesRegex.matches(".62-"))
    }

    @Test
    fun testGetOrdinalIndex_allValues() {
        assertEquals(0, CodeQualifierCreator.getOrdinalIndex("1st"))
        assertEquals(0, CodeQualifierCreator.getOrdinalIndex("first"))
        assertEquals(1, CodeQualifierCreator.getOrdinalIndex("2nd"))
        assertEquals(1, CodeQualifierCreator.getOrdinalIndex("2cnd")) // handles typo in source data
        assertEquals(1, CodeQualifierCreator.getOrdinalIndex("second"))
        assertEquals(2, CodeQualifierCreator.getOrdinalIndex("3rd"))
        assertEquals(6, CodeQualifierCreator.getOrdinalIndex("7th"))
        assertEquals(6, CodeQualifierCreator.getOrdinalIndex("seventh"))
        assertNull(CodeQualifierCreator.getOrdinalIndex("unknown"))
    }

    @Test
    fun testNthCharacterQualifier_2ndCharacter() {
        val qualifier = NthCharacterQualifierCreator.createQualifierWith("with 2nd character 5")
        assertNotNull(qualifier, "Should parse 'with 2nd character 5' as a valid qualifier")

        val result = qualifier.filter(listOf("A50.1", "A60.1", "B50.2"))
        assertEquals(listOf("A50.1", "B50.2"), result,
            "Codes with '5' at 2nd position should match")
    }

    @Test
    fun testFilterCodesRetainsOrder() {
        val qualifier = ContainsQualifierCreator.createQualifierWith("with .14, .24, .94")
        assertNotNull(qualifier)

        val section = Section(
            Chapter(Root(), 1, "Ch1", "Chapter 1",
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            2, "S1", "Section 1",
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )

        val codes = listOf(
            Code(section, 10, "S34.141", "desc1", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            Code(section, 11, "S34.16", "desc2", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            Code(section, 12, "G45.243", "desc3", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            Code(section, 13, "K23.941", "desc4", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
        )

        val filtered = qualifier.filterCodes(codes)
        assertEquals(3, filtered.size)
        assertEquals("S34.141", filtered[0].name)
        assertEquals("G45.243", filtered[1].name)
        assertEquals("K23.941", filtered[2].name)
    }

    @Test
    fun testOpenFractureQualifier() {
        val qualifier = OpenFractureCodeCreatorQualifier.createQualifierWith("with open fracture 7th character")
        assertNotNull(qualifier)

        val codes = listOf("S52.001B", "S52.001A", "S52.001C", "S52.001D", "S52.001E")
        val filtered = qualifier.filter(codes)
        assertEquals(listOf("S52.001B", "S52.001C", "S52.001E"), filtered)
    }

    @Test
    fun testOpenFractureQualifier_codesTooShort() {
        val qualifier = OpenFractureCodeCreatorQualifier
        val codes = listOf("S52.00", "S52.0", "S52")
        val filtered = qualifier.filter(codes)
        assertEquals(emptyList(), filtered, "Short codes without 7th char should not match")
    }

    @Test
    fun testExpandAlphaNumericRange_singleChar() {
        val result = CodeQualifierCreator.expandAlphaNumericRange("A", "D")
        assertEquals(listOf("A", "B", "C", "D"), result)
    }

    @Test
    fun testExpandAlphaNumericRange_numeric() {
        val result = CodeQualifierCreator.expandAlphaNumericRange("1", "4")
        assertEquals(listOf("1", "2", "3", "4"), result)
    }

    @Test
    fun testExpandAlphaNumericRange_sameValue() {
        val result = CodeQualifierCreator.expandAlphaNumericRange("B", "B")
        assertEquals(listOf("B"), result)
    }

    @Test
    fun testCleanup_fixesPsychoactiveDrugTypo() {
        val result = CodeQualifierCreator.cleanup(".15. .25, .95")
        assertEquals(".15, .25, .95", result, "Should fix the period-space typo to comma-space")
    }

    @Test
    fun testCleanup_noChangeForCorrectInput() {
        val result = CodeQualifierCreator.cleanup(".15, .25, .95")
        assertEquals(".15, .25, .95", result)
    }

    @Test
    fun testGetOrdinalIndicesFrom_multipleOrdinals() {
        assertEquals(listOf(0), CodeQualifierCreator.getOrdinalIndicesFrom("1st"))
        assertEquals(listOf(0), CodeQualifierCreator.getOrdinalIndicesFrom("first"))
        assertEquals(listOf(1), CodeQualifierCreator.getOrdinalIndicesFrom("2nd"))
        assertEquals(listOf(2), CodeQualifierCreator.getOrdinalIndicesFrom("3rd"))
        assertEquals(listOf(5, 6), CodeQualifierCreator.getOrdinalIndicesFrom("6th or 7th"))
    }

    @Test
    fun testDoesCodeQualify() {
        val qualifier = ContainsQualifierCreator.createQualifierWith("with .14")
        assertNotNull(qualifier)
        assertTrue(qualifier.doesCodeQualify("S34.141"))
        assertFalse(qualifier.doesCodeQualify("S34.151"))
    }

    @Test
    fun testCodePattern_matchesStandardIcdCodes() {
        val regex = CodePattern.toRegex()
        assertTrue(regex.matches("A00"), "Simple 3-char code")
        assertTrue(regex.matches("A00.0"), "Code with one decimal digit")
        assertTrue(regex.matches("A00.01"), "Code with two decimal digits")
        assertTrue(regex.matches("S52.001B"), "Full 7-char code")
        assertFalse(regex.matches("00.1"), "Must start with letter")
    }

    @Test
    fun testCodeNumericChar_includesLetterA() {
        // CodeNumericChar = "0-9A" includes letter A in the character class
        // This means the code pattern allows 'A' in the second position
        val regex = CodePattern.toRegex()
        assertTrue(regex.matches("AA0"),
            "CodeNumericChar includes 'A', so 'AA0' matches CodePattern. " +
            "If this is unintentional, CodeNumericChar should be '0-9' not '0-9A'")
    }
}
