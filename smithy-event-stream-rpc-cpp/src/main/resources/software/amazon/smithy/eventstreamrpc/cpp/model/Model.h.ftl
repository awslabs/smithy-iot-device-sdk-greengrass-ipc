<#include "macros/Namespace.ftl">
<#include "macros/Enum.h.ftl">
<#include "macros/StructUnion.h.ftl">
<#include "macros/Operation.h.ftl">
<#assign serviceName=context.getServiceShapeName()/>
<#assign namespace=context.getNamespaceAsCppPath()/>
<#assign export=context.getExportName()/>
#pragma once
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is generated. */

#include <aws/eventstreamrpc/EventStreamClient.h>

#include <aws/crt/DateTime.h>
#include <${namespace}/Exports.h>

#include <memory>

using namespace Aws::Eventstreamrpc;

<@startNamespace namespaceList=context.getNamespaceAsList() spacesPerTab=4/>
            class ${serviceName}Client;
            class ${serviceName}ServiceModel;
<#list allShapes as shape>
    <#if fn_is_enum.apply(shape)>
        <@newEnum shape=shape indents=3/>
    <#elseif shape.getDataShape().get().getType().name() == "STRUCTURE" || shape.getDataShape().get().getType().name() == "UNION">
        <@newStructUnion shape=shape context=context indents=3/>
    </#if>
</#list>
<#list operations as operation>
    <@newOperation operation=operation context=context indents=3/>
</#list>
            class ${export} ${serviceName}ServiceModel : public ServiceModel
            {
              public:
                ${serviceName}ServiceModel() noexcept;
                Aws::Crt::ScopedResource<OperationError> AllocateOperationErrorFromPayload(
                    const Aws::Crt::String &errorModelName,
                    Aws::Crt::StringView stringView,
                    Aws::Crt::Allocator *allocator = Aws::Crt::g_allocator) const noexcept override;
                void AssignModelNameToErrorResponse(Aws::Crt::String, ErrorResponseFactory) noexcept;
              private:
                friend class ${serviceName}Client;
              <#list operations as operation>
                ${operation.getId().getName()}OperationContext m_${operation.getId().getName()?uncap_first}OperationContext;
              </#list>
                Aws::Crt::Map<Aws::Crt::String, ErrorResponseFactory> m_modelNameToErrorResponse;
            };
<@endNamespace namespaceList=context.getNamespaceAsList() spacesPerTab=4/>
