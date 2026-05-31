package org.aideway.kicd

object TestHelp {
    const val DatabaseFileClasspath = "/icd-10-2025.sqlite"

    val db: DB
    init {
        val resource = javaClass.getResource(DatabaseFileClasspath)
        if (resource == null) {
            throw IllegalStateException("Could not load test database at classpath: $DatabaseFileClasspath")
        }

        db = DB(resource.path)
    }
}
