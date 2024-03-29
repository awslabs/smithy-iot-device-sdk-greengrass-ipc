/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "Generates EventStream RPC Java code from Smithy model"

repositories {
    mavenLocal()
    mavenCentral()
}

plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    implementation("com.squareup:javapoet:1.13.0")
    implementation("software.amazon.smithy:smithy-codegen-core:[1.0.2,1.1.0[")
    implementation("com.atlassian.commonmark:commonmark:0.14.0")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("software.amazon.smithy:smithy-protocol-test-traits:[1.0.2,1.1.0[")

    testCompileOnly("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testCompileOnly("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testCompileOnly("org.hamcrest:hamcrest:2.1")
}
