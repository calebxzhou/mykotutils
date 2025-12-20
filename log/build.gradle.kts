plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    `java-library`
    `maven-publish`
    idea
}
group = "calebxzhou.mykotutils.log"
repositories {
    mavenLocal()
}
kotlin {
    jvmToolchain(21)
}
dependencies{
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.6")
}
// === Register sourcesJar and javadocJar tasks FIRST ===
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

// === Publishing configuration ===
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            // Now we can safely reference the registered tasks
            artifact(sourcesJar.get()) {
                classifier = "sources"  // Optional: some tools expect this
            }
            artifact(javadocJar.get()) {
                classifier = "javadoc"
            }
        }
    }
}