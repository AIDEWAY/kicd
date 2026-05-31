package org.aideway.kicd

class CodeRanges(
    val values: List<Pair<String, String>>,
) {
    init {
        require(values.isNotEmpty()) { "Code ranges cannot be empty." }
        for ((from, to) in values) {
            if (from.isEmpty() || to.isEmpty()) {
                throw IllegalArgumentException("Invalid code range from: $from to: $to")
            }
        }
    }

    constructor(from: String, to: String) : this(listOf(from to to))

    operator fun plus(ranges: CodeRanges): CodeRanges {
        return CodeRanges(values + ranges.values)
    }

    fun isCodeBetween(code: String): Boolean {
        for ((from, to) in values) {
            val fromMatch = matchFrom(code, from)
            if (fromMatch == MatchType.MATCH) {
                val toMatch = matchTo(code, to)
                if (toMatch == MatchType.MATCH) {
                    return true
                }
            }
        }

        return false
    }

    fun getBillableMatches(codeNode: IcdNode, accumulator: MutableList<Code> = mutableListOf()): List<Code> {
        val code = codeNode.name
        if (code.isBlank()) {
            throw IllegalArgumentException("Invalid code node without name.")
        }

        var candidate = false
        for ((from, to) in values) {
            val fromMatchType = matchFrom(code, from)
            val toMatchType = matchTo(code, to)
            if (fromMatchType == MatchType.MATCH && toMatchType == MatchType.MATCH) {
                accumulator.addAll(codeNode.flatten().filterIsInstance<Code>().filter { it.billable })
                return accumulator
            } else if (fromMatchType != MatchType.NOT_CANDIDATE && toMatchType != MatchType.NOT_CANDIDATE) {
                candidate = true
            }
        }

        if (candidate) {
            for (childNode in codeNode.children) {
                getBillableMatches(childNode, accumulator)
            }
        }

        return accumulator
    }


    companion object {
        val primaryCancerCodes = CodeRanges("C00", "C96")
        val preCancerousCodes = CodeRanges("D00", "D09")

        fun parse(value: String): CodeRanges? {
            val values = value.split(CodeSeparatorRegex).map { it.trim() }.filter { it.isNotEmpty() }
            if (values.isEmpty()) {
                return null
            }

            var codeRanges: CodeRanges? = null
            for (value in values) {
                val value = value.replace(Regex("""[\s-]+"""), "-").removeSuffix(",").removeSuffix("-").removeSuffix(".")
                val index = value.indexOf('-')
                val codeRange = if (index > 0) {
                    val from = value.substring(0, index).trim().removeSuffix(".")
                    var to = value.substring(index + 1, value.length).trim().removeSuffix("-").removeSuffix(".")
                    if (to.isBlank()) {
                        to = from
                    }
                    CodeRanges(from, to)
                } else {
                    CodeRanges(value, value)
                }

                if (codeRanges == null) {
                    codeRanges = codeRange
                } else {
                    codeRanges += codeRange
                }
            }

            return codeRanges!!
        }

        fun matchFrom(code: String, fromRange: String): MatchType {
            return if (code.length < fromRange.length) {
                val fromRangeStart = fromRange.substring(0, code.length)
                if (code >= fromRangeStart) {
                    MatchType.CANDIDATE
                } else {
                    MatchType.NOT_CANDIDATE
                }
            } else {
                val codeStart = code.substring(0, fromRange.length)
                if (codeStart >= fromRange) {
                    MatchType.MATCH
                } else {
                    MatchType.NOT_CANDIDATE
                }
            }
        }

        fun matchTo(code: String, toRange: String): MatchType {
            return if (code.length < toRange.length) {
                val toRangeStart = toRange.substring(0, code.length)
                if (code <= toRangeStart) {
                    MatchType.CANDIDATE
                } else {
                    MatchType.NOT_CANDIDATE
                }
            } else {
                val codeStart = code.substring(0, toRange.length)
                if (codeStart <= toRange) {
                    MatchType.MATCH
                } else {
                    MatchType.NOT_CANDIDATE
                }
            }
        }
    }
}
