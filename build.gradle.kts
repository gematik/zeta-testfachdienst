import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

plugins {
    java
    jacoco
    checkstyle
    alias(libs.plugins.spotless)
    alias(libs.plugins.license.report)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.versions)
}

group = "de.gematik.zeta"
description =
    "achelos Testfachdienst providing a REST API with CRUD operations, secure communication via TLS and Websocket connections."

val releaseVersion = project.version.toString()

abstract class SyncVersionMetadataTask : DefaultTask() {
    @get:Input
    abstract val releaseVersion: Property<String>

    @get:OutputFile
    abstract val asyncApiDoc: RegularFileProperty

    @TaskAction
    fun sync() {
        val doc = asyncApiDoc.get().asFile
        val expectedLine = "  version: ${releaseVersion.get()}"
        val versionPattern = Regex("""(?m)^  version: .*$""")
        val currentContent = doc.readText()
        val updatedContent = currentContent.replace(versionPattern, expectedLine)

        check(updatedContent != currentContent || currentContent.contains(expectedLine)) {
            "${doc.name} does not contain an AsyncAPI version field."
        }

        if (updatedContent != currentContent) {
            doc.writeText(updatedContent)
        }
    }
}

abstract class VerifyVersionMetadataTask : DefaultTask() {
    @get:Input
    abstract val releaseVersion: Property<String>

    @get:InputFile
    abstract val asyncApiDoc: RegularFileProperty

    @TaskAction
    fun verify() {
        val doc = asyncApiDoc.get().asFile
        val expectedLine = "  version: ${releaseVersion.get()}"

        check(doc.readText().contains(expectedLine)) {
            "${doc.name} is out of date. Run ./gradlew syncVersionMetadata."
        }
    }
}

tasks.register("printVersion") {
    val v = project.version.toString()
    doLast {
        println(v)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

tasks.processResources {
    val versionTokens = mapOf("projectVersion" to releaseVersion)
    filteringCharset = "UTF-8"
    filesMatching("application.yml") {
        filter<ReplaceTokens>("tokens" to versionTokens)
    }
}

tasks.register<SyncVersionMetadataTask>("syncVersionMetadata") {
    group = "release"
    description = "Sync checked-in metadata snapshots with the Gradle project version."
    releaseVersion.set(project.version.toString())
    asyncApiDoc.set(layout.projectDirectory.file("docs/async-api-docs.yml"))
    mustRunAfter("verifyVersionMetadata")
}

tasks.register<VerifyVersionMetadataTask>("verifyVersionMetadata") {
    group = "verification"
    description = "Verify checked-in metadata snapshots match the Gradle project version."
    releaseVersion.set(project.version.toString())
    asyncApiDoc.set(layout.projectDirectory.file("docs/async-api-docs.yml"))
}

dependencies {
    val lombokVersion = libs.versions.lombok.get()

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation(libs.springdoc)
    implementation(libs.springwolfstomp)
    implementation(libs.springwolfstompbinding)

    implementation(platform(libs.opentelemetrybom))
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-sdk-testing")

    implementation(libs.protobuf)
    implementation(libs.jobrunr)

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    runtimeOnly(libs.springwolfui)
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    val checkstyleToolJar = configurations.checkstyle.get()
        .resolvedConfiguration
        .resolvedArtifacts
        .first {
            it.moduleVersion.id.group == "com.puppycrawl.tools" && it.name == "checkstyle"
        }
        .file
    config = resources.text.fromArchiveEntry(checkstyleToolJar, "google_checks.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

spotless {
    val licenseHeader = file("config/license-header.java").readText() + System.lineSeparator()

    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        licenseHeader(licenseHeader, "package ")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

licenseReport {
    outputDir = layout.buildDirectory.dir("reports/dependency-license").get().asFile.absolutePath
    renderers =
        arrayOf<com.github.jk1.license.render.ReportRenderer>(
            com.github.jk1.license.render.InventoryHtmlReportRenderer(),
            com.github.jk1.license.render.JsonReportRenderer(),
        )
}

graalvmNative {
    metadataRepository {
        enabled.set(true)
    }
    binaries {
        named("main") {
            imageName.set(project.name)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    jvmArgs("-Xshare:off")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Checkstyle>().configureEach {
    if (name == "checkstyleAot" || name == "checkstyleAotTest") {
        enabled = false
    }
}

tasks.named("check") {
    dependsOn("spotlessCheck", "verifyVersionMetadata")
}

tasks.named<com.github.jk1.license.task.ReportTask>("generateLicenseReport") {
    notCompatibleWithConfigurationCache("dependency-license-report touches Project during execution")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}
