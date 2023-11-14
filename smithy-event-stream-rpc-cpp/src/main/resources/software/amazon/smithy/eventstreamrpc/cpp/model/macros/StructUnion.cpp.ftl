<#include "DirectiveUtils.ftl">
<#macro newStructUnion indents shape context>
<#if shape.getDataShape().get().getType().name() == "STRUCTURE">
    <#local convertedShape = fn_to_structure_shape.apply(shape.getDataShape().get())>
    <#local memberShapes = fn_structure_members.apply(convertedShape)>
<#elseif shape.getDataShape().get().getType().name() == "UNION">
    <#local convertedShape = fn_to_union_shape.apply(shape.getDataShape().get())>
    <#local memberShapes = fn_union_members.apply(convertedShape)>
</#if>
<#local PascalCaseStructUnionName = context.getTypeName(convertedShape)/>
<#local outputObjectVarName = "payloadObject">
<@placeIndents quantity=indents/>void ${PascalCaseStructUnionName}::SerializeToJsonObject(Aws::Crt::JsonObject &${outputObjectVarName}) const noexcept
<@placeIndents quantity=indents/>{
<#if memberShapes?has_content>
<#list memberShapes as memberShape>
<#assign camelCaseMemberName = memberShape.getMemberName()>
<#assign memberVar = "m_" + camelCaseMemberName>
<#assign serializationCode = context.generateSerializerForVarWithKey(memberShape, camelCaseMemberName, memberVar+".value()", outputObjectVarName)>
<#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>    <#if !memberShape?is_first>else </#if>if (m_chosenMember == TAG_${fn_to_constant_case.apply(camelCaseMemberName)} && ${memberVar}.has_value()) {
<#else>
<@placeIndents quantity=indents/>    if (${memberVar}.has_value()) {
</#if>
<#--Do not manually indent here-->${fn_tab_each_line.apply(serializationCode, indents+2)}
<@placeIndents quantity=indents/>    }
</#list>
<#else>
<@placeIndents quantity=indents/>    (void)${outputObjectVarName};
</#if>
<@placeIndents quantity=indents/>}

<#assign jsonViewVar = "jsonView">
<#assign camelCaseStructure = fn_pascal_to_camel.apply(PascalCaseStructUnionName)>
<@placeIndents quantity=indents/>void ${PascalCaseStructUnionName}::s_loadFromJsonView(${PascalCaseStructUnionName} &${camelCaseStructure}, const Aws::Crt::JsonView &${jsonViewVar}) noexcept
<@placeIndents quantity=indents/>{
<#if memberShapes?has_content>
<#list memberShapes as memberShape>
<#assign camelCaseMemberName = memberShape.getMemberName()>
<#assign memberVar = "m_" + camelCaseMemberName>
<#assign deserializationCode = context.generateDeserializerForMemberWithKey(memberShape, camelCaseStructure+"."+memberVar, jsonViewVar, camelCaseMemberName)>
<#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>    <#if !memberShape?is_first>else </#if>if (${jsonViewVar}.ValueExists("${camelCaseMemberName}")) {
<#else>
<@placeIndents quantity=indents/>    if (${jsonViewVar}.ValueExists("${camelCaseMemberName}")) {
</#if>
<#--Do not manually indent here-->${fn_tab_each_line.apply(deserializationCode, indents+2)}
<#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>        ${camelCaseStructure}.m_chosenMember = TAG_${fn_to_constant_case.apply(camelCaseMemberName)};
</#if>
<@placeIndents quantity=indents/>    }
</#list>
<#else>
<@placeIndents quantity=indents/>    (void)${camelCaseStructure};
<@placeIndents quantity=indents/>    (void)${jsonViewVar};
</#if>
<@placeIndents quantity=indents/>}

<#if shape.getDataShape().get().getType().name() == "UNION">
<#--Copy constructor must have custom definition for unions-->
<@placeIndents quantity=indents/>${PascalCaseStructUnionName}& ${PascalCaseStructUnionName}::operator=(const ${PascalCaseStructUnionName}& objectToCopy) noexcept
<@placeIndents quantity=indents/>{
    <#list memberShapes as memberShape>
        <#assign camelCaseMemberName = memberShape.getMemberName()>
        <#assign memberVar = "m_" + camelCaseMemberName>
<@placeIndents quantity=indents/>    <#if !memberShape?is_first>else </#if>if (objectToCopy.m_chosenMember == TAG_${fn_to_constant_case.apply(camelCaseMemberName)})
<@placeIndents quantity=indents/>    {
<@placeIndents quantity=indents/>        ${memberVar} = objectToCopy.${memberVar};
<@placeIndents quantity=indents/>        m_chosenMember = objectToCopy.m_chosenMember;
<@placeIndents quantity=indents/>    }
    </#list>
<@placeIndents quantity=indents/>    return *this;
<@placeIndents quantity=indents/>}
</#if>

<#list memberShapes as memberShape>
    <#local memberTargetShape = context.getShape(memberShape.getTarget())>
    <#if memberTargetShape.hasTrait("enum")>
    <#local PascalCaseMemberName = fn_camel_to_pascal.apply(memberShape.getMemberName())>
    <#local camelCaseMemberName = memberShape.getMemberName()>
