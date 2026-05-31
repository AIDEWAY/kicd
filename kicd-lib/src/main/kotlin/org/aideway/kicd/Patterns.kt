package org.aideway.kicd

val UnitSeparator = "\u001F"


fun String.decode(): List<String> {
    return this.split(UnitSeparator)
}

const val EndOrNonAlphaNumeric = """(?=[^A-Za-z0-9]|$)"""

const val CodeAlphaChar = "A-Z"

const val CodeNumericChar = "0-9A"

const val CodeAlphaNumericChar = "$CodeAlphaChar$CodeNumericChar"

const val CodePattern = """[$CodeAlphaChar][$CodeNumericChar]([$CodeAlphaNumericChar](\.([$CodeAlphaNumericChar]([$CodeAlphaNumericChar]([$CodeAlphaNumericChar]([$CodeAlphaNumericChar])?)?)?)?)?)?"""

const val CodeSeparatorPattern = """((\s+to)|((\s*,)?\s+(and|or))\s+|\s*(,)\s*)"""

val CodeSeparatorRegex = CodeSeparatorPattern.toRegex()

const val CodeRangePattern = """${CodePattern}(\s*-(\s*${CodePattern})?)?"""

const val CodeRangesPattern = """${CodeRangePattern}(${CodeSeparatorPattern}${CodeRangePattern})*"""

val CodeRangesRegex = CodeRangesPattern.toRegex()

const val QualifierStartKey = " with "

const val CodeQualifierPattern = """(-)?[A-Z0-9.]+"""

val CodeQualifierRegex = CodeQualifierPattern.toRegex()

const val CodeQualifierRangePattern = """${CodeQualifierPattern}(\s*-(\s*${CodeQualifierPattern})?)?"""

const val CodeQualifierRangesPattern = """$CodeQualifierRangePattern($CodeSeparatorPattern$CodeQualifierRangePattern)*$EndOrNonAlphaNumeric"""

val CodeQualifierRangesRegex = CodeQualifierRangesPattern.toRegex()

const val OrdinalPattern = """(1st|2nd|2cnd|3rd|4th|5th|6th|7th|8th|9th|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth)"""

val OrdinalRegex = OrdinalPattern.toRegex()
