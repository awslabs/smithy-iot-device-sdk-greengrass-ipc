<#list topLevelOutboundShapes as shape>
<#assign className = context.getTypeName(shape.getDataShape().get())>
export function serialize${className}ToEventstreamMessage(request : model.${className}) : eventstream.Message {
    return {
        type: eventstream.MessageType.ApplicationMessage,
        payload: JSON.stringify(normalize${className}(request))
    };
}

</#list>
