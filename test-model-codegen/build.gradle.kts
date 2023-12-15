/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

plugins {
    id("software.amazon.smithy").version("0.5.0")
    `java-library`
    id("com.github.johnrengelman.shadow").version("6.1.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.compileJava {
    dependsOn("smithyBuildJar")
}

tasks.test {
    systemProperties.set("aws.crt.debugnative", "1")
    systemProperties.set("aws.crt.memory.tracing", "1")
}

sourceSets {
    main {
        java {
            srcDirs("${buildDir}/smithyprojections/test-model-codegen/source/event-stream-rpc-java/client/",
                    "${buildDir}/smithyprojections/test-model-codegen/source/event-stream-rpc-java/model/",
                    "${buildDir}/smithyprojections/test-model-codegen/source/event-stream-rpc-java/server/")
        }
    }
}

dependencies {
    implementation(project(":smithy-event-stream-rpc-cpp"))
    implementation(project(":smithy-event-stream-rpc-java"))
    implementation(project(":smithy-event-stream-rpc-python"))
    implementation(project(":smithy-event-stream-rpc-javascript"))
    implementation(project(":event-stream-rpc-model"))
    implementation(project(":event-stream-rpc-client"))
    implementation(project(":event-stream-rpc-server"))

    compileOnly("org.junit.jupiter:junit-jupiter-api:5.4.0")
    compileOnly("org.junit.jupiter:junit-jupiter-params:5.4.0")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.0")

    implementation("software.amazon.awssdk.crt:aws-crt:0.16.14")
    implementation("com.google.code.gson:gson:2.8.6")
    compileOnly("software.amazon.smithy:smithy-cli:1.42.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


//do not copy below for public release - it is debug noise to generate classpath files for CLI unit testing
tasks.withType<Test> {
    this.classpath.forEach { println(it) }
}
