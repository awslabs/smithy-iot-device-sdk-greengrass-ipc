// The smithy IDL file describing the IPC APIs for Greengrass.
// API design doc - https://quip-amazon.com/mbN5ATEk6Db6/IPC-SDK-API-Design

namespace aws.greengrass

/// Provides communication between Greengrass core and customer component
service GreengrassCoreIPC {
    version: "2020-06-08",
    operations: [
        UpdateState,
        SubscribeToComponentUpdates,
        DeferComponentUpdate,
        GetConfiguration,
        UpdateConfiguration,
        SubscribeToConfigurationUpdate,
        SubscribeToValidateConfigurationUpdates,
        SendConfigurationValidityReport,
        SubscribeToTopic,
        PublishToTopic,
        GetComponentDetails,
        RestartComponent,
        StopComponent,
        CreateLocalDeployment,
        CancelLocalDeployment,
        GetLocalDeploymentStatus,
        ListLocalDeployments,
        ListComponents,
        PublishToIoTCore,
        SubscribeToIoTCore,
        ValidateAuthorizationToken,
        GetSecretValue,
        CreateDebugPassword,
        GetThingShadow,
        UpdateThingShadow,
        DeleteThingShadow,
        ListNamedShadowsForThing,
        PauseComponent,
        ResumeComponent,
        SubscribeToCertificateUpdates,
        VerifyClientDeviceIdentity,
        GetClientDeviceAuthToken,
        AuthorizeClientDeviceAction,
        PutComponentMetric
    ]
}

//-----------Operations--------------------

/// Update status of this component
operation UpdateState {
    input:  UpdateStateRequest,
    output:  UpdateStateResponse,
    errors: [ServiceError, ResourceNotFoundError]
}

/// Subscribe to receive notification if GGC is about to update any components
operation SubscribeToComponentUpdates {
    input: SubscribeToComponentUpdatesRequest,
    output: SubscribeToComponentUpdatesResponse,
    errors: [ServiceError, ResourceNotFoundError]
}

/// Defer the update of components by a given amount of time and check again after that.
operation DeferComponentUpdate {
    input: DeferComponentUpdateRequest,
    output: DeferComponentUpdateResponse,
    errors: [ServiceError, ResourceNotFoundError, InvalidArgumentsError]
}

/// Get value of a given key from the configuration
operation GetConfiguration {
    input: GetConfigurationRequest,
    output: GetConfigurationResponse,
    errors: [ServiceError, ResourceNotFoundError]
}

/// Update this component's configuration by replacing the value of given keyName with the newValue.
/// If an oldValue is specified then update will only take effect id the current value matches the given oldValue
operation UpdateConfiguration {
    input: UpdateConfigurationRequest,
    output: UpdateConfigurationResponse,
    errors: [ServiceError, UnauthorizedError, ConflictError, FailedUpdateConditionCheckError, InvalidArgumentsError]
}

/// Subscribes to be notified when GGC updates the configuration for a given componentName and keyName.
operation SubscribeToConfigurationUpdate {
    input: SubscribeToConfigurationUpdateRequest,
    output: SubscribeToConfigurationUpdateResponse,
    errors: [ServiceError, ResourceNotFoundError]
}

/// Subscribes to be notified when GGC is about to update configuration for this component
/// GGC will wait for a timeout period before it proceeds with the update.
/// If the new configuration is not valid this component can use the SendConfigurationValidityReport
/// operation to indicate that
operation SubscribeToValidateConfigurationUpdates {
    input: SubscribeToValidateConfigurationUpdatesRequest,
    output: SubscribeToValidateConfigurationUpdatesResponse,
    errors: [ServiceError]
}

/// This operation should be used in response to event received as part of SubscribeToValidateConfigurationUpdates
/// subscription. It is not necessary to send the report if the configuration is valid (GGC will wait for timeout
/// period and proceed). Sending the report with invalid config status will prevent GGC from applying the updates
operation SendConfigurationValidityReport {
    input: SendConfigurationValidityReportRequest,
    output: SendConfigurationValidityReportResponse,
    errors: [InvalidArgumentsError, ServiceError]
}

/// Creates a subscription for a custom topic
operation SubscribeToTopic {
    input:  SubscribeToTopicRequest,
    output: SubscribeToTopicResponse,
    errors: [InvalidArgumentsError, ServiceError, UnauthorizedError]
}

