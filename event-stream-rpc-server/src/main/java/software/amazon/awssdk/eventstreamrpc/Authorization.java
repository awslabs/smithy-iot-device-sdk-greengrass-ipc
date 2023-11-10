/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.eventstreamrpc;

/**
 * Authorization decision object contains the decision in general
 * and the authentication data along with it.
 */
public enum Authorization {
    ACCEPT,
    REJECT
}
