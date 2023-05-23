import org.gradle.configurationcache.extensions.capitalized

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("app.cash.paparazzi") version "1.2.0"
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
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
    testImplementation("io.github.cdsap:td-paparazzi-ext:0.1")
}

androidComponents {
    onVariants(selector().all()) { variant ->
        val mergeOutputTask = project.tasks.register(
            "mergePaparazzi${variant.name.capitalized()}Outputs", MergeOutputTask::class.java
        ) {
            dependsOn(tasks.named("test${variant.name.capitalized()}UnitTest"))
            artifactFiles.set(layout.projectDirectory.dir("build/reports/paparazzi"))
            outputFile.set(layout.projectDirectory.dir("build/reports/paparazzi-td"))
        }

        project.tasks.withType<Test>().configureEach {
            if (name == "test${variant.name.capitalized()}UnitTest") {
                finalizedBy(mergeOutputTask)
            }
        }
    }
}


@CacheableTask
abstract class MergeOutputTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val artifactFiles: DirectoryProperty

    @get:Internal
    val runList = mutableListOf<String>()

    @get:OutputDirectory
    abstract val outputFile: DirectoryProperty


    @TaskAction
    fun writeResourcesFile() {
        val inputDirectory = artifactFiles.get()
        if (inputDirectory.asFile.walkTopDown().count() > 1) {
            val foldersToCopy = listOf("runs", "images", "videos")
            val outputDirectory = outputFile.get()
            createOutputDirectories(outputDirectory)
            createStaticFiles(inputDirectory, outputDirectory)
            inputDirectory.asFile.walkTopDown()
                .filter { it.isDirectory && it.name.startsWith("td-") }
                .forEach {
                    it.walkTopDown().forEach {
                        if (it.isDirectory && foldersToCopy.contains(it.name)) {
                            copyResources(it, outputDirectory)
                            if (it.name == "runs") {
                                extractRuns(it)
                            }
                        }
                    }

                }
            writeRunsJs(outputDirectory)

        }
    }

    private fun copyResources(it: File, outputDirectory: Directory) {
        it.copyRecursively(
            File("$outputDirectory/${it.name}"), overwrite = true
        )
    }

    private fun extractRuns(it: File) {
        it.walkTopDown().filter { it -> it.name != "runs" }
            .forEach { runList.add(it.name.replace(".js", "")) }
    }

    private fun writeRunsJs(outputDirectory: Directory) {
        var runFormatted = ""
        runList.forEach {
            runFormatted += "\"$it\",\n"
        }

        File("$outputDirectory/index.js").writeText(
            """
          window.all_runs = [
            ${runFormatted.dropLast(1)}
          ];
        """.trimIndent()
        )
    }

    private fun createStaticFiles(
        inputDirectory: Directory, outputDirectory: Directory
    ) {
        inputDirectory.asFileTree.filter { it.name == "index.html" }.first()
            .copyTo(File("$outputDirectory/index.html"))
        inputDirectory.asFileTree.filter { it.name == "paparazzi.js" }.first()
            .copyTo(File("$outputDirectory/paparazzi.js"))
    }

    private fun createOutputDirectories(outputDirectory: Directory) {
        if (File("$outputDirectory/images").isDirectory && File("$outputDirectory/images").exists()) {
            File("$outputDirectory/images").deleteRecursively()
        }
        if (File("$outputDirectory/videos").isDirectory && File("$outputDirectory/videos").exists()) {
            File("$outputDirectory/videos").deleteRecursively()
        }
        if (File("$outputDirectory/runs").isDirectory && File("$outputDirectory/runs").exists()) {
            File("$outputDirectory/runs").deleteRecursively()
        }
        if (File("$outputDirectory/index.html").exists()) {
            File("$outputDirectory/index.html").delete()
        }
        if (File("$outputDirectory/index.js").exists()) {
            File("$outputDirectory/index.js").delete()
        }
        if (File("$outputDirectory/paparazzi.js").exists()) {
            File("$outputDirectory/paparazzi.js").delete()
        }
        val images = File("$outputDirectory/images")
        val runs = File("$outputDirectory/runs")
        val videos = File("$outputDirectory/videos")
        images.mkdir()
        runs.mkdir()
        videos.mkdir()
    }
}
