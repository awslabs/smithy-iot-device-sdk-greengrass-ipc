<#assign dataShape = shape.getDataShape().get()>
<#assign hasShapeDocumentation = context.hasDocumentation(dataShape)>
<#if shape.getDataShape().get().getType().name() == "STRUCTURE">
<#assign downcastShape = fn_to_structure_shape.apply(shape.getDataShape().get())>
<#assign downcastShapeMembers = fn_structure_members.apply(downcastShape)>
<#if hasShapeDocumentation>
<#assign shapeDocumentation = context.getDocumentation(dataShape)>
/**
 * ${shapeDocumentation}
 */
</#if>
<#elseif shape.getDataShape().get().getType().name() == "UNION">
<#assign downcastShape = fn_to_union_shape.apply(shape.getDataShape().get())>
<#assign downcastShapeMembers = fn_union_members.apply(downcastShape)>
<#if hasShapeDocumentation>
<#assign shapeDocumentation = context.getDocumentation(dataShape)>
/**
 * ${shapeDocumentation}
 *
 * ${context.getTypeName(downcastShape)} is a union type.  One and only one member must be set.
 */
</#if>
</#if>
export interface ${context.getTypeName(downcastShape)} {

<#list downcastShapeMembers as memberShape>
<#assign memberName = memberShape.getMemberName()>
<#assign isRequired = memberShape.hasTrait("required")>
<#assign optionalAnnotation=isRequired?then("", "?")>
<#assign memberHasDocumentation = context.hasDocumentation(memberShape)>
<#if memberHasDocumentation>
<#assign memberDocumentation = context.getDocumentation(memberShape)>
    /**
     * ${memberDocumentation}
     */
</#if>
    ${memberName}${optionalAnnotation}: ${context.getTypeName(memberShape)}<#sep>,</#sep>

</#list>
}
