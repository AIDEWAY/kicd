package org.aideway.kicd

import org.aideway.kicd.CodeQualifierCreator.Companion.cleanup
import org.aideway.kicd.CodeQualifierCreator.Companion.getCodeQualifiers
import org.aideway.kicd.CodeQualifierCreator.Companion.getOrdinalIndicesFrom

fun interface CodeQualifier {
    fun doesCodeQualify(code: String): Boolean {
        return filter(listOf(code)).isNotEmpty()
    }

    fun filterCodes(billableCodes: List<Code>): List<Code> {
        val filteredCodes = filter(billableCodes.map { it.name }).toSet()
        return billableCodes.filter { filteredCodes.contains(it.name) }
    }

    fun filter(billableCodes: List<String>): List<String>

    companion object {
        val creators by lazy {
            listOf(
                OpenFractureCodeCreatorQualifier,
                ContainsQualifierCreator,
                FinalCharactersQualifierCreator,
                NthCharacterQualifierCreator,
                NthCharacterSequenceQualifierCreator
            )
        }

        fun parse(withStatement: String): CodeQualifier? {
            val withStatement = cleanup(withStatement)

            for (creator in creators) {
                val qualifier = creator.createQualifierWith(withStatement)
                if (qualifier != null) {
                    return qualifier
                }
            }

            return null
        }


    }
}

interface CodeQualifierCreator {
    fun createQualifierWith(statement: String): CodeQualifier?

    companion object {

        // Cleanup typos from documents.
        fun cleanup(withStatement: String): String {
            var withStatement = withStatement.replaceFirst(".15. .25, .95", ".15, .25, .95") // F20 - psychoactive drug use (F11-F19 with .15. .25, .95)
            return withStatement
        }

        fun expandAlphaNumericRange(from: String, to: String): List<String> {
            require(from.length == to.length) { "from and to must have same length" }

            fun isAlpha(c: Char) = c in 'A'..'Z'
            fun isNumeric(c: Char) = c in '0'..'9'

            val fromChars = from.toCharArray()
            val toChars = to.toCharArray()
            val current = fromChars.copyOf()

            // Validate slot compatibility
            for (i in fromChars.indices) {
                when {
                    fromChars[i] == '.' && toChars[i] == '.' -> {}
                    isAlpha(fromChars[i]) && isAlpha(toChars[i]) -> {}
                    isNumeric(fromChars[i]) && isNumeric(toChars[i]) -> {}
                    else -> error("Invalid mixed types at position $i: ${fromChars[i]} vs ${toChars[i]}")
                }
            }

            fun increment(): Boolean {
                for (i in current.indices.reversed()) {
                    when {
                        current[i] == '.' -> continue

                        isNumeric(current[i]) -> {
                            if (current[i] < '9') {
                                current[i]++
                                return true
                            } else {
                                current[i] = '0'
                            }
                        }

                        isAlpha(current[i]) -> {
                            if (current[i] < 'Z') {
                                current[i] = (current[i].code + 1).toChar()
                                return true
                            } else {
                                current[i] = 'A'
                            }
                        }
                    }
                }
                return false
            }

            fun isBeyondTo(): Boolean {
                for (i in current.indices) {
                    if (current[i] == toChars[i]) continue
                    return current[i] > toChars[i]
                }
                return false
            }

            val result = mutableListOf<String>()

            while (true) {
                if (isBeyondTo()) break
                result.add(current.concatToString())
                if (!increment()) break
            }

            return result
        }

        fun getOrdinalIndicesFrom(text: String): List<Int> {
            val ordinalTexts = OrdinalRegex.findAll(text).map { it.value }.toList()
            return ordinalTexts.mapNotNull { getOrdinalIndex(it) }
        }

        fun getOrdinalIndex(ordinalText: String): Int? {
            return when (ordinalText.lowercase()) {
                "1st", "first" -> 0
                "2nd", "2cnd", "second" -> 1
                "3rd", "third" -> 2
                "4th", "fourth" -> 3
                "5th", "fifth" -> 4
                "6th", "sixth" -> 5
                "7th", "seventh" -> 6
                "8th", "eighth" -> 7
                "9th", "ninth" -> 8
                else -> null
            }
        }

        fun getCodeQualifiers(text: String): List<String> {
            val qualifierTexts = CodeQualifierRangesRegex.findAll(text).map { it.value }.toList()
            if (qualifierTexts.size == 1) {
                val qualifiers = qualifierTexts[0].split(CodeSeparatorRegex).map { it.trim().removePrefix("-").removeSuffix("-").trim() }.filter { it.isNotBlank() }
                val expandedQualifiers = mutableListOf<String>()
                for (qualifier in qualifiers) {
                    val index = qualifier.indexOf("-")
                    if (index > 0) {
                        val from = qualifier.substring(0, index)
                        val to = qualifier.substring(index + 1)
                        expandedQualifiers.addAll(expandAlphaNumericRange(from, to))
                    } else {
                        expandedQualifiers.add(qualifier)
                    }
                }
                return expandedQualifiers
            } else if (qualifierTexts.isEmpty()) {
                throw IllegalArgumentException("No code qualifiers found in: $text")
            } else {
                throw IllegalArgumentException("Multiple code qualifiers (${qualifierTexts.size}) found in: $text")
            }
        }
    }
}

// with .17, .27, .97
object ContainsQualifierCreator : CodeQualifierCreator {
    val regexs = listOf(
        "with $CodeQualifierRangesPattern".toRegex()
    )

