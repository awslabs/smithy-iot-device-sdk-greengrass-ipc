# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0.

# This file is generated

from . import model
import awsiot.eventstreamrpc as rpc
import concurrent.futures


<#list operations as operation>
<#include "operation.py.ftl">


</#list>
class ${context.getServiceShape().getId().getName()}Client(rpc.Client):
    """
    Client for the ${context.getServiceShape().getId().getName()} service.<#if context.getServiceShape().getId()
    .getName() == "GreengrassCoreIPC">

    There is a new V2 client which should be preferred.
    See the ${context.getServiceShape().getId().getName()}ClientV2 class in the clientv2 subpackage.
    </#if>

    Args:
        connection: Connection that this client will use.
    """

    def __init__(self, connection: rpc.Connection):
        super().__init__(connection, model.SHAPE_INDEX)

<#list operations as operation>
    def new_${fn_camel_to_python.apply(operation.getId().getName())}(self<#if context.getOutputEventStreamInfo(operation).isPresent()>, stream_handler: ${operation.getId().getName()}StreamHandler</#if>) -> ${operation.getId().getName()}Operation:
        """
        Create a new ${operation.getId().getName()}Operation.

        This operation will not send or receive any data until activate()
        is called. Call activate() when you're ready for callbacks and
        events to fire.
    <#if context.getOutputEventStreamInfo(operation).isPresent()>

        Args:
            stream_handler: Methods on this object will be called as
                stream events happen on this operation.
    </#if>
        """
        return self._new_operation(${operation.getId().getName()}Operation<#if context.getOutputEventStreamInfo(operation).isPresent()>, stream_handler</#if>)
<#sep>

</#sep>
</#list>
