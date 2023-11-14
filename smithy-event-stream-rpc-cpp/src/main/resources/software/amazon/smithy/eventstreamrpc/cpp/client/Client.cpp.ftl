/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is generated. */

<#include "macros/Namespace.ftl">
<#assign serviceName=context.getServiceShapeName()/>
<#assign namespace=context.getNamespaceAsCppPath()/>
#include <${namespace}/${serviceName}Client.h>
#include <aws/crt/io/Bootstrap.h>
#include <aws/crt/Types.h>

<@startNamespace namespaceList=context.getNamespaceAsList() spacesPerTab=4/>
            ${serviceName}Client::${serviceName}Client(
                Aws::Crt::Io::ClientBootstrap &clientBootstrap,
                Aws::Crt::Allocator *allocator) noexcept
                : m_connection(allocator), m_clientBootstrap(clientBootstrap), m_allocator(allocator), m_asyncLaunchMode(std::launch::deferred)
            {
<#list allShapes as shape>
<#if shape.getDataShape().get().hasTrait("error")>
<#assign errorId = shape.getDataShape().get().getId()/>
<#assign factoryFunction = errorId.getName() + "::s_allocateFromPayload"/>
                m_${serviceName?uncap_first}ServiceModel.AssignModelNameToErrorResponse(Aws::Crt::String("${errorId}"),
                                                                        ${factoryFunction});
</#if>
</#list>
            }

            std::future<RpcError> ${serviceName}Client::Connect(
                ConnectionLifecycleHandler &lifecycleHandler,
                const ConnectionConfig &connectionConfig) noexcept
            {
                return m_connection.Connect(connectionConfig, &lifecycleHandler, m_clientBootstrap);
            }

            void ${serviceName}Client::Close() noexcept { m_connection.Close(); }

            void ${serviceName}Client::WithLaunchMode(std::launch mode) noexcept { m_asyncLaunchMode = mode; }

            ${serviceName}Client::~${serviceName}Client() noexcept { Close(); }

            <#list operations as operation>
            <#assign OperationPascalCaseName = operation.getId().getName()/>
            <#assign operationPascalCaseName = OperationPascalCaseName?uncap_first/>
            <#if context.getOutputEventStreamInfo(operation).isPresent()>

            std::shared_ptr<${OperationPascalCaseName}Operation> ${serviceName}Client::New${OperationPascalCaseName}(
                std::shared_ptr<${OperationPascalCaseName}StreamHandler> streamHandler) noexcept
            {
                return Aws::Crt::MakeShared<${OperationPascalCaseName}Operation>(
                    m_allocator,
                    m_connection,
                    std::move(streamHandler),
                    m_${serviceName?uncap_first}ServiceModel.m_${operationPascalCaseName}OperationContext,
                    m_allocator);
            }

            <#else>

            std::shared_ptr<${OperationPascalCaseName}Operation> ${serviceName}Client::New${OperationPascalCaseName}() noexcept
            {
                auto operation = Aws::Crt::MakeShared<${OperationPascalCaseName}Operation>(
                    m_allocator, m_connection, m_${serviceName?uncap_first}ServiceModel.m_${operationPascalCaseName}OperationContext, m_allocator);
                operation->WithLaunchMode(m_asyncLaunchMode);
                return operation;
            }

            </#if>
            </#list>

<@endNamespace namespaceList=context.getNamespaceAsList() spacesPerTab=4/>
