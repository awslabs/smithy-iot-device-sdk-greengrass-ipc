# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0.

# This file is generated

import awsiot.eventstreamrpc as rpc
import base64
import datetime
import typing


<#assign baseErrorShapeName = context.getServiceShape().getId().getName() + "Error">
class ${baseErrorShapeName}(rpc.ErrorShape):
    """
    Base for all error messages sent by server.
    """

    def _get_error_type_string(self) -> str:
        # overridden in subclasses
        raise NotImplementedError

    def is_retryable(self) -> bool:
        return self._get_error_type_string() == 'server'

    def is_server_error(self) -> bool:
        return self._get_error_type_string() == 'server'

    def is_client_error(self) -> bool:
        return self._get_error_type_string() == 'client'


<#list allShapes as shape>
    <#if fn_is_enum.apply(shape)>
        <#include "class-enum.py.ftl">
    <#elseif shape.getDataShape().get().getType().name() == "STRUCTURE">
        <#include "class-structure.py.ftl">
    <#elseif shape.getDataShape().get().getType().name() == "UNION">
        <#include "class-union.py.ftl">
    </#if>


</#list>
SHAPE_INDEX = rpc.ShapeIndex([
<#list allShapes as shape>
    <#if shape.getDataShape().get().getType().name() == "STRUCTURE">
    ${context.getTypeName(shape.getDataShape().get())},
    </#if>
</#list>
])
<#list operations as operation>


<#include "operation.py.ftl">
</#list>
