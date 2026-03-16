plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    `maven-publish`
    `signing`
}

group = "io.github.cdsap"
version = "0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("app.cash.paparazzi:paparazzi:2.0.0-alpha04")
    implementation("com.squareup.okio:okio:3.1.0")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("com.squareup.moshi:moshi-adapters:1.15.2")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}


publishing {
    repositories {
        maven {
            name = "Snapshots"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

            credentials {
                username = System.getenv("USERNAME_SNAPSHOT")
                password = System.getenv("PASSWORD_SNAPSHOT")
            }
        }
        maven {
            name = "Release"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = System.getenv("USERNAME_SNAPSHOT")
                password = System.getenv("PASSWORD_SNAPSHOT")
            }
        }
    }
    publications {
        create<MavenPublication>("libPublication") {
            from(components["java"])
            artifactId = "td-paparazzi-ext"
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                scm {
                    connection.set("scm:git:git://github.com/cdsap/PaparazziTestDistributionExtension/")
                    url.set("https://github.com/cdsap/PaparazziTestDistributionExtension/")
                }
                name.set("td-paparazzi-ext")
                url.set("https://github.com/cdsap/PaparazziTestDistributionExtension/")
                description.set(
                    "Extension to make compatible Paparazzi html output reports with Gradle Enterprise Test Distribution"
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
