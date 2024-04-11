class _${operation.getId().getName()}Operation(rpc.ClientOperation):
<#if operation.hasTrait("documentation")>
    """
<#list operation.findTrait("documentation").get().getValue()?split("\n") as docLine>
    ${docLine}
</#list>
    """

</#if>
    @classmethod
    def _model_name(cls):
        return '${operation.getId()}'

    @classmethod
    def _request_type(cls):
        return ${operation.getInput().get().getName()}

    @classmethod
    def _request_stream_type(cls):
        <#if context.getInputEventStreamInfo(operation).isPresent()>
        return ${context.getInputEventStreamInfo(operation).get().getEventStreamTarget().getId().getName()}
        <#else>
        return None
        </#if>

    @classmethod
    def _response_type(cls):
        return ${operation.getOutput().get().getName()}

    @classmethod
    def _response_stream_type(cls):
        <#if context.getOutputEventStreamInfo(operation).isPresent()>
        return ${context.getOutputEventStreamInfo(operation).get().getEventStreamTarget().getId().getName()}
        <#else>
        return None
        </#if>
