val ktorVersion: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra

dependencies {
    implementation(project(":std"))
    implementation(project(":log"))

    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    testImplementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")


    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
}