<#include "macros/Namespace.ftl">
<#assign serviceName=context.getServiceShapeName()/>
<#assign namespace=context.getNamespaceAsCppPath()/>
<#assign export=context.getExportName()/>
#pragma once
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is generated. */

#include <${namespace}/${serviceName}Model.h>

#include <memory>

using namespace Aws::Eventstreamrpc;

<@startNamespace namespaceList=context.getNamespaceAsList() spacesPerTab=4/>
            class ${export} DefaultConnectionConfig : public ConnectionConfig
            {
              public:
                DefaultConnectionConfig() noexcept;
            };

            class ${export} ${serviceName}Client
            {
              public:
                ${serviceName}Client(
                    Aws::Crt::Io::ClientBootstrap &clientBootstrap,
                    Aws::Crt::Allocator *allocator = Aws::Crt::g_allocator) noexcept;
                /**
                 * Connect the client to the server
                 * @param lifecycleHandler An interface that is called upon when lifecycle events relating to the connection occur.
                 * @param connectionConfig The configuration parameters used for establishing the connection.
                 * @return An `RpcError` that can be used to check whether the connection was established.
                 */
                std::future<RpcError> Connect(
                    ConnectionLifecycleHandler &lifecycleHandler,
                    const ConnectionConfig &connectionConfig = DefaultConnectionConfig()) noexcept;
                bool IsConnected() const noexcept { return m_connection.IsOpen(); }
                void Close() noexcept;
                void WithLaunchMode(std::launch mode) noexcept;

<#list operations as operation>
<#assign OperationPascalCaseName = operation.getId().getName()/>
<#if operation.hasTrait("documentation")>
    /**
    <#list operation.findTrait("documentation").get().getValue()?split("\n") as docLine>
      * ${docLine}
    </#list>
      */
</#if>
<#if context.getOutputEventStreamInfo(operation).isPresent()>
                std::shared_ptr<${OperationPascalCaseName}Operation> New${OperationPascalCaseName}(std::shared_ptr<${OperationPascalCaseName}StreamHandler> streamHandler) noexcept;
<#else>
                std::shared_ptr<${OperationPascalCaseName}Operation> New${OperationPascalCaseName}() noexcept;
</#if>

</#list>

                ~${serviceName}Client() noexcept;

              private:
                ${serviceName}ServiceModel m_${serviceName?uncap_first}ServiceModel;
                ClientConnection m_connection;
                Aws::Crt::Io::ClientBootstrap &m_clientBootstrap;
                Aws::Crt::Allocator *m_allocator;
                MessageAmendment m_connectAmendment;
                std::launch m_asyncLaunchMode;
            };
<@endNamespace namespaceList=context.getNamespaceAsList() spacesPerTab=4/>
