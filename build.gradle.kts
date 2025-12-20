import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
	kotlin("jvm") version "2.2.21" apply false
	kotlin("plugin.serialization") version "2.2.21" apply false
}

group = "calebxzhou.mykotutils"
version = "0.1"

extra.apply {
	set("ktorVersion", "3.3.3")
	set("kotlinLoggingVersion", "7.0.6")
}

subprojects {
	group = "calebxzhou.mykotutils"
	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
	apply(plugin = "java")
	apply(plugin = "java-library")
	apply(plugin = "maven-publish")

	repositories {
		mavenLocal()
		mavenCentral()
	}

	extensions.configure<KotlinJvmProjectExtension>("kotlin") {
		jvmToolchain(21)
	}

	extensions.configure<JavaPluginExtension> {
		withSourcesJar()
		withJavadocJar()
	}

	plugins.withType<JavaPlugin> {
		extensions.configure<PublishingExtension> {
			publications {
				create<MavenPublication>("maven") {
					groupId = "calebxzhou.mykotutils"
                    version = "0.1"
                    artifactId = project.name
					from(components["java"])
				}
			}
		}
	}
}