/// Publish to a custom topic.
operation PublishToTopic {
    input: PublishToTopicRequest,
    output: PublishToTopicResponse,
    errors: [ServiceError, UnauthorizedError]
}

/// Gets the status and version of the component with the given component name
operation GetComponentDetails {
    input: GetComponentDetailsRequest,
    output: GetComponentDetailsResponse,
    errors: [ServiceError, ResourceNotFoundError, InvalidArgumentsError]
}

/// Restarts a component with the given name
operation RestartComponent {
    input: RestartComponentRequest,
    output: RestartComponentResponse,
    errors: [ServiceError, ComponentNotFoundError, InvalidArgumentsError]
}

/// Stops a component with the given name
operation StopComponent {
    input: StopComponentRequest,
    output: StopComponentResponse,
    errors: [ServiceError, ComponentNotFoundError, InvalidArgumentsError]
}

/// Creates a local deployment on the device.  Also allows to remove existing components.
operation CreateLocalDeployment {
    input: CreateLocalDeploymentRequest,
    output: CreateLocalDeploymentResponse,
    errors: [ServiceError, InvalidRecipeDirectoryPathError,
             InvalidArtifactsDirectoryPathError, InvalidArgumentsError]
}

/// Cancel a local deployment on the device.
operation CancelLocalDeployment {
    input: CancelLocalDeploymentRequest,
    output: CancelLocalDeploymentResponse,
    errors: [ServiceError, ResourceNotFoundError, InvalidArgumentsError]
}

/// Get status of a local deployment with the given deploymentId
operation GetLocalDeploymentStatus {
    input: GetLocalDeploymentStatusRequest,
    output: GetLocalDeploymentStatusResponse,
    errors: [ServiceError, ResourceNotFoundError]
}

/// Lists the last 5 local deployments along with their statuses
operation ListLocalDeployments {
    input: ListLocalDeploymentsRequest,
    output: ListLocalDeploymentsResponse,
    errors: [ServiceError]
}

/// Request for a list of components
operation ListComponents {
    input: ListComponentsRequest,
    output: ListComponentsResponse,
    errors: [ServiceError]
}

/// Publish an MQTT message to AWS IoT message broker
operation PublishToIoTCore {
    input: PublishToIoTCoreRequest,
    output: PublishToIoTCoreResponse,
    errors: [ServiceError, UnauthorizedError]
}

/// Subscribe to a topic in AWS IoT message broker.
operation SubscribeToIoTCore {
    input: SubscribeToIoTCoreRequest,
    output: SubscribeToIoTCoreResponse,
    errors: [ServiceError, UnauthorizedError]
}

/// Validate authorization token
/// NOTE This API can be used only by stream manager, customer component calling this API will receive UnauthorizedError
operation ValidateAuthorizationToken {
    input: ValidateAuthorizationTokenRequest,
    output: ValidateAuthorizationTokenResponse,
    errors: [InvalidTokenError, UnauthorizedError, ServiceError]
}

/// Retrieves a secret stored in AWS secrets manager
operation GetSecretValue {
    input: GetSecretValueRequest,
    output: GetSecretValueResponse,
    errors: [UnauthorizedError, ResourceNotFoundError, ServiceError]
}

/// Generate a password for the HttpDebugView component
operation CreateDebugPassword {
    input: CreateDebugPasswordRequest,
    output: CreateDebugPasswordResponse,
    errors: [UnauthorizedError, ServiceError]
}

/// Retrieves a device shadow document stored by the local shadow service
operation GetThingShadow {
    input: GetThingShadowRequest,
    output: GetThingShadowResponse,
    errors: [InvalidArgumentsError, ResourceNotFoundError, ServiceError, UnauthorizedError]
}

/// Updates a device shadow document stored in the local shadow service
/// The update is an upsert operation, with optimistic locking support
operation UpdateThingShadow {
    input: UpdateThingShadowRequest,
    output: UpdateThingShadowResponse,
    errors: [InvalidArgumentsError, ConflictError, ServiceError, UnauthorizedError]
}

/// Deletes a device shadow document stored in the local shadow service
operation DeleteThingShadow {
    input: DeleteThingShadowRequest,
    output: DeleteThingShadowResponse,
    errors: [InvalidArgumentsError, ResourceNotFoundError, ServiceError, UnauthorizedError]
}

