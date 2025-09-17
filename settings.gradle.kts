pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://repositories.tomtom.com/artifactory/maven")
            content {
                includeGroupByRegex("com\\.tomtom.*")
            }
            credentials {
                username = providers.gradleProperty("repositoriesTomtomComUsername").getOrElse("")
                password = providers.gradleProperty("repositoriesTomtomComPassword").getOrElse("")
            }
        }
        // Fallback to JitPack if needed
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "POConnbandtomtom"
include(":app")
 