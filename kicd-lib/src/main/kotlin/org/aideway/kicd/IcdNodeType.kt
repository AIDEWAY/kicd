package org.aideway.kicd

enum class IcdNodeType(val id: Byte, val label: String) {
    ROOT(-1, "Root"),
    CHAPTER(0, "Chapter"),
    SECTION(1, "Section"),
    CODE(2, "Code");

    companion object {
        fun fromId(id: Byte): IcdNodeType? {
            return entries.firstOrNull() { it.id == id }
        }
    }
}
