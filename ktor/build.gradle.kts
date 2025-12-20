val ktorVersion: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra

dependencies {
    implementation(project(":std"))
    implementation(project(":log"))
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
}