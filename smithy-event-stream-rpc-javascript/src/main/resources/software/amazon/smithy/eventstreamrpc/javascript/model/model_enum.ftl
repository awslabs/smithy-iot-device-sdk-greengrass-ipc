<#assign dataShape = shape.getDataShape().get()>
<#assign hasEnumDocumentation = context.hasDocumentation(dataShape)>
/**
<#if hasEnumDocumentation>
<#assign enumDocumentation = context.getDocumentation(dataShape)>
 * ${enumDocumentation}
 *
</#if>
 * To preserve backwards compatibility, no validation is performed on enum-valued fields.
 */
export enum ${shape.getClassName()} {

<#list fn_get_enum_defs.apply(shape) as enum_def>
<#assign entryHasDocumentation = enum_def.getDocumentation().isPresent()>
<#if entryHasDocumentation>
<#assign entryDocumentation = enum_def.getDocumentation().get()>
    /**
     * ${entryDocumentation}
     */
</#if>
    ${enum_def.getName().get()} = "${enum_def.getValue()}"<#sep>,</#sep>

</#list>
}
