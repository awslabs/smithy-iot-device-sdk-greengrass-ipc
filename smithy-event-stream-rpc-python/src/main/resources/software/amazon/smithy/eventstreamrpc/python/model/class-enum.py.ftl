class ${shape.getClassName()}:
    """
    ${shape.getClassName()} enum
    """

<#list fn_get_enum_defs.apply(shape) as enum_def>
    ${enum_def.getName().get()} = '${enum_def.getValue()}'
</#list>
