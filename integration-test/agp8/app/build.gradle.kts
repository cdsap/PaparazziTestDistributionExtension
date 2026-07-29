import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Functional check for the io.github.cdsap.td.paparazzi plugin: in a real AGP + Paparazzi build
// the unit-test task must declare the Paparazzi render inputs, so Test Distribution ships them to
// remote agents. This lives here rather than in the plugin's functionalTest suite because that
// suite runs without an Android SDK and never triggers the variant wiring that registers them.
tasks.register("verifyPaparazziTestInputs") {
    // Runs after the test task so its full input graph (incl. the AGP ASM-transform output) is
    // materialized; querying the aggregate input set before then is not supported.
    dependsOn("testDebugUnitTest")

    val declaredTestInputs = files(tasks.named<Test>("testDebugUnitTest").map { it.inputs.files })
    val intermediatesDir = layout.buildDirectory.dir("intermediates/paparazzi/debug")
    val layoutlibResources = configurations.named("layoutlibResources").map { cfg ->
        cfg.incoming.artifactView {
            attributes.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                ArtifactTypeDefinition.DIRECTORY_TYPE,
            )
        }.files
    }
    val aarResourceDirs = configurations.named("debugRuntimeClasspath").map { cfg ->
        cfg.incoming.artifactView {
            attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "android-res")
            lenient(true)
            componentFilter { id -> id !is ProjectComponentIdentifier }
        }.files
    }

    doLast {
        val declared = declaredTestInputs.files.map { it.canonicalFile.toPath() }.toSet()
        // An expected artifact is "declared" if some input path is it or lives under it — this is
        // agnostic to whether Gradle exposes a directory input as the dir itself or its contents.
        fun declaresUnder(expected: File): Boolean {
            val root = expected.canonicalFile.toPath()
            return declared.any { it.startsWith(root) }
        }

        require(declaresUnder(intermediatesDir.get().asFile)) {
            "testDebugUnitTest does not declare the paparazzi intermediates " +
                "(${intermediatesDir.get().asFile}) as an input"
        }

        val layoutlib = layoutlibResources.get().toList()
        require(layoutlib.isNotEmpty() && layoutlib.all { declaresUnder(it) }) {
            "testDebugUnitTest does not declare the layoutlib resource inputs; expected $layoutlib"
        }

        val aarRes = aarResourceDirs.get().toList()
        require(aarRes.all { declaresUnder(it) }) {
            "testDebugUnitTest does not declare the AAR android-res inputs; expected $aarRes"
        }

        println(
            "PAPARAZZI_TD_INPUTS_OK: intermediates + ${layoutlib.size} layoutlib + " +
                "${aarRes.size} aar-res inputs declared on testDebugUnitTest"
        )
    }
}
