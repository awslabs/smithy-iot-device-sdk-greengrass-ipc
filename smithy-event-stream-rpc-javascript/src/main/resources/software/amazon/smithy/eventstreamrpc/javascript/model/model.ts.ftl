/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is generated */

<#assign lowerServiceName = fn_to_lower.apply(context.getServiceShape().getId().getName())>

/**
 * @packageDocumentation
 * @module ${lowerServiceName}
 */

import {eventstream} from "aws-crt";

<#list allShapes as shape>
<#if fn_is_enum.apply(shape)>
<#include "model_enum.ftl">

<#elseif shape.getDataShape().get().getType().name() == "STRUCTURE" || shape.getDataShape().get().getType().name() == "UNION">
<#include "model_structure.ftl">

</#if>
</#list>
