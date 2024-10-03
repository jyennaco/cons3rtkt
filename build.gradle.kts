plugins {
    kotlin("jvm") version "2.0.10"
    kotlin("plugin.serialization") version "2.0.20"
    application
}

group = "com.joeyennaco"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.github.ajalt.clikt:clikt:5.0.0")
}

application {
    mainClass = "com.joeyennaco.MainKt"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.joeyennaco.MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}