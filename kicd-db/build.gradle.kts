plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "org.tekfive.kicd"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":kicd-lib"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.sqlite.jdbc)
    implementation(libs.slf4j.api)
    implementation(libs.google.genai)
    implementation(libs.okhttp)

    runtimeOnly(libs.logback.classic)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit.vintage)
}

tasks.test {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "org.tekfive.kicd"
            artifactId = "kicd-db"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri(
                System.getenv("GITHUB_REPOSITORY")?.let { "https://maven.pkg.github.com/$it" }
                    ?: "https://maven.pkg.github.com/AIDEWAY/kicd",
            )
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
            }
        }
    }
}
