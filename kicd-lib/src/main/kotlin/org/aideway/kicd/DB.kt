package org.aideway.kicd

import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.use


fun main(args: Array<String>) {
    val sqliteFilePath = if (args.isNotEmpty()) args[0] else "./icd10.sqlite"

    val db = DB(sqliteFilePath)

    println(db.version)
    val root = db.root
    println(root.billableCodes().size)

}

fun ResultSet.getStringOrNull(name: String): String? {
    val value = getString(name)
    return if (wasNull()) {
        null
    } else {
        value
    }
}

fun ResultSet.getIntOrNull(name: String): Int? {
    val value = getInt(name)
    return if (wasNull()) {
        null
    } else {
        value
    }
}

fun ResultSet.getDecoded(name: String): List<String> {
    val value = getString(name)
    return if (wasNull() || value.isNullOrEmpty()) {
        emptyList()
    } else {
        value.decode()
    }
}

class DB(sqliteFilePath: String) {

    val jdbcUrl = "jdbc:sqlite:${sqliteFilePath}"

    val version: String

    val createdAt: Long

    val root: Root = Root()

    init {
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM icd_meta").use { set ->
                    if (!set.next()) {
                        throw IllegalStateException("No row found in icd10_meta table.")
                    }

                    version = set.getString("version")
                    createdAt = set.getLong("created_at")
                }

                statement.executeQuery("SELECT * FROM icd_nodes order by type_id asc, id asc").use { set ->
                    val nodesById = mutableMapOf<Int, IcdNode>()
                    while (set.next()) {
                        val parentId = set.getIntOrNull("parent_id")
                        val databaseId = set.getInt("id")
                        val name = set.getString("name")
                        val description = set.getString("description")
                        val typeId = set.getByte("type_id")
                        val type = IcdNodeType.fromId(typeId)
                        if (type == null) {
                            throw IllegalStateException("Invalid node type id: $typeId for node: $name ($databaseId)")
                        }

                        val parent = if (parentId == null) {
                            null
                        } else {
                            nodesById[parentId]
                                ?:
                                throw IllegalStateException("Unable to find parent id: $parentId for node: $name ($databaseId)")
                        }

                        val notes = set.getDecoded("notes")
                        val includes = set.getDecoded("includes")
                        val inclusionTerms = set.getDecoded("inclusion_terms")
                        val excludes1 = set.getDecoded("excludes1").map { CodeReference(it) }
                        val excludes2 = set.getDecoded("excludes2").map { CodeReference(it) }
                        val codeFirst = set.getDecoded("code_first").map { CodeReference(it) }
                        val codeAlso = set.getDecoded("code_also").map { CodeReference(it) }
                        val useAdditionalCode = set.getDecoded("use_additional_code").map { CodeReference(it) }

                        val node = when (type) {
                            IcdNodeType.CHAPTER -> {
                                Chapter(
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
                                    useAdditionalCode
                                )
                            }

                            IcdNodeType.SECTION -> {
                                if (parent !is Chapter) {
                                    throw IllegalStateException("Invalid parent type ${parent?.javaClass?.kotlin?.simpleName} (${parentId}) for section node: $name ($databaseId)")
                                }

                                Section(
                                    parent as Chapter,
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
                                    useAdditionalCode
                                )
                            }

                            IcdNodeType.CODE -> {
                                if ((parent !is Code) && (parent !is Section)) {
                                    throw IllegalStateException("Invalid parent type ${parent?.javaClass?.kotlin?.simpleName} (${parentId}) for code node: $name ($databaseId)")
                                }

                                Code(
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
                                    useAdditionalCode
                                )
                            }

                            else -> {
                                throw IllegalStateException("Invalid type: $type (${type.id}) for code node: $name ($databaseId)")
                            }
                        }

                        nodesById[databaseId] = node
                    }
                }
            }
        }
    }

    fun getCodeDetails(icdNode: IcdNode): String? {
        return DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement("SELECT clinical_details FROM icd_node_clinical_details WHERE icd_node_id = ?").use { statement ->
                statement.setInt(1, icdNode.databaseId)
                statement.executeQuery().use { set ->
                    if (set.next()) {
                        set.getString("clinical_details")
                    } else {
                        null
                    }
                }
            }
        }
    }
}