/// Lists the named shadows for the specified thing
operation ListNamedShadowsForThing {
    input: ListNamedShadowsForThingRequest,
    output: ListNamedShadowsForThingResponse,
    errors: [InvalidArgumentsError, ResourceNotFoundError, ServiceError, UnauthorizedError]
}

/// Pause a running component
operation PauseComponent {
    input:  PauseComponentRequest,
    output:  PauseComponentResponse,
    errors: [UnauthorizedError, ServiceError, ResourceNotFoundError]
}

/// Resume a paused component
operation ResumeComponent {
    input:  ResumeComponentRequest,
    output:  ResumeComponentResponse,
    errors: [UnauthorizedError, ServiceError, ResourceNotFoundError]
}

/// Create a subscription for new certificates
operation SubscribeToCertificateUpdates {
    input: SubscribeToCertificateUpdatesRequest,
    output: SubscribeToCertificateUpdatesResponse,
    errors: [ServiceError, UnauthorizedError, InvalidArgumentsError]
}

/// Verify client device credentials
operation VerifyClientDeviceIdentity {
    input: VerifyClientDeviceIdentityRequest,
    output: VerifyClientDeviceIdentityResponse,
    errors: [UnauthorizedError, ServiceError, InvalidArgumentsError]
}

/// Get session token for a client device
operation GetClientDeviceAuthToken {
    input: GetClientDeviceAuthTokenRequest,
    output: GetClientDeviceAuthTokenResponse,
    errors: [UnauthorizedError, ServiceError, InvalidArgumentsError, InvalidCredentialError]
}

/// Send a request to authorize action on some resource
operation AuthorizeClientDeviceAction {
     input: AuthorizeClientDeviceActionRequest,
     output: AuthorizeClientDeviceActionResponse,
     errors: [UnauthorizedError, ServiceError, InvalidArgumentsError, InvalidClientDeviceAuthTokenError]
 }

/// Send component metrics
/// NOTE Only usable by AWS components
operation PutComponentMetric {
    input: PutComponentMetricRequest,
    output: PutComponentMetricResponse,
    errors: [UnauthorizedError, ServiceError, InvalidArgumentsError]
}

//-----------Shapes------------------------

structure SubscribeToCertificateUpdatesRequest {
    @required
    certificateOptions: CertificateOptions
}

structure CertificateOptions {
    /// The types of certificate updates to subscribe to.
    @required
    certificateType: CertificateType,
}

structure SubscribeToCertificateUpdatesResponse {
    messages: CertificateUpdateEvent
}

@streaming
union CertificateUpdateEvent {
    /// The information about the new certificate.
    certificateUpdate: CertificateUpdate
}

@sensitive
structure CertificateUpdate {
    /// The private key in pem format.
    privateKey: String,
    /// The public key in pem format.
    publicKey: String,
    /// The certificate in pem format.
    certificate: String,
    /// List of CA certificates in pem format.
    caCertificates: CACertificates
}

list CACertificates{
    member: String
}

structure GetClientDeviceAuthTokenRequest {
    /// The client device's credentials.
    @required
    credential: CredentialDocument
 }

 union CredentialDocument {
     /// The client device's MQTT credentials. Specify the client ID and certificate that the client device uses to connect.
     mqttCredential: MQTTCredential
 }

 structure MQTTCredential {
     /// The client ID to used to connect.
     clientId: String,
     /// The client certificate in pem format.
     certificatePem: String,
     /// The username. (unused).
     username: String,
     /// The password. (unused).
     password: String
 }

 @sensitive
 structure GetClientDeviceAuthTokenResponse {
    /// The session token for the client device. You can use this session token in subsequent requests to authorize this client device's actions.
    @required
    clientDeviceAuthToken: String
 }


structure AuthorizeClientDeviceActionRequest {
    /// The session token for the client device from GetClientDeviceAuthToken.
    @required
    clientDeviceAuthToken: String,
    /// The operation to authorize.
    @required
    operation: String,
    /// The resource the client device performs the operation on.
    @required
    resource: String
}

structure AuthorizeClientDeviceActionResponse {
    /// Whether the client device is authorized to perform the operation on the resource.
    @required
    isAuthorized: Boolean
}

structure UpdateStateRequest {
    /// The state to set this component to.
    @required
    state: ReportedLifecycleState
}

structure SubscribeToComponentUpdatesResponse {
    messages: ComponentUpdatePolicyEvents
}

