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


include(":std", ":log", "hwspec","mojang")
project(":std").projectDir = file("std")
project(":log").projectDir = file("log")
project(":hwspec").projectDir = file("hwspec")
project(":mojang").projectDir = file("mojang")