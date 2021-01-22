import org.javamodularity.moduleplugin.extensions.TestModuleOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.61"
    `java-library`
    id("org.openjfx.javafxplugin") version "0.0.9"
}

val tornado_version: String by project
val kotlin_version: String by project
val json_version: String by project
val dokka_version: String by project
val httpclient_version: String by project
val felix_framework_version: String by project
val junit_version: String by project
val testfx_version: String by project
val fontawesomefx_version: String by project

group = "no.tornado"
version = "2.0.0-SNAPSHOT"
description = "JavaFX Framework for Kotlin"

javafx {
    version = "11.0.2"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing", "javafx.web", "javafx.media")
}

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin_version}")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlin_version}")

    implementation("no.tornado:tornadofx2:${tornado_version}")

    implementation("org.glassfish:javax.json:${json_version}")
    implementation("org.apache.httpcomponents:httpclient:${httpclient_version}")
    implementation("org.apache.felix:org.apache.felix.framework:${felix_framework_version}")
    implementation("de.jensd:fontawesomefx-fontawesome:${fontawesomefx_version}")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${kotlin_version}")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2")
    testImplementation("junit:junit:${junit_version}")
    testImplementation("org.testfx:testfx-junit:${testfx_version}")

}

sourceSets {
    getByName("test").java.srcDirs("src/test/kotlin")
}

//val patchArgs = listOf(
//    "--add-exports","javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
//    "--add-exports","tornadofx/tornadofx.tests=kotlin.reflect",
//    "--add-opens","tornadofx/tornadofx.tests=javafx.base",
//    "--add-reads","tornadofx=jdk.httpserver"
//)
tasks.test {
    extensions.configure(TestModuleOptions::class) {
        runOnClasspath = true
    }
    testLogging.showStandardStreams = true
}

//tasks.withType<Test> {
//    useJUnitPlatform()
//}
//
//dependencies {
//    testImplementation(platform("org.junit:junit-bom:5.7.0"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
//
//    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.0")
//    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")
//    testImplementation("org.hamcrest:hamcrest:2.1")
//}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

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