@streaming
union ComponentUpdatePolicyEvents {
    /// An event that indicates that the Greengrass wants to update a component.
    preUpdateEvent: PreComponentUpdateEvent,
    /// An event that indicates that the nucleus updated a component.
    postUpdateEvent: PostComponentUpdateEvent
}

structure PreComponentUpdateEvent {
    /// The ID of the AWS IoT Greengrass deployment that updates the component.
    @required
    deploymentId: String,
    /// Whether or not Greengrass needs to restart to apply the update.
    @required
    isGgcRestarting: Boolean
}

structure PostComponentUpdateEvent {
    /// The ID of the AWS IoT Greengrass deployment that updated the component.
    @required
    deploymentId: String
}

structure DeferComponentUpdateRequest {
    /// The ID of the AWS IoT Greengrass deployment to defer.
    @required
    deploymentId: String,
    /// (Optional) The name of the component for which to defer updates. Defaults to the name of the component that makes the request.
    message: String,
    /// The amount of time in milliseconds for which to defer the update. Greengrass waits for this amount of time and then sends another PreComponentUpdateEvent
    recheckAfterMs: Long
}

list KeyPath {
    member: String
}

structure GetConfigurationRequest {
    /// (Optional) The name of the component. Defaults to the name of the component that makes the request.
    componentName: String,
    /// The key path to the configuration value. Specify a list where each entry is the key for a single level in the configuration object.
    @required
    keyPath: KeyPath
}

structure GetConfigurationResponse {
    /// The name of the component.
    componentName: String,
    /// The requested configuration as an object.
    value: Document
}

structure UpdateConfigurationRequest {
    /// (Optional) The key path to the container node (the object) to update. Specify a list where each entry is the key for a single level in the configuration object. Defaults to the root of the configuration object.
    keyPath: KeyPath,
    /// The current Unix epoch time in milliseconds. This operation uses this timestamp to resolve concurrent updates to the key. If the key in the component configuration has a greater timestamp than the timestamp in the request, then the request fails.
    @required
    timestamp: Timestamp,
    /// The configuration object to merge at the location that you specify in keyPath.
    @required
    valueToMerge: Document
}

structure SubscribeToConfigurationUpdateRequest {
    /// (Optional) The name of the component. Defaults to the name of the component that makes the request.
    componentName: String,
    /// The key path to the configuration value for which to subscribe. Specify a list where each entry is the key for a single level in the configuration object.
    @required
    keyPath: KeyPath
}

structure SubscribeToConfigurationUpdateResponse {
    messages: ConfigurationUpdateEvents
}

@streaming
union ConfigurationUpdateEvents {
    /// The configuration update event.
    configurationUpdateEvent: ConfigurationUpdateEvent
}

structure ConfigurationUpdateEvent {
    /// The name of the component.
    @required
    componentName: String,
    /// The key path to the configuration value that updated.
    @required
    keyPath: KeyPath
}

structure SubscribeToValidateConfigurationUpdatesResponse {
    messages: ValidateConfigurationUpdateEvents
}

@streaming
union ValidateConfigurationUpdateEvents {
    /// The configuration update event.
    validateConfigurationUpdateEvent: ValidateConfigurationUpdateEvent
}

structure ValidateConfigurationUpdateEvent {
    /// The object that contains the new configuration.
    configuration: Document,
    /// The ID of the AWS IoT Greengrass deployment that updates the component.
    @required
    deploymentId: String
}

structure ConfigurationValidityReport {
    /// The validity status.
    @required
    status: ConfigurationValidityStatus,
    /// The ID of the AWS IoT Greengrass deployment that requested the configuration update.
    @required
    deploymentId: String,
    /// (Optional) A message that reports why the configuration isn't valid.
    message: String
}

structure SendConfigurationValidityReportRequest {
    /// The report that tells Greengrass whether or not the configuration update is valid.
    @required
    configurationValidityReport: ConfigurationValidityReport
}

structure SubscribeToTopicRequest {
    /// The topic to subscribe to. Supports MQTT-style wildcards.
    @required
    topic: String,
    /// (Optional) The behavior that specifies whether the component receives messages from itself.
    receiveMode: ReceiveMode
}

structure SubscribeToTopicResponse {
    @deprecated(message: "No longer used")
    topicName: String,
    messages: SubscriptionResponseMessage
}

@streaming
union SubscriptionResponseMessage {
    /// (Optional) A JSON message.
    jsonMessage: JsonMessage,
    /// (Optional) A binary message.
    binaryMessage: BinaryMessage
}

