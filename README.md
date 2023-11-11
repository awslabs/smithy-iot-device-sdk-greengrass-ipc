# Smithy IoT Device SDK for Greengrass IPC

Smithy code generators for IoT Devise SDKs that generate clients and server for Greengrass IPC.

## Build

```
./gradlew clean build
```

## Deploying Local Uber JAR to GG Maven repo

- Run `./gradlew clean build` to build local changes. Should be able to find generated classes in
/greengrass-client/build/classes/java/main/software
- Check for the latest version of SDK in maven [here](https://mvnrepository.com/artifact/software.amazon.awssdk.iotdevicesdk/aws-iot-device-sdk)
- Update the **SDK_VERSION** field from [import-spec.ini](./import-spec.ini) with the SDK version to build from
- Update the **TARGET_VERSION** field from [import-spec.ini](./import-spec.ini) with the snapshot version to
deploy to gg maven repo. The version naming convention for target version is `[SDK_VERSION]-[FeatureAcronym]-[SNAPSHOT]`

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.
