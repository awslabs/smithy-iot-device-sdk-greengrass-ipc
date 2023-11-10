<#list allShapes as shape>
<#if shape.getDataShape().get().getType().name() == "STRUCTURE">
<#assign downcastShape = fn_to_structure_shape.apply(shape.getDataShape().get())>
<#assign downcastShapeMembers = fn_structure_members.apply(downcastShape)>
<#assign shapeName = context.getTypeName(downcastShape)>
export function validate${context.getTypeName(downcastShape)}(value : model.${context.getTypeName(downcastShape)}) : void {
<#list downcastShapeMembers as shapeMember>
    ${context.getValidateMemberLine(shapeMember, shapeName)}
</#list>
}

<#elseif shape.getDataShape().get().getType().name() == "UNION">
<#assign downcastShape = fn_to_union_shape.apply(shape.getDataShape().get())>
<#assign downcastShapeMembers = fn_union_members.apply(downcastShape)>
const _${context.getTypeName(downcastShape)}PropertyValidators : Map<string, eventstream_rpc_utils.ElementValidator> = new Map<string, eventstream_rpc_utils.ElementValidator>([
<#list downcastShapeMembers as shapeMember>
<#assign memberName = shapeMember.getMemberName()>
    ["${memberName}", ${context.getValidationFunctionObject(shapeMember)}]<#sep>,</#sep>
</#list>
]);

export function validate${context.getTypeName(downcastShape)}(value : model.${context.getTypeName(downcastShape)}) : void {
    eventstream_rpc_utils.validateValueAsUnion(value, _${context.getTypeName(downcastShape)}PropertyValidators);
}

</#if>
</#list>