structure PublishToTopicRequest {
    /// The topic to publish the message.
    @required
    topic: String,
    /// The message to publish.
    @required
    publishMessage: PublishMessage
}

structure PublishToTopicResponse { }

union PublishMessage {
    /// (Optional) A JSON message.
    jsonMessage: JsonMessage,
    /// (Optional) A binary message.
    binaryMessage: BinaryMessage
}

@documentation("Contextual information about the message.\nNOTE The context is ignored if used in PublishMessage.")
structure MessageContext {
    /// The topic where the message was published.
    topic: String
}

structure JsonMessage {
    /// The JSON message as an object.
    message: Document,
    /// The context of the message, such as the topic where the message was published.
    context: MessageContext
}

structure BinaryMessage {
    /// The binary message as a blob.
    message: Blob,
    /// The context of the message, such as the topic where the message was published.
    context: MessageContext
}

structure GetComponentDetailsRequest {
    /// The name of the component to get.
    @required
    componentName: String
}

structure GetComponentDetailsResponse {
    /// The component's details.
    @required
    componentDetails: ComponentDetails
}

structure ComponentDetails {
    /// The name of the component.
    @required
    componentName: String,
    /// The version of the component.
    @required
    version: String,
    /// The state of the component.
    @required
    state: LifecycleState,
    /// The component's configuration as a JSON object.
    configuration: Document
}

structure RestartComponentRequest {
    /// The name of the component.
    @required
    componentName: String
}

structure RestartComponentResponse {
    /// The status of the restart request.
    @required
    restartStatus: RequestStatus,
    /// A message about why the component failed to restart, if the request failed.
    message: String
}

structure StopComponentRequest {
    /// The name of the component.
    @required
    componentName: String
}

structure StopComponentResponse {
    /// The status of the stop request.
    @required
    stopStatus: RequestStatus,
    /// A message about why the component failed to stop, if the request failed.
    message: String
}

structure CreateLocalDeploymentRequest {
    // None of the members are required by themselves but this structure cannot be empty

    /// The thing group name the deployment is targeting. If the group name is not specified, "LOCAL_DEPLOYMENT" will be used.
    groupName: String,
    /// Map of component name to version. Components will be added to the group's existing root components.
    rootComponentVersionsToAdd: ComponentToVersionMap,
    /// List of components that need to be removed from the group, for example if new artifacts were loaded in this request but recipe version did not change.
    rootComponentsToRemove: ComponentList,
    /// Map of component names to configuration.
    componentToConfiguration: ComponentToConfiguration,
    /// Map of component names to component run as info.
    componentToRunWithInfo: ComponentToRunWithInfo,
    /// All recipes files in this directory will be copied over to the Greengrass package store.
    recipeDirectoryPath: String,
    /// All artifact files in this directory will be copied over to the Greengrass package store.
    artifactsDirectoryPath: String,
    /// Deployment failure handling policy.
    failureHandlingPolicy: FailureHandlingPolicy
}

structure CreateLocalDeploymentResponse {
    /// The ID of the local deployment that the request created.
    deploymentId: String
}

list ComponentList {
    member: String
}

map ComponentToVersionMap {
    key: String,
    value: String
}

map ComponentToConfiguration {
    key: String,
    value: Document
}

map ComponentToRunWithInfo {
    key: String,
    value: RunWithInfo
}

structure RunWithInfo {
    /// (Optional) The POSIX system user and, optionally, group to use to run this component on Linux core devices.
    posixUser: String,
    /// (Optional) The Windows user to use to run this component on Windows core devices.
    windowsUser: String,
    /// (Optional) The system resource limits to apply to this component's processes.
    systemResourceLimits: SystemResourceLimits
}

structure SystemResourceLimits {
    /// (Optional) The maximum amount of RAM (in kilobytes) that this component's processes can use on the core device.
    memory: Long,
    /// (Optional) The maximum amount of CPU time that this component's processes can use on the core device.
    cpus: Double
}

structure CancelLocalDeploymentRequest {
    /// (Optional) The ID of the local deployment to cancel.
    deploymentId: String
}

structure CancelLocalDeploymentResponse {
    message: String
}

structure GetLocalDeploymentStatusRequest {
    /// The ID of the local deployment to get.
    @required
    deploymentId: String
}

