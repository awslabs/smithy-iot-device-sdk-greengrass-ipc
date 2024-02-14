/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

rootProject.name = "smithy-java"

include(":smithy-event-stream-rpc-java")
include(":smithy-event-stream-rpc-python")
include(":smithy-event-stream-rpc-cpp")
include(":smithy-event-stream-rpc-javascript")

include(":event-stream-rpc-server")
include(":event-stream-rpc-client")
include(":event-stream-rpc-model")
include(":test-model-codegen") //generates and builds from a "PetShop" model to test this set of libraries

//convenience projects for greengrass project iteration
include(":greengrass-client")
include(":greengrass-server")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
