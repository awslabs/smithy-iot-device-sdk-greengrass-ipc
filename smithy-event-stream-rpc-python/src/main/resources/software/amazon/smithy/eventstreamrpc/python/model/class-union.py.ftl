<#-- TODO delete this file, and have class-structure.py.ftl handle both unions and structures. The files are currently 90% identical -->
<#assign union = fn_to_union_shape.apply(shape.getDataShape().get())>
<#assign memberShapes = fn_union_members.apply(union)>
class ${context.getTypeName(union)}(rpc.Shape):
    """
    ${context.getTypeName(union)} is a "tagged union" class.
<#if union.hasTrait("documentation")>

    ${union.findTrait("documentation").get().getValue()}
</#if>
<#if memberShapes?has_content>

    When sending, only one of the attributes may be set.
    When receiving, only one of the attributes will be set.
    All other attributes will be None.

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
        return '${union.getId()}'

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
