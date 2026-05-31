package org.aideway.kicd

class CodeReference(
    val note: String,
    val ranges: CodeRanges?,
    val qualifier: CodeQualifier?,
) {

    fun findBillableCodes(note: IcdNode): List<Code> {
        var codes = if (ranges != null) {
            ranges.getBillableMatches(note)
        } else {
            note.getBillableCodes()
        }

        if (qualifier != null) {
            codes = qualifier.filterCodes(codes)
        }

        return codes
    }

    override fun toString(): String {
        return note
    }

    companion object {
        operator fun invoke(note: String): CodeReference {
            val note = note.trim()
            var indexStart = note.lastIndexOf('(')
            if (indexStart > 0) {
                val indexEnd = note.indexOf(')', indexStart)
                if (indexEnd > indexStart) {
                    var rangeText = note.substring(indexStart + 1, indexEnd)
                    var withIndex = rangeText.indexOf(QualifierStartKey)
                    val qualifierText = if (withIndex > 0) {
                        val withText = rangeText.substring(withIndex).trim()
                        rangeText = rangeText.substring(0, withIndex).trim()
                        withText
                    } else {
                        null
                    }

                    if (CodeRangesRegex.matches(rangeText)) {
                        return CodeReference(note, CodeRanges.parse(rangeText), qualifierText?.let { CodeQualifier.parse(it) })
                    }
                }
            }

            val matches = CodeRangesRegex.findAll(note).map { it.value }.toList()
            return if (matches.isEmpty()) {
                val withIndex = note.lastIndexOf(QualifierStartKey)
                val qualifier = if (withIndex > 0) {
                    try {
                        CodeQualifier.parse(note.substring(withIndex + QualifierStartKey.length))
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                CodeReference(note, null, qualifier)
            } else {
                val rangeText = matches[0]
                val withIndex = note.indexOf(rangeText) + rangeText.length
                val qualifier = if (note.startsWith(QualifierStartKey, withIndex)) {
                    val withText = note.substring(withIndex + QualifierStartKey.length).trim()
                    try {
                        CodeQualifier.parse(note.substring(withIndex + QualifierStartKey.length))
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                CodeReference(note, CodeRanges.parse(rangeText), qualifier)
            }
        }
    }
}
