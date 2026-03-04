plugins {
    groovy
    java
    kotlin("jvm") version "1.7.22"
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
    dependsOn(tasks.named("compileKotlin"))
    classpath += files(tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin").get().destinationDirectory)
}

tasks.named<GroovyCompile>("compileTestGroovy") {
    dependsOn(tasks.named("compileTestKotlin"))
    classpath += files(tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin").get().destinationDirectory)
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
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
            groupId = project.group as String
            artifactId = "plantuml-gradle-plugin"
            version = project.version as String
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
        named("plantUmlPlugin") {
            displayName = "PlantUML plugin"
            description = "Gradle plugin to build PlantUML diagrams from code (for living and up-to-date documentation)"
        }
    }
}

// Configure classDiagram extension using direct property access
(extensions.getByName("classDiagram") as groovy.lang.GroovyObject).apply {
    setProperty("packageName", "com.github.roroche")
    setProperty("outputFile", project.file("diagrams/class_diagram.plantuml"))
//    setProperty("ignoredClasses", listOf(
//        "com.github.roroche.plantuml.diagrams.Diagram\$Simple",
//        "com.github.roroche.plantuml.diagrams.Diagram\$Wrap",
//        "com.github.roroche.plantuml.classes.Classes\$Simple",
//        "com.github.roroche.plantuml.classes.Classes\$Wrap",
//        "com.github.roroche.plantuml.urls.Urls\$Wrap"
//    ))
}

// Configure plantuml extension using direct method invocation
(extensions.getByName("plantuml") as groovy.lang.GroovyObject).apply {
    invokeMethod("options", closureOf<Any> {
        (this as groovy.lang.GroovyObject).setProperty("outputDir", project.file("diagrams"))
    })
    invokeMethod("diagrams", closureOf<Any> {
        // diagrams is a NamedDomainObjectContainer, use register method
        (this as org.gradle.api.NamedDomainObjectContainer<*>).register("classes") {
            (this as groovy.lang.GroovyObject).setProperty("sourceFile", project.file("diagrams/class_diagram.plantuml"))
        }
    })
}

tasks.named("generateDiagramClasses") {
    dependsOn(tasks.named("buildClassDiagram"))
}