    override fun createQualifierWith(statement: String): CodeQualifier? {

        for (regex in regexs) {
            if (regex.matches(statement)) {
                val codeQualifiers = getCodeQualifiers(statement)
                if (codeQualifiers.isEmpty()) {
                    throw IllegalStateException("No code qualifiers found matched statement: $statement")
                }

                return CodeQualifier { billableCodes: List<String> ->
                    billableCodes.filter { code ->
                        codeQualifiers.any { code.contains(it) }
                    }
                }

            }
        }

        return null
    }
}

// with final characters .00 or .01
// with final characters -21
object FinalCharactersQualifierCreator : CodeQualifierCreator {

    private val regexs = listOf(
        """with final\s+character(s)?\s+${CodeQualifierRangesPattern}""".toRegex(),
    )

    override fun createQualifierWith(statement: String): CodeQualifier? {
        for (regex in regexs) {
            if (regex.matches(statement)) {
                val codeQualifiers = getCodeQualifiers(statement)
                if (codeQualifiers.isEmpty()) {
                    throw IllegalStateException("No code qualifiers found matched statement: $statement")
                }

                return CodeQualifier { billableCodes: List<String> ->
                    billableCodes.filter { code ->
                        codeQualifiers.any { code.endsWith(it) }
                    }
                }

            }
        }

        return null
    }
}

// with 7th character B or C
// with fifth or sixth character 1-4 or 6
// with 5th character 9
object NthCharacterQualifierCreator : CodeQualifierCreator {

    private val regexs = listOf(
        """with ${OrdinalPattern}\s+character\s+${CodeQualifierRangesPattern}""".toRegex(),
        """with ${OrdinalPattern}-character\s+${CodeQualifierRangesPattern}""".toRegex(),
        """with ${OrdinalPattern}\s+or\s+${OrdinalPattern}\s+character\s+${CodeQualifierRangesPattern}""".toRegex(),
    )

    override fun createQualifierWith(statement: String): CodeQualifier? {

        for (regex in regexs) {
            if (regex.matches(statement)) {
                val ordinals = getOrdinalIndicesFrom(statement)
                if (ordinals.isEmpty()) {
                    throw IllegalStateException("Unable to get matched ordinal from text: $statement")

                }

                val codeQualifiers = getCodeQualifiers(statement)
                if (codeQualifiers.isEmpty()) {
                    throw IllegalStateException("Unable to get matched code qualifiers from text: $statement")
                }

                return CodeQualifier { billableCodes: List<String> ->
                    billableCodes.filter { code ->
                        var keepCode = false
                        val codeChars = code.toCodeChars()
                        for (codeQualifier in codeQualifiers) {
                            for (ordinal in ordinals) {
                                if (codeChars.getOrNull(ordinal)?.toString() == codeQualifier) {
                                    keepCode = true
                                    break
                                }
                            }
                            if (keepCode) {
                                break
                            }
                        }
                        keepCode
                    }
                }
            }
        }

        return null
    }
}

// with fifth to sixth characters 51
object NthCharacterSequenceQualifierCreator : CodeQualifierCreator {

    private val regexs = listOf(
        """with ${OrdinalPattern}\s+to\s+${OrdinalPattern}\s+character(s)?\s+${CodeQualifierRangesPattern}""".toRegex(),
    )

    override fun createQualifierWith(statement: String): CodeQualifier? {

        for (regex in regexs) {
            if (regex.matches(statement)) {
                val ordinals = getOrdinalIndicesFrom(statement)
                if (ordinals.isEmpty()) {
                    throw IllegalStateException("Unable to get matched ordinal from text: $statement")
                } else if (ordinals.size != 2) {
                    throw IllegalStateException("Invalid number of ordinals (${ordinals.size}) from text: $statement")
                }

                val from = ordinals[0]
                val to = ordinals[1]

                if (from >= to) {
                    throw IllegalArgumentException("Invalid ordinal values. From greater equal to in statement: $statement")
                }


                val codeQualifiers = getCodeQualifiers(statement)
                if (codeQualifiers.isEmpty()) {
                    throw IllegalStateException("Unable to get matched code qualifiers from text: $statement")
                }

                return CodeQualifier { billableCodes: List<String> ->
                    billableCodes.filter { code ->
                        var keepCode = false
                        val codeChars = String(code.toCodeChars())
                        if (codeChars.length > to) {
                            val codeSequence = codeChars.substring(from, to + 1)
                            for (codeQualifier in codeQualifiers) {
                                if (codeSequence == codeQualifier) {
                                    keepCode = true
                                    break
                                }
                            }
                        }
                        keepCode
                    }
                }
            }
        }

        return null
    }
}

// with open fracture 7th character
object OpenFractureCodeCreatorQualifier : CodeQualifierCreator, CodeQualifier {

    // Specific 7th characters for open fractures:
    // B, C: Initial encounters
    // E, F: Subsequent (Routine healing)
    // H, J: Subsequent (Delayed healing)
    // M, N: Subsequent (Nonunion)
    // Q, R: Subsequent (Malunion)
    val openFractureChars = setOf('B', 'C', 'E', 'F', 'H', 'J', 'M', 'N', 'Q', 'R')

    override fun createQualifierWith(statement: String): CodeQualifier? {
        return if (statement == "with open fracture 7th character") {
            this
        } else {
            null
        }
    }

    override fun filter(billableCodes: List<String>): List<String> {
        return billableCodes.filter {  code ->
            val codeChars = code.toCodeChars()
            openFractureChars.contains(codeChars.getOrNull(6))
        }
    }
}
