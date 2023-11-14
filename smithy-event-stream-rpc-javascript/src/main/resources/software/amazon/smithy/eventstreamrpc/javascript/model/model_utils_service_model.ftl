function createNormalizerMap() : Map<string, eventstream_rpc.ShapeNormalizer> {
    return new Map<string, eventstream_rpc.ShapeNormalizer>([
<#list allShapes as shape>
<#if shape.getDataShape().get().getType().name() == "STRUCTURE" || shape.getDataShape().get().getType().name() == "UNION">
        ["${shape.getDataShape().get().getId()}", normalize${context.getTypeName(shape.getDataShape().get())}]<#sep>,</#sep>
</#if>
</#list>
    ]);
}

function createValidatorMap() : Map<string, eventstream_rpc.ShapeValidator> {
    return new Map<string, eventstream_rpc.ShapeValidator>([
<#list allShapes as shape>
    <#if shape.getDataShape().get().getType().name() == "STRUCTURE" || shape.getDataShape().get().getType().name() == "UNION">
        ["${shape.getDataShape().get().getId()}", validate${context.getTypeName(shape.getDataShape().get())}]<#sep>,</#sep>
    </#if>
</#list>
    ]);
}

function createDeserializerMap() : Map<string, eventstream_rpc.ShapeDeserializer> {
    return new Map<string, eventstream_rpc.ShapeDeserializer>([
<#list topLevelInboundShapes as shape>
        ["${shape.getDataShape().get().getId()}", deserializeEventstreamMessageTo${context.getTypeName(shape.getDataShape().get())}]<#sep>,</#sep>
</#list>
    ]);
}

function createSerializerMap() : Map<string, eventstream_rpc.ShapeSerializer> {
    return new Map<string, eventstream_rpc.ShapeSerializer>([
<#list topLevelOutboundShapes as shape>
        ["${shape.getDataShape().get().getId()}", serialize${context.getTypeName(shape.getDataShape().get())}ToEventstreamMessage]<#sep>,</#sep>
</#list>
    ]);
}

function createOperationMap() : Map<string, eventstream_rpc.EventstreamRpcServiceModelOperation> {
    return new Map<string, eventstream_rpc.EventstreamRpcServiceModelOperation>([
<#list operations as operation>
<#assign operationId = operation.getId().toString()>
<#assign requestId = operation.getInput().get().toString()>
<#assign responseId = operation.getOutput().get().toString()>
<#assign hasStreamingRequest = context.getInputEventStreamInfo(operation).isPresent()>
<#assign hasStreamingResponse = context.getOutputEventStreamInfo(operation).isPresent()>
<#assign errorList = operation.getErrors()>
        ["${operationId}", {
            requestShape: "${requestId}",
            responseShape: "${responseId}",
<#if hasStreamingRequest>
<#assign streamingRequestType=context.getInputEventStreamInfo(operation).get().getEventStreamTarget().getId()>
            outboundMessageShape: "${streamingRequestType}",
</#if>
<#if hasStreamingResponse>
<#assign streamingResponseType=context.getOutputEventStreamInfo(operation).get().getEventStreamTarget().getId()>
            inboundMessageShape: "${streamingResponseType}",
</#if>
            errorShapes: new Set<string>([
<#list errorList as error>
                "${error.toString()}"<#sep>,</#sep>
</#list>
            ])
        }]<#sep>,</#sep>
</#list>
    ]);
}

<#list allShapes as shape>
<#if fn_is_enum.apply(shape)>
const ${shape.getClassName()}Values : Set<string> = new Set<string>([
<#list fn_get_enum_defs.apply(shape) as enum_def>
    "${enum_def.getValue()}"<#sep>,</#sep>
</#list>
]);

</#if>
</#list>

function createEnumsMap() : Map<string, Set<string>> {
    return new Map<string, Set<string>>([
<#list allShapes as shape>
<#if fn_is_enum.apply(shape)>
        ["${shape.getClassName()}", ${shape.getClassName()}Values]<#sep>,</#sep>
</#if>
</#list>
    ]);
}

export function makeServiceModel() : eventstream_rpc.EventstreamRpcServiceModel {
    return {
        normalizers: createNormalizerMap(),
        validators: createValidatorMap(),
        deserializers: createDeserializerMap(),
        serializers: createSerializerMap(),
        operations: createOperationMap(),
        enums: createEnumsMap()
    };
}
