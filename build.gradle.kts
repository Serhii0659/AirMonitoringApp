plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "2.25.0"
}

group = "io.github.serhii0659.air_monitoring"
version = "1.0-SNAPSHOT"

// ---- ВЕРСІЇ ----
val javaVersion = 24
val javafxVersion = "21.0.6"

val postgresqlVersion = "42.7.3"
val controlsFxVersion = "11.2.1"
val validatorFxVersion = "0.6.1"
val ikonliVersion = "12.3.1"
val bootstrapFxVersion = "0.4.0"
val tilesFxVersion = "21.0.9"

val junitVersion = "5.12.1"
// -----------------

repositories {
    mavenCentral()
}

val targetRelease = 21

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(targetRelease)
    options.compilerArgs.addAll(listOf(
        "--add-reads", "io.github.serhii0659.air_monitoring.airmonitoringapp=ALL-UNNAMED"
    ))
}

application {
    // Run on classpath using Launcher main
    mainClass.set("io.github.serhii0659.air_monitoring.airmonitoringapp.Launcher")
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing")
}

dependencies {
    implementation("org.postgresql:postgresql:$postgresqlVersion")

    implementation("org.controlsfx:controlsfx:$controlsFxVersion")
    implementation("net.synedra:validatorfx:$validatorFxVersion") {
        exclude(group = "org.openjfx")
    }

    implementation("org.kordamp.ikonli:ikonli-javafx:$ikonliVersion")
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:$bootstrapFxVersion")

    implementation("eu.hansolo:tilesfx:$tilesFxVersion") {
        exclude(group = "org.openjfx")
    }

    // Apache POI for Excel reports
    implementation("org.apache.poi:poi:5.5.0")
    implementation("org.apache.poi:poi-ooxml:5.5.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Run tests on the classpath (disable JPMS for tests) to avoid module path issues
    modularity.inferModulePath.set(false)
    // Helpful logging if a failure occurs
    testLogging {
        events("PASSED", "FAILED", "SKIPPED", "STANDARD_OUT", "STANDARD_ERROR")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
    ignoreFailures = true
}

// Create fat JAR with all dependencies for jpackage
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("AirMonitoringApp")
    archiveVersion.set("1.0")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "io.github.serhii0659.air_monitoring.airmonitoringapp.Launcher"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}