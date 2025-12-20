val kotlinLoggingVersion: String by rootProject.extra

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    testImplementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}