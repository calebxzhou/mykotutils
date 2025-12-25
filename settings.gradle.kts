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


include(":std", ":log", ":ktor",":curseforge","modrinth","hwspec","mojang")
project(":std").projectDir = file("std")
project(":log").projectDir = file("log")
project(":ktor").projectDir = file("ktor")
project(":curseforge").projectDir = file("curseforge")
project(":modrinth").projectDir = file("modrinth")
project(":hwspec").projectDir = file("hwspec")
project(":mojang").projectDir = file("mojang")