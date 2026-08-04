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

# C++-v2 - additional setup
repo=$workspace/${PREFIX}aws-iot-device-sdk-cpp-v2

# C++-v2 - copy files (event-stream-rpc)
for pkg in "${projections[@]}"; do
    cp -Rv ./${pkg}/build/smithyprojections/${pkg}/source/event-stream-rpc-cpp/. ${repo}
done

popd > /dev/null

# Format C++v2
pushd ${repo} > /dev/null

python3 format-check.py -i

popd > /dev/null