structure GetLocalDeploymentStatusResponse {
    /// The local deployment.
    @required
    deployment: LocalDeployment
}

structure LocalDeployment {
    /// The ID of the local deployment.
    @required
    deploymentId: String,
    /// The status of the local deployment.
    @required
    status: DeploymentStatus,
    /// (Optional) The timestamp at which the local deployment was created in MM/dd/yyyy hh:mm:ss format
    createdOn: String,
    /// (Optional) The status details of the local deployment.
    deploymentStatusDetails: DeploymentStatusDetails
}

structure DeploymentStatusDetails {
    /// The detailed deployment status of the local deployment.
    @required
    detailedDeploymentStatus: DetailedDeploymentStatus,
    /// (Optional) The list of local deployment errors
    deploymentErrorStack: DeploymentErrorStack,
    /// (Optional) The list of local deployment error types
    deploymentErrorTypes: DeploymentErrorTypes,
    /// (Optional) The cause of local deployment failure
    deploymentFailureCause: String
}

list DeploymentErrorStack {
    member: String
}

list DeploymentErrorTypes {
    member: String
}

structure ListLocalDeploymentsResponse {
    /// The list of local deployments.
    localDeployments: ListOfLocalDeployments
}

list ListOfLocalDeployments {
    member: LocalDeployment
}

structure ListComponentsResponse {
    /// The list of components.
    components: ListOfComponents
}

list ListOfComponents {
    member: ComponentDetails
}

structure UserProperty {
    key: String,
    value: String
}

list ListOfUserProperties {
    member: UserProperty
}

structure PublishToIoTCoreRequest {
    /// The topic to which to publish the message.
    @required
    topicName: String,
    /// The MQTT QoS to use.
    @required
    qos: QOS,
    /// (Optional) The message payload as a blob.
    payload: Blob,
    /// (Optional) Whether to set MQTT retain option to true when publishing.
    retain: Boolean,
    /// (Optional) MQTT user properties associated with the message.
    userProperties: ListOfUserProperties,
    /// (Optional) Message expiry interval in seconds.
    messageExpiryIntervalSeconds: Long,
    /// (Optional) Correlation data blob for request/response.
    correlationData: Blob,
    /// (Optional) Response topic for request/response.
    responseTopic: String,
    /// (Optional) Message payload format.
    payloadFormat: PayloadFormat,
    /// (Optional) Message content type.
    contentType: String
}

structure SubscribeToIoTCoreRequest {
    /// The topic to which to subscribe. Supports MQTT wildcards.
    @required
    topicName: String,
    /// The MQTT QoS to use.
    @required
    qos: QOS,
}

structure SubscribeToIoTCoreResponse {
    messages: IoTCoreMessage
}

@streaming
union IoTCoreMessage {
    /// The MQTT message.
    message: MQTTMessage
}

structure MQTTMessage {
    /// The topic to which the message was published.
    @required
    topicName: String,
    /// (Optional) The message payload as a blob.
    payload: Blob,
    /// (Optional) The value of the retain flag.
    retain: Boolean,
    /// (Optional) MQTT user properties associated with the message.
    userProperties: ListOfUserProperties,
    /// (Optional) Message expiry interval in seconds.
    messageExpiryIntervalSeconds: Long,
    /// (Optional) Correlation data blob for request/response.
    correlationData: Blob,
    /// (Optional) Response topic for request/response.
    responseTopic: String,
    /// (Optional) Message payload format.
    payloadFormat: PayloadFormat,
    /// (Optional) Message content type.
    contentType: String
}

structure ValidateAuthorizationTokenRequest {
    @required
    token: String
}

structure ValidateAuthorizationTokenResponse {
    @required
    isValid: Boolean
}

@sensitive
union SecretValue {
    /// The decrypted part of the protected secret information that you provided to Secrets Manager as a string.
    secretString: String,
    /// (Optional) The decrypted part of the protected secret information that you provided to Secrets Manager as binary data in the form of a byte array.
    secretBinary: Blob
}

list SecretVersionList {
    member: String
}

structure GetSecretValueRequest {
    /// The name of the secret to get. You can specify either the Amazon Resource Name (ARN) or the friendly name of the secret.
    @required
    secretId: String,
    /// (Optional) The ID of the version to get. If you don't specify versionId or versionStage, this operation defaults to the version with the AWSCURRENT label.
    versionId: String,
    /// (Optional) The staging label of the version to get. If you don't specify versionId or versionStage, this operation defaults to the version with the AWSCURRENT label.
    versionStage: String
}

