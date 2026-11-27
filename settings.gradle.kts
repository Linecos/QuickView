pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
}

stonecutter {
    create(rootProject) {
        versions("1.21.1", "1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1", "26.2", "26.3").buildscript("build.gradle.kts")
        vcsVersion = "26.2"
    }
}

rootProject.name = "quickview"
