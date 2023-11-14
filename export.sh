#!/usr/bin/env bash

set -ex

PREFIX=

# Usage:
# $0 <path-to-github-workspace>

[ $# -eq 1 ]
workspace=$1
[ -d $workspace ]
[ -d $workspace/${PREFIX}aws-iot-device-sdk-java-v2 ]
[ -d $workspace/${PREFIX}aws-iot-device-sdk-python-v2 ]
[ -d $workspace/${PREFIX}aws-iot-device-sdk-cpp-v2 ]
[ -d $workspace/${PREFIX}aws-iot-device-sdk-js-v2 ]

pushd $(dirname $0) > /dev/null

# Do a clean gradle install
./gradlew clean build -x test

# JS-v2 - setup
projections=(greengrass-client test-model-codegen)

# JS-v2 - copy files
repo=$workspace/${PREFIX}aws-iot-device-sdk-js-v2
for pkg in "${projections[@]}"; do
    cp -Rv ./${pkg}/build/smithyprojections/${pkg}/source/event-stream-rpc-javascript/. ${repo}
done

# Java-v2 - setup
libs=(event-stream-rpc-client event-stream-rpc-model)
projections=(greengrass-client)

repo=$workspace/${PREFIX}aws-iot-device-sdk-java-v2

# Java-v2 - add generated comment and copyright text
for entry in `find ./greengrass-client/build/smithyprojections/greengrass-client/source/event-stream-rpc-java -type f`; do
    if [[ $entry =~ \.java$ ]]; then # only do .java files
        printf "/**\n * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.\n * SPDX-License-Identifier: Apache-2.0.\n *\n * This file is generated.\n */\n\n" | cat - $entry > temp && mv temp $entry
    fi
done
for entry in `find ./greengrass-server/build/smithyprojections/greengrass-server/source/event-stream-rpc-java -type f`; do
    if [[ $entry =~ \.java$ ]]; then # only do .java files
        printf "/**\n * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.\n * SPDX-License-Identifier: Apache-2.0.\n *\n * This file is generated.\n */\n\n" | cat - $entry > temp && mv temp $entry
    fi
done

# Java-v2 - copy files (first greengrass then event-stream-rpc)
for pkg in "${libs[@]}"; do
    mkdir -p ${repo}/sdk/greengrass/${pkg}/src/main/java
    cp -Rv ./${pkg}/src/* ${repo}/sdk/greengrass/${pkg}/src/
done
for pkg in "${projections[@]}"; do
    mkdir -p ${repo}/sdk/greengrass/${pkg}/src/event-stream-rpc-java
    cp -Rv ./${pkg}/build/smithyprojections/${pkg}/source/event-stream-rpc-java/* ${repo}/sdk/greengrass/${pkg}/src/event-stream-rpc-java/
done

# Python-v2 - setup
projections=(greengrass-client test-model-codegen)

# Python-v2 - copy files (event-stream-rpc)
repo=$workspace/${PREFIX}aws-iot-device-sdk-python-v2
for pkg in "${projections[@]}"; do
    cp -Rv ./${pkg}/build/smithyprojections/${pkg}/source/event-stream-rpc-python/. ${repo}
done

# C++-v2 - setup
projections=(greengrass-client test-model-codegen)

# C++-v2 - Check if clang-format-8 is installed
if NOT type clang-format-8 2> /dev/null ; then
    echo "No appropriate clang-format-8 found."
    exit 1
fi

# C++-v2 - additional setup
repo=$workspace/${PREFIX}aws-iot-device-sdk-cpp-v2

# Verify that the cpp-v2 repo contains a .clang-format file and copy it
if [ ! -f $repo/.clang-format ]; then
    echo "No .clang-format in $repo could be found"
    exit 1
else
    cp $repo/.clang-format $workspace
fi

# C++-v2 - copy files (event-stream-rpc)
for pkg in "${projections[@]}"; do
    find ./${pkg}/build/smithyprojections/${pkg}/source/event-stream-rpc-cpp -iname *.h -o -iname *.cpp | xargs clang-format-8 -i -style=file
    cp -Rv ./${pkg}/build/smithyprojections/${pkg}/source/event-stream-rpc-cpp/. ${repo}
done

# C++-v2 - Clean up copied clang-format file
rm $workspace/.clang-format

popd > /dev/null