<@placeIndents quantity=indents/>void ${PascalCaseStructUnionName}::Set${PascalCaseMemberName}(${memberTargetShape.getId().name} ${camelCaseMemberName}) noexcept
<@placeIndents quantity=indents/>{
<#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>    m_${camelCaseMemberName} = Aws::Crt::Optional<${context.getTypeName(memberShape)}>();
</#if>
<@placeIndents quantity=indents/>    switch (${camelCaseMemberName}) {
<#list fn_get_enum_defs_from_shape.apply(memberTargetShape) as enum_def>
<@placeIndents quantity=indents/>        case ${fn_to_constant_case.apply(memberTargetShape.getId().name)}_${enum_def.getName().get()}:
<#-- If Enums refer to more complicated types, this may need an update. -->
<#if context.getTypeName(memberShape) == "Aws::Crt::String">
<@placeIndents quantity=indents/>            m_${camelCaseMemberName} = ${context.getTypeName(memberTargetShape)}("${enum_def.getValue()}");
<#else>
<@placeIndents quantity=indents/>            m_${camelCaseMemberName} = ${context.getTypeName(memberTargetShape)}(${enum_def.getValue()});
</#if>
<#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>            m_chosenMember = TAG_${fn_to_constant_case.apply(camelCaseMemberName)};
</#if>
<@placeIndents quantity=indents/>            break;
</#list>
<@placeIndents quantity=indents/>        default:
<@placeIndents quantity=indents/>            break;
<@placeIndents quantity=indents/>    }
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>Aws::Crt::Optional<${memberTargetShape.getId().name}> ${PascalCaseStructUnionName}::Get${PascalCaseMemberName}() noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    if (!m_${camelCaseMemberName}.has_value()) return Aws::Crt::Optional<${memberTargetShape.getId().name}>();
<#list fn_get_enum_defs_from_shape.apply(memberTargetShape) as enum_def>
    <#if context.getTypeName(memberShape) == "Aws::Crt::String">
<#local valueToCheck = context.getTypeName(memberTargetShape)+"(\""+enum_def.getValue()+"\")">
    <#else>
<#local valueToCheck = context.getTypeName(memberTargetShape)+"("+enum_def.getValue()+")">
    </#if>
    <#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>    if (m_chosenMember == TAG_${fn_to_constant_case.apply(camelCaseMemberName)} &&
<@placeIndents quantity=indents/>        m_${camelCaseMemberName}.value() == ${valueToCheck}) {
    <#else>
<@placeIndents quantity=indents/>    if (m_${camelCaseMemberName}.value() == ${valueToCheck}) {
    </#if>
<@placeIndents quantity=indents/>        return Aws::Crt::Optional<${memberTargetShape.getId().name}>(${fn_to_constant_case.apply(memberTargetShape.getId().name)}_${enum_def.getName().get()});
<@placeIndents quantity=indents/>    }
</#list>

<@placeIndents quantity=indents/>    return Aws::Crt::Optional<${memberTargetShape.getId().name}>();
<@placeIndents quantity=indents/>}
    </#if>
</#list>

<@placeIndents quantity=indents/>const char* ${PascalCaseStructUnionName}::MODEL_NAME = "${convertedShape.getId()}";

<@placeIndents quantity=indents/>Aws::Crt::String ${PascalCaseStructUnionName}::GetModelName() const noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    return ${PascalCaseStructUnionName}::MODEL_NAME;
<@placeIndents quantity=indents/>}

<#if convertedShape.hasTrait("error")>
    <#local BaseClass = "OperationError"/>
<#else>
    <#local BaseClass = "AbstractShapeBase"/>
</#if>
<@placeIndents quantity=indents/>Aws::Crt::ScopedResource<${BaseClass}> ${PascalCaseStructUnionName}::s_allocateFromPayload(
<@placeIndents quantity=indents/>    Aws::Crt::StringView stringView,
<@placeIndents quantity=indents/>    Aws::Crt::Allocator *allocator) noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    Aws::Crt::String payload = {stringView.begin(), stringView.end()};
<@placeIndents quantity=indents/>    Aws::Crt::JsonObject jsonObject(payload);
<@placeIndents quantity=indents/>    Aws::Crt::JsonView jsonView(jsonObject);

<@placeIndents quantity=indents/>    Aws::Crt::ScopedResource<${PascalCaseStructUnionName}> shape(
<@placeIndents quantity=indents/>        Aws::Crt::New<${PascalCaseStructUnionName}>(allocator), ${PascalCaseStructUnionName}::s_customDeleter);
<@placeIndents quantity=indents/>    shape->m_allocator = allocator;
<@placeIndents quantity=indents/>    ${PascalCaseStructUnionName}::s_loadFromJsonView(*shape, jsonView);
<@placeIndents quantity=indents/>    auto operationResponse = static_cast<${BaseClass} *>(shape.release());
<@placeIndents quantity=indents/>    return Aws::Crt::ScopedResource<${BaseClass}>(operationResponse, ${BaseClass}::s_customDeleter);
<@placeIndents quantity=indents/>}

<@placeIndents quantity=indents/>void ${PascalCaseStructUnionName}::s_customDeleter(${PascalCaseStructUnionName} *shape) noexcept
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    ${BaseClass}::s_customDeleter(static_cast<${BaseClass} *>(shape));
<@placeIndents quantity=indents/>}

</#macro>
