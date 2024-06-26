/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is part of greengrass-ipc project. */

package software.amazon.awssdk.eventstreamrpc;

import software.amazon.awssdk.crt.eventstream.Header;

import java.util.List;
import java.util.function.BiFunction;

/**
 * apply() accepts the connection message and produces authentication data from it to at least be
 * used for authorization decisions
 *
 * Exact implementation is up to service implementations to decide what it is and how to handle it
 */
public interface AuthenticationHandler extends BiFunction<List<Header>, byte[], AuthenticationData> { }
