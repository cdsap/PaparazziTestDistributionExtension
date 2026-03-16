plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
}

group = "io.github.cdsap"
version = "0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
        }
    }
}
