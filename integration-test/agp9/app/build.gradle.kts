import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.PathSensitivity
plugins {
    id("com.android.library")
 
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi")
    id("io.github.cdsap.td.paparazzi")
}

android {
    namespace = "com.example.integration"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
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
    implementation("androidx.compose.material:material:1.3.1")
    implementation(platform("androidx.compose:compose-bom:2022.10.00"))
    implementation("androidx.compose.ui:ui")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Allow CI to relocate the TD report dirs without touching this file. The default
// keeps the plugin's built-in convention (build/reports/paparazzi -> build/reports/paparazzi-td);
// passing -PtdInputReportDir=... -PtdOutputReportDir=... exercises the
// `paparazzi.td.report.dir` wiring end-to-end on Test Distribution agents.
val tdInputReportDirOverride = providers.gradleProperty("tdInputReportDir")
val tdOutputReportDirOverride = providers.gradleProperty("tdOutputReportDir")
val tdInputReportDirPath = tdInputReportDirOverride.getOrElse("build/reports/paparazzi")

tdPaparazzi {
    tdInputReportDirOverride.orNull?.let { inputReportDir.set(it) }
    tdOutputReportDirOverride.orNull?.let { outputReportDir.set(it) }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    maxParallelForks = 3
    setForkEvery(1L)

    inputs.file(
        layout.buildDirectory.file(
            "intermediates/paparazzi/${
                name.replace("UnitTest", "")
                    .replace("test", "")
                    .lowercase()
            }/resources.json"
        )
    )

    develocity.testDistribution {
        enabled = true
        remoteExecutionPreferred = true
        maxLocalExecutors = 0
    }

    inputs.dir(layout.buildDirectory.dir("intermediates/paparazzi"))
        .withPathSensitivity(PathSensitivity.RELATIVE)

    configurations.findByName("layoutlibResources")?.let { layoutlibResources ->
        // Configuration required to include the file collection layoutlibResourcesFiles
        val layoutlibResourcesFiles = layoutlibResources.incoming.artifactView {
            attributes.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                ArtifactTypeDefinition.DIRECTORY_TYPE
            )
        }.files

        inputs.files(layoutlibResourcesFiles)
            .withPropertyName("paparazzi.layoutlib.resources")
            .withPathSensitivity(PathSensitivity.NONE)
    }

    outputs.dir(layout.projectDirectory.dir(tdInputReportDirPath))
}
