plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "io.github.kdroidfilter.taokt"
version = "0.1.0-SNAPSHOT"

dependencies {
    implementation("io.github.kdroidfilter.taokt:taokt-bindings:$version")
    implementation(project(":"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("io.github.kdroidfilter.taokt.skiko.MainKt")
}
