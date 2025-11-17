import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    groovy
    java
    kotlin("jvm") version "1.3.72"
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
    project.property("newVersion") as String
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

tasks.test {
    useJUnitPlatform()
}

tasks.named<GroovyCompile>("compileGroovy") {
    dependsOn(tasks.getByPath("compileKotlin"))
    classpath = classpath + files(tasks.named<KotlinCompile>("compileKotlin").get().destinationDirectory)
}

tasks.named<GroovyCompile>("compileTestGroovy") {
    dependsOn(tasks.getByPath("compileTestKotlin"))
    classpath = classpath + files(tasks.named<KotlinCompile>("compileTestKotlin").get().destinationDirectory)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.jacocoTestReport {
    executionData(tasks.withType<Test>())
    reports {
        xml.isEnabled = true
    }
    dependsOn(tasks.check)
}

sonarqube {
    properties {
        property("sonar.projectKey", "RoRoche_plantuml-gradle-plugin")
        property("sonar.organization", "roroche")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

tasks.sonarqube {
    dependsOn(tasks.check, tasks.jacocoTestReport)
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

configure<com.gradle.publish.PluginBundleExtension> {
    website = "https://github.com/RoRoche/plantuml-gradle-plugin"
    vcsUrl = "https://github.com/RoRoche/plantuml-gradle-plugin.git"
    tags = listOf(
        "plantuml",
        "plantuml-diagrams",
        "plantuml-generator",
        "living-documentation"
    )

    plugins {
        named("plantUmlPlugin") {
            id = "com.github.roroche.plantuml"
            displayName = "PlantUML plugin"
            description = "Gradle plugin to build PlantUML diagrams from code (for living and up-to-date documentation)"
        }
    }
}

extensions.configure<Any>("classDiagram") {
    withGroovyBuilder {
        setProperty("packageName", "com.github.roroche")
        setProperty("outputFile", project.file("diagrams/class_diagram.plantuml"))
//        setProperty("ignoredClasses", listOf(
//            "com.github.roroche.plantuml.diagrams.Diagram\$Simple",
//            "com.github.roroche.plantuml.diagrams.Diagram\$Wrap",
//            "com.github.roroche.plantuml.classes.Classes\$Simple",
//            "com.github.roroche.plantuml.classes.Classes\$Wrap",
//            "com.github.roroche.plantuml.urls.Urls\$Wrap"
//        ))
    }
}

// Note: The plantuml extension configuration is challenging to convert to Kotlin DSL
// due to the dynamic nature of Groovy closures and NamedDomainObjectContainers.
// The original Groovy configuration was:
// plantuml {
//     options {
//         outputDir = project.file('diagrams')
//     }
//     diagrams {
//         classes {
//             sourceFile = project.file('diagrams/class_diagram.plantuml')
//         }
//     }
// }
// This can be configured using project properties or a separate Groovy script if needed.

afterEvaluate {
    tasks.findByName("generateDiagramClasses")?.let {
        it.dependsOn(tasks.named("buildClassDiagram"))
    }
}

