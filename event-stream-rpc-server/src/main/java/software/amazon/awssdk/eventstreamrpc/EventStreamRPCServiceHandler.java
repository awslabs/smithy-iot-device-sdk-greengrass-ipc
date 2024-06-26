/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is part of greengrass-ipc project. */

package software.amazon.awssdk.eventstreamrpc;

import java.util.Collection;

/**
 * The EventStream RPC Service Handler
 */
public abstract class EventStreamRPCServiceHandler implements OperationContinuationHandlerFactory {
    private AuthenticationHandler authenticationHandler;
    private AuthorizationHandler authorizationHandler;

    /**
     * Constructs a new EventStreamRPCServiceHandler
     */
    public EventStreamRPCServiceHandler() {
        authorizationHandler = null;
    }

    protected abstract EventStreamRPCServiceModel getServiceModel();

    /**
     * Probably only useful for logging
     * @return Returns the service name for the set of RPC operations
     */
    public String getServiceName() {
        return getServiceModel().getServiceName();
    }

    /**
     * TODO: How may we want to protect this from being re-assigned after service creation?
     * @param handler Sets the authorization handler
     */
    public void setAuthorizationHandler(final AuthorizationHandler handler) {
        this.authorizationHandler = handler;
    }

    /**
     * Use this to determine if the connection should be accepted or rejected for this service
     * @return Returns the authorization handler
     */
    public AuthorizationHandler getAuthorizationHandler() {
        return authorizationHandler;
    }

    @Override
    public Collection<String> getAllOperations() {
        return getServiceModel().getAllOperations();
    }

    /**
     * Pulls caller/client identity when server connection occurs
     * @return Returns the authentication handler
     */
    public AuthenticationHandler getAuthenticationHandler() {
        return authenticationHandler;
    }

    /**
     * TODO: How may we want to protect this from being re-assigned after service creation?
     * @param authenticationHandler Sets the authentication handler
     */
    public void setAuthenticationHandler(AuthenticationHandler authenticationHandler) {
        this.authenticationHandler = authenticationHandler;
    }
}
