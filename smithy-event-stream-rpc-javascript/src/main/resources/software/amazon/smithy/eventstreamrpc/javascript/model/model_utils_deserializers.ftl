<#list allShapes as shape>
<#if shape.getDataShape().get().getType().name() == "STRUCTURE" || shape.getDataShape().get().getType().name() == "UNION">
<#if shape.getDataShape().get().getType().name() == "STRUCTURE">
<#assign downcastShape = fn_to_structure_shape.apply(shape.getDataShape().get())>
<#assign downcastShapeMembers = fn_structure_members.apply(downcastShape)>
<#elseif shape.getDataShape().get().getType().name() == "UNION">
<#assign downcastShape = fn_to_union_shape.apply(shape.getDataShape().get())>
<#assign downcastShapeMembers = fn_union_members.apply(downcastShape)>
</#if>
<#assign className = context.getTypeName(shape.getDataShape().get())>
export function deserialize${className}(value : model.${className}) : model.${className} {
<#list downcastShapeMembers as shapeMember>
<#assign needsDeserialization = context.shouldDeserializeMember(shapeMember)>
<#if needsDeserialization>
    ${context.getDeserializeLine(shapeMember)}
</#if>
</#list>
    return value;
}

</#if>
</#list>
<#list topLevelInboundShapes as shape>
<#assign className = context.getTypeName(shape.getDataShape().get())>
export function deserializeEventstreamMessageTo${className}(message: eventstream.Message) : model.${className} {
    const payload_text : string = toUtf8(new Uint8Array(message.payload as ArrayBuffer));
    let response : model.${className} = JSON.parse(payload_text) as model.${className};

    return deserialize${className}(response);
}

</#list>
