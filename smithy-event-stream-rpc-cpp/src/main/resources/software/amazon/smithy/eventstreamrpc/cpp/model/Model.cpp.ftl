/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is generated. */

<#include "macros/Namespace.ftl">
<#include "macros/StructUnion.cpp.ftl">
<#include "macros/Operation.cpp.ftl">
<#assign serviceName=context.getServiceShapeName()/>
<#assign namespace=context.getNamespaceAsCppPath()/>
#include <aws/crt/Api.h>
#include <${namespace}/${serviceName}Model.h>

<@startNamespace namespaceList=context.getNamespaceAsList() spacesPerTab=4/>
<#list allShapes as shape>
    <#if shape.getDataShape().get().getType().name() == "STRUCTURE" || shape.getDataShape().get().getType().name() == "UNION">
        <@newStructUnion shape=shape context=context indents=3/>
    </#if>
</#list>
<#list operations as operation>
    <@newOperation operation=operation context=context indents=3/>
</#list>
            ${serviceName}ServiceModel::${serviceName}ServiceModel() noexcept :
            <#list operations as operation>
                m_${operation.getId().getName()?uncap_first}OperationContext(*this)<#if operation_has_next>,</#if>
            </#list>
            {
            }

            Aws::Crt::ScopedResource<OperationError> ${serviceName}ServiceModel::AllocateOperationErrorFromPayload(
                const Aws::Crt::String &errorModelName,
                Aws::Crt::StringView stringView,
                Aws::Crt::Allocator *allocator) const noexcept
            {
                auto it = m_modelNameToErrorResponse.find(errorModelName);
                if (it == m_modelNameToErrorResponse.end())
                {
                    return nullptr;
                }
                else
                {
                    return it->second(stringView, allocator);
                }
            }

            void ${serviceName}ServiceModel::AssignModelNameToErrorResponse(Aws::Crt::String modelName,
                                                                            ErrorResponseFactory factory) noexcept
            {
                m_modelNameToErrorResponse[modelName] = factory;
            }
<@endNamespace namespaceList=context.getNamespaceAsList() spacesPerTab=4/>
