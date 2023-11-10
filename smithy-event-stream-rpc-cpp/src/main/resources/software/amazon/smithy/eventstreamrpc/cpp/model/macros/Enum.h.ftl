<#include "DirectiveUtils.ftl">
<#macro newEnum indents shape>
    <#local padding = ""?left_pad(4*indents)/>
<@placeIndents quantity=indents/>enum ${shape.getClassName()}
<@placeIndents quantity=indents/>{
<#list fn_get_enum_defs.apply(shape) as enum_def>
<@placeIndents quantity=indents/>    ${fn_to_constant_case.apply(shape.getClassName())}_${enum_def.getName().get()}<#if enum_def_has_next>,</#if>
</#list>
<@placeIndents quantity=indents/>};

</#macro>
