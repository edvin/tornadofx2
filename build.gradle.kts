import org.javamodularity.moduleplugin.extensions.TestModuleOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
/**
 * Gradle version 6.6.1
 */
plugins {
    kotlin("jvm") version "1.4.32"
    `java-library`
    id("org.openjfx.javafxplugin") version "0.0.9"
    `maven-publish`
    id("org.jetbrains.dokka") version "1.4.20"
    signing
}
//see gradle.properties
val tornado_version: String by project
val kotlin_version: String by project
val json_version: String by project
val dokka_version: String by project
val httpclient_version: String by project
val felix_framework_version: String by project
val junit4_version: String by project
val junit5_version: String by project
val testfx_version: String by project
val hamcrest_version: String by project
val fontawesomefx_version: String by project

group = "no.tornado"
version = tornado_version
description = "JavaFX Framework for Kotlin"

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

repositories {
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin_version}")

    api("org.glassfish:javax.json:${json_version}")
    api("org.apache.httpcomponents:httpclient:${httpclient_version}")
    api("de.jensd:fontawesomefx-fontawesome:${fontawesomefx_version}")
    implementation("org.apache.felix:org.apache.felix.framework:${felix_framework_version}")
}

javafx {
    version = "15.0.1"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing", "javafx.web", "javafx.media")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<JavaCompile> {
    options.release.set(11)
}

java.withSourcesJar()

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

/**
 * Testing
 */
sourceSets {
    getByName("test").java.srcDirs("src/test/kotlin")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    //no modules yet
    extensions.configure(TestModuleOptions::class) {
        runOnClasspath = true
    }
}

dependencies {
    //common
    testImplementation("org.hamcrest:hamcrest:${hamcrest_version}")
    testImplementation("org.hamcrest:hamcrest-library:${hamcrest_version}")
    testImplementation("org.testfx:testfx-junit5:${testfx_version}")
    //Junit 5
    testImplementation(platform("org.junit:junit-bom:${junit5_version}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${kotlin_version}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    //Junit 4
    testCompileOnly("junit:junit:${junit4_version}")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    //headless
    testRuntimeOnly("org.testfx:openjfx-monocle:jdk-12.0.1+2") // jdk-9+181 for Java 9, jdk-11+26 for Java 11
}

/**
 * Publishing
 */
publishing {
    repositories {
        maven {
            name = "release"
            description = "Release repository"
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            url = if(version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
//            credentials {
//                username = scdUserName
//                password = scdPassword
//            }
        }
    }
    publications {
        create<MavenPublication>("tornadofx") {
            from(components["java"])
            pom {
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("Edvin Syse")
                        email.set("es@syse.no")
                        organization.set("SYSE AS")
                        organizationUrl.set("https://www.syse.no")
                    }
                    developer {
                        name.set("Thomas Nield")
                        email.set("thomasnield@live.com")
                        organization.set("Southwest Airlines")
                        organizationUrl.set("https://www.southwest.com/")
                    }
                    developer {
                        name.set("Matthew Turnblom")
                        email.set("uberawesomeemailaddressofdoom@gmail.com")
                        organization.set("Xactware")
                        organizationUrl.set("https://www.xactware.com/")
                    }
                    developer {
                        name.set("Craig Tadlock")
                        email.set("craig.tadlock@gototags.com")
                        organization.set("GoToTags")
                        organizationUrl.set("https://gototags.com/")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:edvin/tornadofx2.git")
                    developerConnection.set("scm:git:git@github.com:edvin/tornadofx2.git")
                    url.set("git@github.com:edvin/tornadofx2.git")
                }

            }
        }
    }
}

signing {
    setRequired({
        (project.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
    })
    val signingKey: String? by project // ORG_GRADLE_PROJECT_signingKey
    val signingPassword: String? by project // ORG_GRADLE_PROJECT_signingPassword
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["tornadofx"])
    }
}
