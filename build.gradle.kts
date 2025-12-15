import com.google.cloud.tools.jib.gradle.BuildDockerTask
import com.google.cloud.tools.jib.gradle.BuildImageTask
import com.google.cloud.tools.jib.gradle.BuildTarTask
import com.google.cloud.tools.jib.gradle.JibExtension

plugins {
    java
    jacoco
    checkstyle
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.freefair.lombok)
    alias(libs.plugins.versions)
    alias(libs.plugins.jib)
}

group = "de.gematik.zeta"
version = "0.1.3"
description =
    "achelos Testfachdienst providing a REST API with CRUD operations, secure communication via TLS and Websocket connections."

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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-websocket")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation(libs.springdoc)
    implementation(libs.springwolfstomp)
    implementation(libs.springwolfstompbinding)

    implementation(platform(libs.opentelemetrybom))
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-sdk-testing")

    implementation(libs.protobuf)
    implementation(libs.jobrunr)


    runtimeOnly(libs.springwolfui)
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    config = resources.text.fromFile(file("config/checkstyle/custom_google_checks.xml"))
    isIgnoreFailures = false
    maxWarnings = 0
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

lombok {
    version.set(
        libs.versions.lombok
            .get(),
    )
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

extensions.configure<JibExtension>("jib") {
    from {
        image = "gcr.io/distroless/java21-debian12"
    }
    to {
        image = "your-docker-registry.example.org/zeta/testing/testfachdienst"
        tags = setOf(project.version.toString(), "latest")
    }
    container {
        containerizingMode = "exploded"
        jvmFlags =
            listOf(
                "-XX:+UseContainerSupport",
                "-XX:MaxRAMPercentage=75.0",
                "-XX:+ExitOnOutOfMemoryError",
            )
        ports = listOf("8080", "8081")
        user = "65532:65532"
        creationTime = "USE_CURRENT_TIMESTAMP"
        workingDirectory = "/app"
        labels =
            mapOf(
                "org.opencontainers.image.title" to project.name,
                "org.opencontainers.image.version" to project.version.toString(),
            )
    }
}

tasks.withType<BuildDockerTask>().configureEach {
    notCompatibleWithConfigurationCache("Jib touches Project at execution time")
}
tasks.withType<BuildImageTask>().configureEach {
    notCompatibleWithConfigurationCache("Jib touches Project at execution time")
}
tasks.withType<BuildTarTask>().configureEach {
    notCompatibleWithConfigurationCache("Jib touches Project at execution time")
}
