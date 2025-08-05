<#assign export=context.getExportName()/>
<#include "DirectiveUtils.ftl">
<#macro newOperation indents operation context>
    <#local OperationPascalCaseName = operation.getId().getName()/>
<#if context.getOutputEventStreamInfo(operation).isPresent()>
class ${export} ${OperationPascalCaseName}StreamHandler : public StreamResponseHandler
{
  public:
    virtual void OnStreamEvent(${context.getStreamingResponseClassName(operation)} *response) { (void)response; }

    /**
     * A callback that is invoked when an error occurs while parsing a message from the stream.
     * @param rpcError The RPC error containing the status and possibly a CRT error.
     */
    virtual bool OnStreamError(RpcError rpcError)
    {
      (void)rpcError;
      return true;
    }

    <#list operation.getErrors() as shapeId>
    /**
     * A callback that is invoked upon receiving an error of type `${shapeId.getName()}`.
     * @param operationError The error message being received.
     */
    virtual bool OnStreamError(${shapeId.getName()} *operationError)
    {
      (void)operationError;
      return true;
    }

    </#list>

    /**
     * A callback that is invoked upon receiving ANY error response from the server.
     * @param operationError The error message being received.
     */
    virtual bool OnStreamError(OperationError *operationError)
    {
      (void)operationError;
      return true;
    }

  private:
    /**
     * Invoked when a message is received on this continuation.
     */
    void OnStreamEvent(Aws::Crt::ScopedResource<AbstractShapeBase> response) override;

    /**
     * Invoked when a message is received on this continuation but results in an error.
     *
     * This callback can return true so that the stream is closed afterwards.
     */
    bool OnStreamError(Aws::Crt::ScopedResource<OperationError> error, RpcError rpcError) override;
};

</#if>

<@placeIndents quantity=indents/>class ${export} ${OperationPascalCaseName}Result
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>  public:
<@placeIndents quantity=indents/>    ${OperationPascalCaseName}Result() noexcept {};
<@placeIndents quantity=indents/>    ${OperationPascalCaseName}Result(EventstreamResultVariantType&& result) noexcept : m_result(std::move(result)) {}
<@placeIndents quantity=indents/>    ${context.getResponseClassName(operation)} *GetOperationResponse() const noexcept
<@placeIndents quantity=indents/>    {
<@placeIndents quantity=indents/>        return static_cast<${context.getResponseClassName(operation)} *>(std::get<Aws::Crt::ScopedResource<AbstractShapeBase>>(m_result).get());
<@placeIndents quantity=indents/>    }

<@placeIndents quantity=indents/>    /**
<@placeIndents quantity=indents/>      * @return true if the response is associated with an expected response;
<@placeIndents quantity=indents/>      * false if the response is associated with an error.
<@placeIndents quantity=indents/>      */
<@placeIndents quantity=indents/>    operator bool() const noexcept { return GetResultType() == OPERATION_RESPONSE; }
<@placeIndents quantity=indents/>    OperationError *GetOperationError() const noexcept { return std::get<Aws::Crt::ScopedResource<OperationError>>(m_result).get(); }
<@placeIndents quantity=indents/>    RpcError GetRpcError() const noexcept { return std::get<RpcError>(m_result); }
<@placeIndents quantity=indents/>    ResultType GetResultType() const noexcept { return ResultVariantToResultType(m_result); }
<@placeIndents quantity=indents/>  private:
<@placeIndents quantity=indents/>    EventstreamResultVariantType m_result;
<@placeIndents quantity=indents/>};

<@placeIndents quantity=indents/>class ${export} ${OperationPascalCaseName}Operation : public ClientOperation
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    public:
<@placeIndents quantity=indents/>        ${OperationPascalCaseName}Operation(
<@placeIndents quantity=indents/>            ClientConnection &connection,
<#if context.getOutputEventStreamInfo(operation).isPresent()>
<@placeIndents quantity=indents/>            std::shared_ptr<${OperationPascalCaseName}StreamHandler> streamHandler,
</#if>
<@placeIndents quantity=indents/>            const std::shared_ptr<OperationModelContext> &operationContext,
<@placeIndents quantity=indents/>            Aws::Crt::Allocator *allocator = Aws::Crt::g_allocator) noexcept;

<@placeIndents quantity=indents/>        /**
<@placeIndents quantity=indents/>         * Used to activate a stream for the `${OperationPascalCaseName}Operation`
<@placeIndents quantity=indents/>         * @param request The request used for the `${OperationPascalCaseName}Operation`
<@placeIndents quantity=indents/>         * @param onMessageFlushCallback An optional callback that is invoked when the request is flushed.
<@placeIndents quantity=indents/>         * @return An `RpcError` that can be used to check whether the stream was activated.
<@placeIndents quantity=indents/>         */
<@placeIndents quantity=indents/>        std::future<RpcError> Activate(
<@placeIndents quantity=indents/>            const ${context.getRequestClassName(operation)} &request,
<@placeIndents quantity=indents/>            OnMessageFlushCallback onMessageFlushCallback = nullptr) noexcept;

<#if context.getInputEventStreamInfo(operation).isPresent()>
<#assign requestStreamType = context.getInputEventStreamInfo(operation).get().getEventStreamTarget().getId().getName()>
<@placeIndents quantity=indents/>         /**
<@placeIndents quantity=indents/>           * Send a ${requestStreamType} stream event.
<@placeIndents quantity=indents/>           *
<@placeIndents quantity=indents/>           * Activate() must have completed before calling SendMessage().
<@placeIndents quantity=indents/>           *
<@placeIndents quantity=indents/>           * Returns a Future which completes with a value indicating message
<@placeIndents quantity=indents/>           * flush success or what went wrong.
<@placeIndents quantity=indents/>           **/
<@placeIndents quantity=indents/>         std::future<RpcError> SendStreamMessage(const ${requestStreamType} &event, OnMessageFlushCallback onMessageFlushCallback = nullptr);

</#if>
<@placeIndents quantity=indents/>        /**
<@placeIndents quantity=indents/>         * Retrieve the result from activating the stream.
<@placeIndents quantity=indents/>         */
<@placeIndents quantity=indents/>        std::future<${OperationPascalCaseName}Result> GetResult() noexcept;

<@placeIndents quantity=indents/>    private:
<@placeIndents quantity=indents/>        std::shared_ptr<std::promise<${OperationPascalCaseName}Result>> m_resultPromise;

<@placeIndents quantity=indents/>};

</#macro>
