pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {setUrl("https://jitpack.io")}
//        maven {setUrl("https://raw.github.com/zeeshanejaz/unirest-android/mvn-reop")}
    }
}

rootProject.name = "Thirdtry"
include(":app")