structure GetSecretValueResponse {
    /// The ID of the secret.
    @required
    secretId: String,
    /// The ID of this version of the secret.
    @required
    versionId: String,
    /// The list of staging labels attached to this version of the secret.
    @required
    versionStage: SecretVersionList,
    /// The value of this version of the secret.
    @required
    secretValue: SecretValue
}

structure CreateDebugPasswordRequest {
}

structure CreateDebugPasswordResponse {
    @required
    password: String,
    @required
    username: String,
    @required
    passwordExpiration: Timestamp,
    certificateSHA256Hash: String,
    certificateSHA1Hash: String,
}

structure GetThingShadowRequest {
    /// The name of the thing.
    @required
    thingName: String,
    /// The name of the shadow. To specify the thing's classic shadow, set this parameter to an empty string ("").
    shadowName: String
}

structure GetThingShadowResponse {
    /// The response state document as a JSON encoded blob.
    @required
    payload: Blob
}

structure UpdateThingShadowRequest {
    /// The name of the thing.
    @required
    thingName: String,
    /// The name of the shadow. To specify the thing's classic shadow, set this parameter to an empty string ("").
    shadowName: String,
    /// The request state document as a JSON encoded blob.
    @required
    payload: Blob
}

structure UpdateThingShadowResponse {
    /// The response state document as a JSON encoded blob.
    @required
    payload: Blob
}

structure DeleteThingShadowRequest {
    /// The name of the thing.
    @required
    thingName: String,
    /// The name of the shadow. To specify the thing's classic shadow, set this parameter to an empty string ("").
    shadowName: String
}

structure DeleteThingShadowResponse {
    /// An empty response state document.
    @required
    payload: Blob
}

list NamedShadowList {
    member: String
}

structure ListNamedShadowsForThingRequest {
    /// The name of the thing.
    @required
    thingName: String,
    /// (Optional) The token to retrieve the next set of results. This value is returned on paged results and is used in the call that returns the next page.
    nextToken: String,
    /// (Optional) The number of shadow names to return in each call. Value must be between 1 and 100. Default is 25.
    @range(min: 1, max: 100)
    pageSize: Integer
}

structure ListNamedShadowsForThingResponse {
    /// The list of shadow names.
    @required
    results: NamedShadowList,
    /// (Optional) The date and time that the response was generated.
    @required
    timestamp: Timestamp,
    /// (Optional) The token value to use in paged requests to retrieve the next page in the sequence. This token isn't present when there are no more shadow names to return.
    nextToken: String
}

structure PauseComponentRequest {
    /// The name of the component to pause, which must be a generic component.
    @required
    componentName: String
}

structure ResumeComponentRequest {
    /// The name of the component to resume.
    @required
    componentName: String
}

structure VerifyClientDeviceIdentityRequest {
    /// The client device's credentials.
    @required
    credential: ClientDeviceCredential
}

union ClientDeviceCredential {
    /// The client device's X.509 device certificate.
    clientDeviceCertificate: String
}

structure VerifyClientDeviceIdentityResponse {
    /// Whether the client device's identity is valid.
    @required
    isValidClientDevice: Boolean
}

structure PutComponentMetricRequest {
    @required
    metrics: MetricsList
}

list MetricsList {
    member: Metric
}

structure Metric {
    @required
    name: String,
    @required
    unit: MetricUnitType,
    @required
    value: Double
}

//----------enums-----------------------

@enum([
    {
        value: "RUNNING",
        name: "RUNNING"
    },
    {
        value:"ERRORED",
        name:"ERRORED"
    },
    {
        value:"NEW",
        name:"NEW"
    },
    {
        value:"FINISHED",
        name:"FINISHED"
    },
    {
        value:"INSTALLED",
        name:"INSTALLED"
    },
    {
        value:"BROKEN",
        name:"BROKEN"
    },
    {
        value:"STARTING",
        name:"STARTING"
    },
    {
        value:"STOPPING",
        name:"STOPPING"
    }
])
string LifecycleState

@enum([
    {
        value: "RUNNING",
        name: "RUNNING"
    },
    {
        value:"ERRORED",
        name:"ERRORED"
    }
])
string ReportedLifecycleState

@enum([
    {
        value: "ACCEPTED",
        name: "ACCEPTED"
    },
    {
        value: "REJECTED",
        name: "REJECTED"
    }
])
string ConfigurationValidityStatus

