plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

val isJitPackBuild = providers.environmentVariable("JITPACK").orNull == "true"
val jitPackGroup = if (isJitPackBuild) {
    val ownerGroup = providers.environmentVariable("GROUP").orNull
    val repository = providers.environmentVariable("ARTIFACT").orNull
    if (!ownerGroup.isNullOrBlank() && !repository.isNullOrBlank()) "$ownerGroup.$repository" else null
} else {
    null
}
val jitPackVersion = providers.environmentVariable("VERSION").orNull.takeIf { isJitPackBuild }

allprojects {
    group = jitPackGroup ?: "org.tekfive.kicd"
    version = jitPackVersion ?: "1.0.0"
}
