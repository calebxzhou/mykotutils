pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenLocal()
        mavenCentral()
    }
}


include(":std", ":log", ":ktor")
project(":std").projectDir = file("std")
project(":log").projectDir = file("log")
project(":ktor").projectDir = file("ktor")
