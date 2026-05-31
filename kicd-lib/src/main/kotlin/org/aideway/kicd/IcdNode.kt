package org.aideway.kicd


fun String.toCodeChars(): CharArray {
    return replace(".", "").toCharArray()
}

class Root() : IcdNode(
    null,
    -1,
    "/",
    description = "The root of the ICD node tree.",
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
) {

    override val type = IcdNodeType.ROOT

    override val children: List<Chapter>
        get() = super.children as List<Chapter>

    fun billableCodes(): List<Code> {
        return flatten().filterIsInstance<Code>().filter { it.billable }
    }
}

class Chapter(
    root: Root,
    databaseId: Int,
    name: String,
    description: String,
    notes: List<String>,
    includes: List<String>,
    inclusionTerms: List<String>,
    excludes1: List<CodeReference>,
    excludes2: List<CodeReference>,
    codeFirst: List<CodeReference>,
    codeAlso: List<CodeReference>,
    useAdditionalCode: List<CodeReference>,
) : IcdNode(
    root,
    databaseId,
    name,
    description,
    notes,
    includes,
    inclusionTerms,
    excludes1,
    excludes2,
    codeFirst,
    codeAlso,
    useAdditionalCode,

    ) {

    override val type = IcdNodeType.CHAPTER

    override val parent: Root
        get() = super.parent as Root

    override val children: List<Section>
        get() = super.children as List<Section>

}

class Section(
    parent: Chapter,
    databaseId: Int,
    name: String,
    description: String,
    notes: List<String>,
    includes: List<String>,
    inclusionTerms: List<String>,
    excludes1: List<CodeReference>,
    excludes2: List<CodeReference>,
    codeFirst: List<CodeReference>,
    codeAlso: List<CodeReference>,
    useAdditionalCode: List<CodeReference>,
) : IcdNode(
    parent,
    databaseId,
    name,
    description,
    notes,
    includes,
    inclusionTerms,
    excludes1,
    excludes2,
    codeFirst,
    codeAlso,
    useAdditionalCode,

    ) {

    override val type = IcdNodeType.SECTION

    override val parent: Chapter
        get() = super.parent as Chapter

    override val children: List<Code>
        get() = super.children as List<Code>
}


class Code(
    parent: IcdNode,
    databaseId: Int,
    name: String,
    description: String,
    notes: List<String>,
    includes: List<String>,
    inclusionTerms: List<String>,
    excludes1: List<CodeReference>,
    excludes2: List<CodeReference>,
    codeFirst: List<CodeReference>,
    codeAlso: List<CodeReference>,
    useAdditionalCode: List<CodeReference>,
) : IcdNode(
    parent,
    databaseId,
    name,
    description,
    notes,
    includes,
    inclusionTerms,
    excludes1,
    excludes2,
    codeFirst,
    codeAlso,
    useAdditionalCode,

) {
    override val type = IcdNodeType.CODE

    val billable: Boolean
        get() = children.isEmpty()

}

sealed class IcdNode(
    private val _parent: IcdNode? = null,
    val databaseId: Int,
    val name: String,
    val description: String,
    val notes: List<String>,
    val includes: List<String>,
    val inclusionTerms: List<String>,
    val excludes1: List<CodeReference>,
    val excludes2: List<CodeReference>,
    val codeFirst: List<CodeReference>,
    val codeAlso: List<CodeReference>,
    val useAdditionalCode: List<CodeReference>,
) : Comparable<IcdNode> {

    abstract val type: IcdNodeType

    private val _children: MutableList<IcdNode> = mutableListOf()
    init {
        _parent?._children?.add(this)
    }

    open val parent: IcdNode?
        get() = _parent

    open val children: List<IcdNode>
        get() = _children

    fun findNode(name: String, type: IcdNodeType): IcdNode? {
        if (type == this.type && name == this.name) {
            return this
        }

        for (child in children) {
            if (child.type.id < type.id) {
                val node = child.findNode(name, type)
                if (node != null) return node
            } else if (child.type == type) {
                if (child.name == name) {
                    return child
                }
                if (name.startsWith(child.name)) {
                    val node = child.findNode(name, type)
                    if (node != null) return node
                }
            }
        }

        return null
    }

    fun flatten(): List<IcdNode> {
        return listOf(this) + _children.flatMap { it.flatten() }
    }

    fun getBillableCodes(): List<Code> {
        return flatten().filterIsInstance<Code>().filter { it.billable }
    }

    override fun toString(): String {
        return "${type.label} ($name)"
    }

    override fun compareTo(other: IcdNode): Int {
        return if (type != other.type) {
            type.compareTo(other.type)
        } else {
            name.compareTo(other.name)
        }
    }
}
