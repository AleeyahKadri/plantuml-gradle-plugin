import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    groovy
    java
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.11.0"
    `maven-publish`
    jacoco
    id("org.sonarqube") version "2.8"
    id("com.github.roroche.plantuml") version "1.0.2"
    id("org.fmiw.plantuml") version "0.1"
}

group = "com.github.roroche"
version = if (project.hasProperty("newVersion")) {
    project.property("newVersion").toString()
} else {
    "1.0.33-SNAPSHOT"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.codehaus.groovy:groovy-all:3.0.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("ch.ifocusit:plantuml-builder:1.4")
    implementation("io.github.classgraph:classgraph:4.8.78")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
    testImplementation("com.pragmaticobjects.oo.tests:oo-tests:0.0.1")
    testImplementation("com.pragmaticobjects.oo.tests:tests-junit5:0.0.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation(gradleTestKit())
    testImplementation("org.assertj:assertj-core:3.15.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<GroovyCompile>("compileGroovy") {
    val compileKotlin = tasks.named<KotlinCompile>("compileKotlin")
    dependsOn(compileKotlin)
    classpath += files(compileKotlin.get().destinationDirectory.get().asFile)
}

tasks.named<GroovyCompile>("compileTestGroovy") {
    val compileTestKotlin = tasks.named<KotlinCompile>("compileTestKotlin")
    dependsOn(compileTestKotlin)
    classpath += files(compileTestKotlin.get().destinationDirectory.get().asFile)
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    executionData(tasks.withType<Test>())
    reports {
        xml.isEnabled = true
    }
    dependsOn(tasks.named("check"))
}

sonarqube {
    properties {
        property("sonar.projectKey", "RoRoche_plantuml-gradle-plugin")
        property("sonar.organization", "roroche")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

tasks.named("sonarqube") {
    dependsOn(tasks.named("check"), tasks.named("jacocoTestReport"))
}

gradlePlugin {
    plugins {
        create("plantUmlPlugin") {
            id = "com.github.roroche.plantuml"
            implementationClass = "com.github.roroche.plantuml.PlantUmlPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginPublication") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "plantuml-gradle-plugin"
            version = project.version.toString()
        }
    }
}

pluginBundle {
    website = "https://github.com/RoRoche/plantuml-gradle-plugin"
    vcsUrl = "https://github.com/RoRoche/plantuml-gradle-plugin.git"
    tags = listOf(
        "plantuml",
        "plantuml-diagrams",
        "plantuml-generator",
        "living-documentation"
    )

    plugins {
        getByName("plantUmlPlugin") {
            displayName = "PlantUML plugin"
            description = "Gradle plugin to build PlantUML diagrams from code (for living and up-to-date documentation)"
        }
    }
}

configure<com.github.roroche.plantuml.tasks.ClassDiagramExtension> {
    packageName = "com.github.roroche"
    outputFile = project.file("diagrams/class_diagram.plantuml")
}

configure<org.fmiw.plantuml.PlantUML> {
    options {
        outputDir = project.file("diagrams")
    }

    diagrams {
        create("classes") {
            sourceFile = project.file("diagrams/class_diagram.plantuml")
        }
    }
}

tasks.named("generateDiagramClasses") {
    dependsOn(tasks.named("buildClassDiagram"))
}
