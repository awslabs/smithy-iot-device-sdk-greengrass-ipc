/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is part of greengrass-ipc project. */

package software.amazon.awssdk.eventstreamrpc;

/**
 * Exact implementation of this is between the EventStreamRPCServiceHandler at the Authentication handler itself
 */
public interface AuthenticationData {

    /**
     * Return a human readable string for who the identity of the client/caller is. This
     * string must be appropriate for audit logs and enable tracing specific callers/clients
     * to relevant decision and operations executed
     *
     * @return A human readable string for who the identity of the client/caller is
     */
    public String getIdentityLabel();
}
