plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish") version "0.36.0"
    `maven-publish`
    `signing`
}

group = "io.github.cdsap"
version = "0.2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly("app.cash.paparazzi:paparazzi:2.0.0-alpha04")
    implementation("com.squareup.okio:okio:3.1.0")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("com.squareup.moshi:moshi-adapters:1.15.2")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")

    testImplementation("app.cash.paparazzi:paparazzi:2.0.0-alpha04")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Compatibility test tasks that run the existing test suite against different Paparazzi versions.
// This verifies that the compileOnly dependency allows the library to work with any supported version.
val paparazziCompatVersions = mapOf(
    "alpha02" to "2.0.0-alpha02",
    "alpha03" to "2.0.0-alpha03",
    "alpha04" to "2.0.0-alpha04"
)

paparazziCompatVersions.forEach { (label, version) ->
    val compatConfig = configurations.create("paparazziCompat_$label") {
        extendsFrom(configurations["testImplementation"], configurations["testRuntimeOnly"])
        resolutionStrategy {
            force("app.cash.paparazzi:paparazzi:$version")
        }
    }

    tasks.register<Test>("testPaparazziCompat_$label") {
        group = "verification"
        description = "Run tests with Paparazzi $version"
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].output + sourceSets["main"].output + compatConfig
        useJUnitPlatform()
    }
}

tasks.register("testPaparazziCompatAll") {
    group = "verification"
    description = "Run tests against all supported Paparazzi versions"
    dependsOn(paparazziCompatVersions.keys.map { "testPaparazziCompat_$it" })
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.cdsap", "td-paparazzi-ext", "0.2.0")

    pom {
        scm {
            connection.set("scm:git:git://github.com/cdsap/PaparazziTestDistributionExtension/")
            url.set("https://github.com/cdsap/PaparazziTestDistributionExtension/")
        }
        name.set("td-paparazzi-ext")
        url.set("https://github.com/cdsap/PaparazziTestDistributionExtension/")
        description.set(
            "Extension to make compatible Paparazzi html output reports with Develocity Test Distribution"
        )
        licenses {
            license {
                name.set("The MIT License (MIT)")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("cdsap")
                name.set("Inaki Villar")
            }
        }
    }
}

if (extra.has("signing.keyId")) {
    afterEvaluate {
        configure<SigningExtension> {
            (extensions.getByName("publishing") as
                PublishingExtension).publications.forEach {
                sign(it)
            }
        }
    }
}
