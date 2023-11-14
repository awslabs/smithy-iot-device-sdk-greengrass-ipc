<#include "DirectiveUtils.ftl">
<#macro newOperation indents operation context>
    <#local OperationPascalCaseName = operation.getId().getName()/>
<#if context.getOutputEventStreamInfo(operation).isPresent()>
<@placeIndents quantity=indents/>void ${OperationPascalCaseName}StreamHandler::OnStreamEvent(Aws::Crt::ScopedResource<AbstractShapeBase> response)
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    OnStreamEvent(static_cast<${context.getStreamingResponseClassName(operation)}*>(response.get()));
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>bool ${OperationPascalCaseName}StreamHandler::OnStreamError(Aws::Crt::ScopedResource<OperationError> operationError,
<@placeIndents quantity=indents/>                                                            RpcError rpcError)
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    bool streamShouldTerminate = false;
<@placeIndents quantity=indents/>    if (rpcError.baseStatus != EVENT_STREAM_RPC_SUCCESS) {
<@placeIndents quantity=indents/>        streamShouldTerminate = OnStreamError(rpcError);
<@placeIndents quantity=indents/>    }
<#list operation.getErrors() as shapeId>
<@placeIndents quantity=indents/>    if (operationError != nullptr && operationError->GetModelName() == Aws::Crt::String("${shapeId}") && !streamShouldTerminate) {
<@placeIndents quantity=indents/>        streamShouldTerminate = OnStreamError(static_cast<${shapeId.getName()}*>(operationError.get()));
<@placeIndents quantity=indents/>    }
</#list>
<@placeIndents quantity=indents/>    if (operationError != nullptr && !streamShouldTerminate) streamShouldTerminate = OnStreamError(operationError.get());
<@placeIndents quantity=indents/>    return streamShouldTerminate;
<@placeIndents quantity=indents/>}
</#if>

<@placeIndents quantity=indents/>${OperationPascalCaseName}OperationContext::${OperationPascalCaseName}OperationContext(
<@placeIndents quantity=indents/>    const ${context.getServiceShapeName()}ServiceModel &serviceModel) noexcept
<@placeIndents quantity=indents/>    : OperationModelContext(serviceModel)
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>Aws::Crt::ScopedResource<AbstractShapeBase> ${OperationPascalCaseName}OperationContext::AllocateInitialResponseFromPayload(
<@placeIndents quantity=indents/>    Aws::Crt::StringView stringView,
<@placeIndents quantity=indents/>    Aws::Crt::Allocator *allocator) const noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    return ${context.getResponseClassName(operation)}::s_allocateFromPayload(stringView, allocator);
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>Aws::Crt::ScopedResource<AbstractShapeBase> ${OperationPascalCaseName}OperationContext::
<@placeIndents quantity=indents/>    AllocateStreamingResponseFromPayload(Aws::Crt::StringView stringView, Aws::Crt::Allocator *allocator)
<@placeIndents quantity=indents/>        const noexcept
<@placeIndents quantity=indents/>{
<#if context.getOutputEventStreamInfo(operation).isPresent()>
<@placeIndents quantity=indents/>    return ${context.getStreamingResponseClassName(operation)}::s_allocateFromPayload(stringView, allocator);
<#else>
<@placeIndents quantity=indents/>    (void)stringView;
<@placeIndents quantity=indents/>    (void)allocator;
<@placeIndents quantity=indents/>    return nullptr;
</#if>
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>Aws::Crt::String ${OperationPascalCaseName}OperationContext::GetRequestModelName() const noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    return Aws::Crt::String("${context.getRequestAppType(operation)}");
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>Aws::Crt::String ${OperationPascalCaseName}OperationContext::GetInitialResponseModelName() const noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    return Aws::Crt::String("${context.getResponseAppType(operation)}");
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>Aws::Crt::Optional<Aws::Crt::String> ${OperationPascalCaseName}OperationContext::GetStreamingResponseModelName() const noexcept
<@placeIndents quantity=indents/>{
<#if context.getOutputEventStreamInfo(operation).isPresent()>
<@placeIndents quantity=indents/>    return Aws::Crt::String("${context.getStreamingResponseAppType(operation)}");
<#else>
<@placeIndents quantity=indents/>    return Aws::Crt::Optional<Aws::Crt::String>();
</#if>
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>Aws::Crt::String ${OperationPascalCaseName}OperationContext::GetOperationName() const noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    return Aws::Crt::String("${operation.getId()}");
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>std::future<${OperationPascalCaseName}Result> ${OperationPascalCaseName}Operation::GetResult() noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    return std::async(m_asyncLaunchMode, [this](){
<@placeIndents quantity=indents/>        return ${OperationPascalCaseName}Result(GetOperationResult().get());
<@placeIndents quantity=indents/>    });
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>${OperationPascalCaseName}Operation::${OperationPascalCaseName}Operation(
<@placeIndents quantity=indents/>    ClientConnection &connection,
    <#if context.getOutputEventStreamInfo(operation).isPresent()>
<@placeIndents quantity=indents/>    std::shared_ptr<${OperationPascalCaseName}StreamHandler> streamHandler,
    </#if>
<@placeIndents quantity=indents/>    const ${OperationPascalCaseName}OperationContext &operationContext,
<@placeIndents quantity=indents/>    Aws::Crt::Allocator *allocator) noexcept
    <#if context.getOutputEventStreamInfo(operation).isPresent()>
<@placeIndents quantity=indents/>    : ClientOperation(connection, streamHandler, operationContext, allocator)
    <#else>
<@placeIndents quantity=indents/>    : ClientOperation(connection, nullptr, operationContext, allocator)
    </#if>
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>std::future<RpcError> ${OperationPascalCaseName}Operation::Activate(
<@placeIndents quantity=indents/>    const ${context.getRequestClassName(operation)} &request,
<@placeIndents quantity=indents/>    OnMessageFlushCallback onMessageFlushCallback) noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    return ClientOperation::Activate(static_cast<const AbstractShapeBase *>(&request), onMessageFlushCallback);
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>Aws::Crt::String ${OperationPascalCaseName}Operation::GetModelName() const noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    return m_operationModelContext.GetOperationName();
<@placeIndents quantity=indents/>}

</#macro>
