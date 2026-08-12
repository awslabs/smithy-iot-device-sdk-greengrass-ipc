<#list allShapes as shape>
<#if shape.getDataShape().get().getType().name() == "STRUCTURE" || shape.getDataShape().get().getType().name() == "UNION">
<#if shape.getDataShape().get().getType().name() == "STRUCTURE">
<#assign downcastShape = fn_to_structure_shape.apply(shape.getDataShape().get())>
<#assign downcastShapeMembers = fn_structure_members.apply(downcastShape)>
<#elseif shape.getDataShape().get().getType().name() == "UNION">
<#assign downcastShape = fn_to_union_shape.apply(shape.getDataShape().get())>
<#assign downcastShapeMembers = fn_union_members.apply(downcastShape)>
</#if>
export function normalize${context.getTypeName(downcastShape)}(value : model.${context.getTypeName(downcastShape)}) : any {
    let normalizedValue : any = {};
<#list downcastShapeMembers as shapeMember>
    ${context.getNormalizerMemberLine(shapeMember)}
</#list>

    return normalizedValue;
}

</#if>
</#list>
