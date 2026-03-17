plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
}

group = "io.github.cdsap"
version = "0.2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly("com.android.tools.build:gradle:9.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("tdPaparazzi") {
            id = "io.github.cdsap.td.paparazzi"
            implementationClass = "io.github.cdsap.td.paparazzi.plugin.TDPaparazziPlugin"
            displayName = "Test Distribution Paparazzi Extension"
            description = "Extension to make compatible Paparazzi html output reports with Develocity Test Distribution"
        }
    }
}

pluginBundle {
    website = "https://github.com/cdsap/PaparazziTestDistributionExtension"
    vcsUrl = "https://github.com/cdsap/PaparazziTestDistributionExtension"
    tags = listOf("Develocity", "Test Distribution", "Paparazzi")
}
