/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

repositories {
    mavenLocal()
    mavenCentral()
}

plugins {
    `java-library`
    `maven-publish`
    jacoco
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    implementation(project(":event-stream-rpc-model"))
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("software.amazon.awssdk.crt:aws-crt:0.29.16")

    testImplementation("org.json:json:20231013")
    testCompileOnly("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testCompileOnly("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("org.junit.platform:junit-platform-console-standalone:1.7.0")
    testRuntimeOnly("org.slf4j:slf4j-jdk14:1.7.30")

    //pull in server for tests
    testImplementation(project(":test-model-codegen"))
    testImplementation(project(":event-stream-rpc-server"))
}

val test by tasks.getting(Test::class) {
    // Use junit platform for unit tests
    useJUnitPlatform()
}

tasks.test {
    systemProperties.set("aws.crt.debugnative", "1")
    systemProperties.set("aws.crt.memory.tracing", "1")
}

/*
 * CheckStyle
 * ====================================================
 */
//disabled due to how generated code is included in normal source
//apply(plugin = "checkstyle")
//tasks["checkstyleTest"].enabled = false

/*
 * Tests
 * ====================================================
 *
 * Configure the running of tests.
 */
// Log on passed, skipped, and failed test events if the `-Plog-tests` property is set.
if (project.hasProperty("log-tests")) {
    tasks.test {
        testLogging.events("passed", "skipped", "failed")
    }
}

/*
 * Code coverage
 * ====================================================
 */
apply(plugin = "jacoco")

// Always run the jacoco test report after testing.
tasks["test"].finalizedBy(tasks["jacocoTestReport"])

// Configure jacoco to generate an HTML report.
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("$buildDir/reports/jacoco")
    }
}
