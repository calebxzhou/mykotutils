val kotlinLoggingVersion: String by rootProject.extra

dependencies {
    implementation(project(":log"))
    implementation("io.github.oshai:kotlin-logging-jvm:${kotlinLoggingVersion}")
    implementation("org.lwjgl:lwjgl-glfw:3.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.github.oshi:oshi-core:6.9.1")
}