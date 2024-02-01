plugins {
    id("software.amazon.smithy").version("0.5.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.compileJava {
    dependsOn("smithyBuildJar")
    // We need to add the actual Smithy model to the resulting jar file.
    dependsOn("copySmithyModel")
}

// Since imported Smithy models are not added to the result artifacts, copy them explicitly.
// NOTE: Smithy 2.0 allows adding all models into the build, making the following copying obsolete.
tasks.register<Copy>("copySmithyModel") {
    // The processResources task copies the empty main Smithy model (which imports the actual model) into the build
    // directory. We rewrite it with the actual Smithy model right after that.
    dependsOn("processResources")

    val smithyModelFile = layout.projectDirectory.file("../greengrass-ipc-model/main.smithy")

    // Parent output directory, all into-directories below are relative to it.
    into(layout.buildDirectory)

    from(smithyModelFile) {
        into("resources/main/META-INF/smithy/")
    }
    from(smithyModelFile) {
        into("smithyprojections/greengrass-client/source/sources/")
    }
    from(smithyModelFile) {
        into("tmp/smithy-inf/META-INF/smithy/")
    }
}

sourceSets {
    main {
        java {
            srcDirs("${buildDir}/smithyprojections/greengrass-client/source/event-stream-rpc-java/client/",
                    "${buildDir}/smithyprojections/greengrass-client/source/event-stream-rpc-java/model/",
                    "${projectDir}/../event-stream-rpc-client/src/main/java/software/amazon/awssdk/eventstreamrpc/",
                    "${projectDir}/../event-stream-rpc-model/src/main/java/software/amazon/awssdk/eventstreamrpc/",
                    "${projectDir}}/../event-stream-rpc-model/src/main/java/software/amazon/awssdk/eventstreamrpc/model")
        }
    }
}

dependencies {
    implementation(project(":smithy-event-stream-rpc-python"))
    implementation(project(":smithy-event-stream-rpc-java"))
    implementation(project(":smithy-event-stream-rpc-cpp"))
    implementation(project(":smithy-event-stream-rpc-javascript"))
    implementation(project(":event-stream-rpc-model"))
    implementation(project(":event-stream-rpc-client"))

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("software.amazon.awssdk.crt:aws-crt:0.29.9")
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
