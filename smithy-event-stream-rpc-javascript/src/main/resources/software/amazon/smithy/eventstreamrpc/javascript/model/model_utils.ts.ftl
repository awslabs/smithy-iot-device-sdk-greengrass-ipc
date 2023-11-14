/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is generated */

import * as eventstream_rpc_utils from "../eventstream_rpc_utils";
import * as model from "./model";
import {eventstream} from "aws-crt";
import * as eventstream_rpc from "../eventstream_rpc";
import {toUtf8} from "@aws-sdk/util-utf8-browser";

<#include "model_utils_service_model.ftl">

<#include "model_utils_normalizers.ftl">
<#include "model_utils_validators.ftl">
<#include "model_utils_deserializers.ftl">
<#include "model_utils_serializers.ftl">
