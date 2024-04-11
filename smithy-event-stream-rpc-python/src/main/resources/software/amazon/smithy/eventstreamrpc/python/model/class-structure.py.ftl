<#assign structure = fn_to_structure_shape.apply(shape.getDataShape().get())>
<#assign memberShapes = fn_structure_members.apply(structure)>
<#if structure.hasTrait("error")>
    <#assign baseClassName = baseErrorShapeName>
<#else>
    <#assign baseClassName = "rpc.Shape">
</#if>
<#-- class declaration -->
class ${context.getTypeName(structure)}(${baseClassName}):
    """
    ${context.getTypeName(structure)}
<#if structure.hasTrait("documentation")>

    ${structure.findTrait("documentation").get().getValue()}
</#if>
<#if memberShapes?has_content>

    All attributes are None by default, and may be set by keyword in the constructor.

    Keyword Args:
    <#list memberShapes as memberShape>
        <#assign memberName = fn_camel_to_python.apply(memberShape.getMemberName())>
        <#assign memberTargetShape = context.getShape(memberShape.getTarget())>
        <#if memberTargetShape.hasTrait("enum")>
            <#assign description = memberTargetShape.getId().name + " enum value. ">
        <#else>
            <#assign description = "">
        </#if>
        <#if memberShape.hasTrait("documentation")>
            <#assign description = description + memberShape.findTrait("documentation").get().getValue()>
        </#if>
        <#if memberShape.hasTrait("deprecated")>
            <#if memberShape.findTrait("deprecated").get().getMessage().isPresent()>
                <#assign description = " " + memberShape.findTrait("deprecated").get().getMessage().get() + description>
            </#if>
            <#if memberShape.findTrait("deprecated").get().getSince().isPresent()>
                <#assign description = "in " + memberShape.findTrait("deprecated").get().getSince().get() + description>
            </#if>
            <#assign description = "Deprecated " + description>
        </#if>
        ${memberName}: ${description}
    </#list>

    Attributes:
    <#list memberShapes as memberShape>
        <#assign memberName = fn_camel_to_python.apply(memberShape.getMemberName())>
        <#assign memberTargetShape = context.getShape(memberShape.getTarget())>
        <#if memberTargetShape.hasTrait("enum")>
            <#assign description = memberTargetShape.getId().name + " enum value. ">
        <#else>
            <#assign description = "">
        </#if>
        <#if memberShape.hasTrait("documentation")>
            <#assign description = description + memberShape.findTrait("documentation").get().getValue()>
        </#if>
        <#if memberShape.hasTrait("deprecated")>
            <#if memberShape.findTrait("deprecated").get().getMessage().isPresent()>
                <#assign description = " " + memberShape.findTrait("deprecated").get().getMessage().get() + description>
            </#if>
            <#if memberShape.findTrait("deprecated").get().getSince().isPresent()>
                <#assign description = "in " + memberShape.findTrait("deprecated").get().getSince().get() + description>
            </#if>
            <#assign description = "Deprecated " + description>
        </#if>
        ${memberName}: ${description}
    </#list>
</#if>
    """

<#-- init -->
<#if memberShapes?has_content>
    def __init__(self, *,
<#list memberShapes as memberShape>                 ${fn_camel_to_python.apply(memberShape.getMemberName())}: typing.Optional[${context.getTypeName(memberShape)}] = None<#sep>,${"\n"}</#sep></#list>):
<#else>
    def __init__(self):
</#if>
        super().__init__()
<#list memberShapes as memberShape>
    <#assign memberName = fn_camel_to_python.apply(memberShape.getMemberName())>
    <#if context.getTypeName(memberShape, "", false) == "bytes">
        if ${memberName} is not None and isinstance(${memberName}, str):
            ${memberName} = ${memberName}.encode('utf-8')
    </#if>
        self.${memberName} = ${memberName}  # type: typing.Optional[${context.getTypeName(memberShape, "", false)}]
</#list>

<#list memberShapes as memberShape>
    <#assign memberName = fn_camel_to_python.apply(memberShape.getMemberName())>
    def set_${memberName}(self, ${memberName}: ${context.getTypeName(memberShape)}):
    <#if context.getTypeName(memberShape, "", false) == "bytes">
        if ${memberName} is not None and isinstance(${memberName}, str):
            ${memberName} = ${memberName}.encode('utf-8')
    </#if>
        self.${memberName} = ${memberName}
        return self

</#list>

<#-- if error -->
<#if structure.hasTrait("error")>
    def _get_error_type_string(self):
        return '${structure.findTrait("error").get().getValue()}'

</#if>
<#-- to payload -->
    def _to_payload(self):
        payload = {}
<#list memberShapes as memberShape>
    <#assign memberName = fn_camel_to_python.apply(memberShape.getMemberName())>
        if self.${memberName} is not None:
            payload['${memberShape.getMemberName()}'] = ${context.getShapeToPayloadCode(memberShape, "self." + memberName)}
</#list>
        return payload

<#-- from payload -->
    @classmethod
    def _from_payload(cls, payload):
        new = cls()
<#list memberShapes as memberShape>
    <#assign memberName = fn_camel_to_python.apply(memberShape.getMemberName())>
    <#assign payloadName = memberShape.getMemberName()>
        if '${payloadName}' in payload:
            new.${memberName} = ${context.getPayloadToShapeCode(memberShape, "payload['" + payloadName + "']")}
</#list>
        return new

<#-- model name -->
    @classmethod
    def _model_name(cls):
        return '${structure.getId()}'

<#-- repr -->
    def __repr__(self):
        attrs = []
        for attr, val in self.__dict__.items():
            if val is not None:
                attrs.append('%s=%r' % (attr, val))
        return '%s(%s)' % (self.__class__.__name__, ', '.join(attrs))

<#-- eq -->
    def __eq__(self, other):
        if isinstance(other, self.__class__):
            return self.__dict__ == other.__dict__
        return False