@enum([
    {
        value: "SUCCEEDED",
        name: "SUCCEEDED"
    },
    {
        value: "FAILED",
        name: "FAILED"
    }
])
string RequestStatus

@enum([
    {
        value: "QUEUED",
        name: "QUEUED"
    },
    {
        value:"IN_PROGRESS",
        name:"IN_PROGRESS"
    },
    {
        value:"SUCCEEDED",
        name:"SUCCEEDED"
    },
    {
        value:"FAILED",
        name:"FAILED"
    },
    {
        value:"CANCELED",
        name:"CANCELED"
    }
])
string DeploymentStatus

// values for QOS are not finalised yet.
@enum([
    {
        value: "0",
        name: "AT_MOST_ONCE"
    },
    {
        value: "1",
        name: "AT_LEAST_ONCE"

    }
])
string QOS

@enum([
    {
        value: "0",
        name: "BYTES"
    },
    {
        value: "1",
        name: "UTF8"

    }
])
string PayloadFormat

@enum([
    {
        value: "RECEIVE_ALL_MESSAGES",
        name: "RECEIVE_ALL_MESSAGES"
    },
    {
        value: "RECEIVE_MESSAGES_FROM_OTHERS",
        name: "RECEIVE_MESSAGES_FROM_OTHERS"
    }
])
string ReceiveMode

@enum([
    {
        value: "SERVER",
        name: "SERVER"
    }
])
string CertificateType

@enum([
    {
        value: "BYTES",
        name: "BYTES"
    },
    {
        value: "BYTES_PER_SECOND",
        name: "BYTES_PER_SECOND"
    },
    {
        value: "COUNT",
        name: "COUNT"
    },
    {
        value: "COUNT_PER_SECOND",
        name: "COUNT_PER_SECOND"
    },
    {
        value: "MEGABYTES",
        name: "MEGABYTES"
    },
    {
        value: "SECONDS",
        name: "SECONDS"
    }
])
string MetricUnitType

@enum([
    {
        value: "ROLLBACK",
        name: "ROLLBACK",
    },
    {
        value: "DO_NOTHING",
        name: "DO_NOTHING",
    }
])
string FailureHandlingPolicy

@enum([
    {
        value: "SUCCESSFUL",
        name: "SUCCESSFUL"
    },
    {
        value: "FAILED_NO_STATE_CHANGE",
        name: "FAILED_NO_STATE_CHANGE"
    },
    {
        value: "FAILED_ROLLBACK_NOT_REQUESTED",
        name: "FAILED_ROLLBACK_NOT_REQUESTED"
    },
    {
        value: "FAILED_ROLLBACK_COMPLETE",
        name: "FAILED_ROLLBACK_COMPLETE"
    },
    {
        value: "REJECTED",
        name: "REJECTED"
    }
])
string DetailedDeploymentStatus

//----------errors----------------------

@error("client")
structure UnauthorizedError {
    message: String
}

@error("client")
structure ResourceNotFoundError {
    message: String,
    resourceType: String,
    resourceName: String
}

@error("client")
structure ConflictError {
    message: String
}

@error("client")
structure ComponentNotFoundError {
    message: String
}

@error("client")
structure InvalidRecipeDirectoryPathError {
    message: String
}

@error("client")
structure InvalidArtifactsDirectoryPathError {
    message: String
}

@error("client")
structure InvalidArgumentsError {
    message: String
}

@error("client")
structure FailedUpdateConditionCheckError {
    message: String
}

@error("server")
structure ServiceError {
    message: String,
    context: Document
}

@error("server")
structure InvalidTokenError {
    message: String
}

@error("client")
structure InvalidCredentialError {
    message: String
}

@error("client")
structure InvalidClientDeviceAuthTokenError {
    message: String
}

// Empty structures follow
structure SubscribeToValidateConfigurationUpdatesRequest {}
structure SubscribeToComponentUpdatesRequest {}
structure UpdateStateResponse {}
structure DeferComponentUpdateResponse {}
structure UpdateConfigurationResponse {}
structure SendConfigurationValidityReportResponse {}
structure ListLocalDeploymentsRequest {}
structure ListComponentsRequest {}
structure PublishToIoTCoreResponse {}
structure PauseComponentResponse {}
structure ResumeComponentResponse {}
structure PutComponentMetricResponse {}
