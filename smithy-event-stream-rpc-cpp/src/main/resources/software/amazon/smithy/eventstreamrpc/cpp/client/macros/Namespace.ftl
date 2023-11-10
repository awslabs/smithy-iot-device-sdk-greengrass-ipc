<#macro startNamespace namespaceList spacesPerTab>
    <#local tab = ""?left_pad(4*spacesPerTab)/>
    <#list namespaceList as namespace>
        <#list 0..<namespace?index as i>${tab}</#list>namespace ${namespace} {
    </#list>
</#macro>
<#macro endNamespace namespaceList spacesPerTab>
    <#local tab = ""?left_pad(4*spacesPerTab)/>
    <#list namespaceList as namespace>
        <#list 0..<namespace?index as i>${tab}</#list>}
    </#list>
</#macro>
