<#assign export=context.getExportName()/>
<#include "DirectiveUtils.ftl">
<#macro newStructUnion indents shape context>
<#if shape.getDataShape().get().getType().name() == "STRUCTURE">
    <#local convertedShape = fn_to_structure_shape.apply(shape.getDataShape().get())/>
    <#local memberShapes = fn_structure_members.apply(convertedShape)/>
<#elseif shape.getDataShape().get().getType().name() == "UNION">
    <#local convertedShape = fn_to_union_shape.apply(shape.getDataShape().get())/>
    <#local memberShapes = fn_union_members.apply(convertedShape)/>
</#if>
<#local PascalCaseMemberName = context.getTypeName(convertedShape)/>
<#if convertedShape.hasTrait("error")>
    <#local BaseClass = "OperationError"/>
<#else>
    <#local BaseClass = "AbstractShapeBase"/>
</#if>
<@placeIndents quantity=indents/>class ${export} ${PascalCaseMemberName} : public ${BaseClass}
<@placeIndents quantity=indents/>{
<@placeIndents quantity=indents/>    public:
<@placeIndents quantity=indents/>        ${PascalCaseMemberName}() noexcept {}
<#--A custom destructor must be defined for a tagged union.-->
<#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>        ${PascalCaseMemberName}& operator=(const ${PascalCaseMemberName} &) noexcept;
<@placeIndents quantity=indents/>        ${PascalCaseMemberName}(const ${PascalCaseMemberName} &objectToCopy) { *this = objectToCopy; }
<#else>
<@placeIndents quantity=indents/>        ${PascalCaseMemberName}(const ${PascalCaseMemberName} &) = default;
</#if>
<#list memberShapes as memberShape>
    <#local PascalCase = fn_camel_to_pascal.apply(memberShape.getMemberName())>
    <#local camelCase = memberShape.getMemberName()>
    <#local memberTargetShape = context.getShape(memberShape.getTarget())>
    <#if memberShape.hasTrait("deprecated")>
        <#local deprecationMessage = "/* Deprecated">
        <#if memberShape.findTrait("deprecated").get().getSince().isPresent()>
            <#local deprecationMessage = deprecationMessage + " in " + memberShape.findTrait("deprecated").get().getSince().get()>
        </#if>
        <#if memberShape.findTrait("deprecated").get().getMessage().isPresent()>
            <#local deprecationMessage = deprecationMessage + " " + memberShape.findTrait("deprecated").get().getMessage().get()>
        </#if>
        <#local deprecationMessage = deprecationMessage + " */">
<@placeIndents quantity=indents/>        ${deprecationMessage}
    </#if>
    <#if memberTargetShape.hasTrait("documentation")>
        <#local docMessage = "/* " + memberTargetShape.findTrait("documentation").get().getValue() + " */">
<@placeIndents quantity=indents/>        ${docMessage}
    </#if>
    <#if memberTargetShape.hasTrait("enum")>
<@placeIndents quantity=indents/>        void Set${PascalCase}(${memberTargetShape.getId().name} ${camelCase}) noexcept;
        <#if memberShape.hasTrait("deprecated")>
<@placeIndents quantity=indents/>        ${deprecationMessage}
        </#if>
        <#if memberTargetShape.hasTrait("documentation")>
<@placeIndents quantity=indents/>        ${docMessage}
        </#if>
<@placeIndents quantity=indents/>        Aws::Crt::Optional<${memberTargetShape.getId().name}> Get${PascalCase}() noexcept;
    <#else>
<@placeIndents quantity=indents/>        void Set${PascalCase}(const ${context.getTypeName(memberShape)}& ${camelCase}) noexcept {
    <#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>            m_${camelCase} = ${camelCase};
<@placeIndents quantity=indents/>            m_chosenMember = TAG_${fn_to_constant_case.apply(camelCase)};
    <#else>
<@placeIndents quantity=indents/>            m_${camelCase} = ${camelCase};
    </#if>
<@placeIndents quantity=indents/>        }
        <#if memberShape.hasTrait("deprecated")>
<@placeIndents quantity=indents/>        ${deprecationMessage}
        </#if>
        <#if memberTargetShape.hasTrait("documentation")>
<@placeIndents quantity=indents/>        ${docMessage}
        </#if>
<#if convertedShape.hasTrait("error") && PascalCase == "Message">
<@placeIndents quantity=indents/>        Aws::Crt::Optional<${context.getTypeName(memberShape)}> Get${PascalCase}() noexcept override {
<#else>
<@placeIndents quantity=indents/>        Aws::Crt::Optional<${context.getTypeName(memberShape)}> Get${PascalCase}() noexcept {
</#if>
    <#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>            if (m_chosenMember == TAG_${fn_to_constant_case.apply(camelCase)}) {
<@placeIndents quantity=indents/>                return m_${camelCase};
<@placeIndents quantity=indents/>            } else {
<@placeIndents quantity=indents/>                return Aws::Crt::Optional<${context.getTypeName(memberShape)}>();
<@placeIndents quantity=indents/>            }
    <#else>
<@placeIndents quantity=indents/>            return m_${camelCase};
    </#if>
<@placeIndents quantity=indents/>        }
    </#if>
</#list>
<@placeIndents quantity=indents/>        void SerializeToJsonObject(Aws::Crt::JsonObject &payloadObject) const noexcept override;
<@placeIndents quantity=indents/>        static void s_loadFromJsonView(${PascalCaseMemberName}&, const Aws::Crt::JsonView&) noexcept;
<@placeIndents quantity=indents/>        static Aws::Crt::ScopedResource<${BaseClass}> s_allocateFromPayload(Aws::Crt::StringView, Aws::Crt::Allocator *) noexcept;
<@placeIndents quantity=indents/>        static void s_customDeleter(${PascalCaseMemberName} *) noexcept;
<@placeIndents quantity=indents/>        /* This needs to be defined so that `${PascalCaseMemberName}` can be used as a key in maps. */
<@placeIndents quantity=indents/>        bool operator<(const ${PascalCaseMemberName}&) const noexcept;
<@placeIndents quantity=indents/>        static const char* MODEL_NAME;

<@placeIndents quantity=indents/>    protected:
<@placeIndents quantity=indents/>        Aws::Crt::String GetModelName() const noexcept override;

<@placeIndents quantity=indents/>    private:
<#if shape.getDataShape().get().getType().name() == "UNION">
<@placeIndents quantity=indents/>        enum ChosenMember
<@placeIndents quantity=indents/>        {
<#list memberShapes as memberShape>
    <#local camelCase = memberShape.getMemberName()/>
<@placeIndents quantity=indents/>            TAG_${fn_to_constant_case.apply(camelCase)}<#if memberShape_has_next>,</#if>
</#list>
<@placeIndents quantity=indents/>        } m_chosenMember;
</#if>
<#list memberShapes as memberShape>
    <#local camelCase = memberShape.getMemberName()/>
<@placeIndents quantity=indents/>        Aws::Crt::Optional<${context.getTypeName(memberShape)}> m_${camelCase};
</#list>
<@placeIndents quantity=indents/>};

</#macro>
