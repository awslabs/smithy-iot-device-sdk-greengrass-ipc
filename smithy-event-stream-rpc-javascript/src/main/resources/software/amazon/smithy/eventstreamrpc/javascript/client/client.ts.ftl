/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

/* This file is generated */

<#assign serviceName = context.getServiceShape().getId().getName()>
<#assign lowerServiceName = fn_to_lower.apply(serviceName)>

/**
 * @packageDocumentation
 * @module ${lowerServiceName}
 */

import * as model from "./model"
import * as model_utils from "./model_utils";
import * as eventstream_rpc from "../eventstream_rpc";
import {EventEmitter} from "events";

export {model};

<#assign hasServiceDocumentation = context.hasDocumentation(context.getServiceShape())>
/**
 * A network client for interacting with the ${serviceName} service using the eventstream RPC protocol.
<#if hasServiceDocumentation>
<#assign serviceDocumentation = context.getDocumentation(context.getServiceShape())>
 *
 * ${serviceDocumentation}
</#if>
 */
export class Client extends EventEmitter {

    private rpcClient : eventstream_rpc.RpcClient;

    private serviceModel : eventstream_rpc.EventstreamRpcServiceModel;

    /**
     * Constructor for a ${serviceName} service client.
     *
     * @param config client configuration settings
     */
    constructor(config: eventstream_rpc.RpcClientConfig) {
        super();
        this.serviceModel = model_utils.makeServiceModel();
        this.rpcClient = eventstream_rpc.RpcClient.new(config);

        this.rpcClient.on('disconnection', (eventData?: eventstream_rpc.DisconnectionEvent) => {
            setImmediate(() => {
                this.emit(Client.DISCONNECTION, eventData);
            });
        });
    }

    /**
     * Attempts to open an eventstream connection to the configured remote endpoint.  Returned promise will be fulfilled
     * if the transport-level connection is successfully established and the eventstream handshake completes without
     * error.
     *
     * connect() may only be called once.
     */
    async connect() : Promise<void> {
        await this.rpcClient.connect();
    }

    /**
     * Shuts down the client and begins the process of releasing all native resources associated with the client
     * as well as any unclosed operations.  It is critical that this function be called when finished with the client;
     * otherwise, native resources will leak.
     *
     * The client tracks unclosed operations and, as part of this process, closes them as well.
     *
     * Once a client has been closed, it may no longer be used.
     */
    async close() : Promise<void> {
        await this.rpcClient.close();
    }

    /**
     * Event emitted when the client's underlying network connection is ended.  Only emitted if the connection
     * was previously successfully established.
     *
     * Listener type: {@link eventstream_rpc.DisconnectionListener}
     *
     * @event
     */
    static DISCONNECTION : string = 'disconnection';

    on(event: 'disconnection', listener: eventstream_rpc.DisconnectionListener): this;

    on(event: string | symbol, listener: (...args: any[]) => void): this {
        super.on(event, listener);
        return this;
    }

    /************************ Service Operations ************************/

<#list operations as operation>
<#assign operationName = operation.getId().getName()>
<#assign operationBaseName = fn_uncapitalize_first.apply(operationName)>
<#assign requestType = operation.getInput().get().getName()>
<#assign responseType = operation.getOutput().get().getName()>
<#assign operationHasDocumentation = context.hasDocumentation(operation)>
<#assign hasStreamingResponse = context.getOutputEventStreamInfo(operation).isPresent()>
<#assign hasStreamingRequest = context.getInputEventStreamInfo(operation).isPresent()>
<#if hasStreamingRequest||hasStreamingResponse>
<#assign requestStreamType=hasStreamingRequest?then("model." + context.getInputEventStreamInfo(operation).get().getEventStreamTarget().getId().getName(), "void")>
<#assign responseStreamType=hasStreamingResponse?then("model." + context.getOutputEventStreamInfo(operation).get().getEventStreamTarget().getId().getName(), "void")>
    /**
     * Creates a ${operationName} streaming operation.
<#if operationHasDocumentation>
<#assign operationDocumentation = context.getDocumentation(operation)>
     *
     * ${operationDocumentation}
</#if>
     *
     * Once created, the streaming operation must be started by a call to activate().
     *
     * If the operation allows for streaming input, the user may attach event listeners to receive messages.
     *
     * If the operation allows for streaming output, the user may call sendProtocolMessage() to send messages on
     * the operation's event stream once the operation has been activated.
     *
     * The user should close() a streaming operation once finished with it.  If close() is not called, the native
     * resources associated with the streaming operation will not be freed until the client is closed.
     *
     * @param request data describing the ${operationName} streaming operation to create
     * @param options additional eventstream options to use while this operation is active
     * @return a new StreamingOperation object
     */
    ${operationBaseName}(request : model.${requestType}, options?: eventstream_rpc.OperationOptions) : eventstream_rpc.StreamingOperation<model.${requestType}, model.${responseType}, ${requestStreamType}, ${responseStreamType}> {
        let operationConfig = {
            name: "${operation.getId()}",
            client: this.rpcClient,
            options: (options) ? options : {}
        };

        return new eventstream_rpc.StreamingOperation<model.${requestType}, model.${responseType}, ${requestStreamType}, ${responseStreamType}>(request, operationConfig, this.serviceModel);
    }

<#else>
    /**
     * Performs a ${operationName} operation.
<#if operationHasDocumentation>
<#assign operationDocumentation = context.getDocumentation(operation)>
     *
<#list operationDocumentation?split("\n") as docLine>
     * ${docLine}
</#list>
</#if>
     *
     * @param request data describing the ${operationName} operation to perform
     * @param options additional eventstream options to use while performing this operation
     * @return a Promise that is resolved with the ${operationName} operation's result, or rejected with an
     *    RpcError
     */
    async ${operationBaseName}(request : model.${requestType}, options?: eventstream_rpc.OperationOptions) : Promise<model.${responseType}> {
        let operationConfig = {
            name: "${operation.getId()}",
            client: this.rpcClient,
            options: (options) ? options : {}
        };

        let operation = new eventstream_rpc.RequestResponseOperation<model.${requestType}, model.${responseType}>(operationConfig, this.serviceModel);

        return await operation.activate(request);
    }

</#if>
</#list>
}
