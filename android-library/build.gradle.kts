import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test

plugins {
    id("com.android.library")

    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi") version "2.0.0-alpha02"
    id("io.github.cdsap.td.paparazzi")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

}

dependencies {

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.compose.material:material:1.3.1")

    implementation("androidx.activity:activity-compose:1.5.1")
    implementation(platform("androidx.compose:compose-bom:2022.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jcodec:jcodec:0.2.5")
    testImplementation(project(":lib"))
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.2")
}

    tasks.withType<Test>().configureEach {

        develocity.testDistribution {
            // your TD config
            enabled = true
            maxLocalExecutors = 1
            maxRemoteExecutors = 3
        }

        inputs.dir(layout.buildDirectory.dir("intermediates/paparazzi"))
            .withPathSensitivity(PathSensitivity.RELATIVE)


        configurations.findByName("layoutlibResources")?.let { layoutlibResources ->
            // Configuration required to include the file collection layoutlibResourcesFiles
            val layoutlibResourcesFiles = layoutlibResources.incoming.artifactView {
                attributes {
                    attribute(
                        ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                        ArtifactTypeDefinition.DIRECTORY_TYPE
                    )
                }
            }.files

            inputs.files(layoutlibResourcesFiles)
                .withPropertyName("paparazzi.layoutlib.resources")
                .withPathSensitivity(PathSensitivity.NONE)
        }

        outputs.dir("build/reports/paparazzi/")
        useJUnitPlatform()
    }

