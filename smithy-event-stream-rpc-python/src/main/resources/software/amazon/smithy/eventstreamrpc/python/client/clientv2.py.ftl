# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0.

# This file is generated

from . import model
from .client import ${context.getServiceShape().getId().getName()}Client
from . import client

import concurrent.futures
import datetime
import typing


class ${context.getServiceShape().getId().getName()}ClientV2:
    """
    V2 client for the ${context.getServiceShape().getId().getName()} service.  When finished with the client,
    you must call close() to free the client's native resources.

    Args:
        client: Connection that this client will use. If you do not provide one, it will be made automatically.
        executor: Executor used to run on_stream_event and on_stream_closed callbacks to avoid blocking the networking
         thread. By default, a ThreadPoolExecutor will be created and used. Use None to run callbacks in the
         networking thread, but understand that your code can deadlock the networking thread if it performs a
         synchronous network call.
    """

    def __init__(self, client: typing.Optional[${context.getServiceShape().getId().getName()}Client] = None,
                 executor: typing.Optional[concurrent.futures.Executor] = True):
        if client is None:
            import awsiot.greengrasscoreipc
            client = awsiot.greengrasscoreipc.connect()
        self.client = client
        if executor is True:
            executor = concurrent.futures.ThreadPoolExecutor()
        self.executor = executor
        self.ignore_executor_exceptions = False

    def close(self, *, executor_wait=True) -> concurrent.futures.Future:
        """
        Close the underlying connection and shutdown the event executor (if any)

        Args:
            executor_wait: If true (default), then this method will block until the executor finishes running
                all tasks and shuts down.

        Returns:
            The future which will complete
            when the shutdown process is done. The future will have an
            exception if shutdown was caused by an error, or a result
            of None if the shutdown was clean and user-initiated.
        """
        fut = self.client.close()

        # events that arrive during the shutdown process will generate executor exceptions, ignore them
        self.ignore_executor_exceptions	= True
        if self.executor is not None:
            self.executor.shutdown(wait=executor_wait)
        return fut

    def __combine_futures(self, future1: concurrent.futures.Future,
                          future2: concurrent.futures.Future) -> concurrent.futures.Future:
        def callback(*args, **kwargs):
            try:
                future1.result()
            except Exception as e:
                future2.set_exception(e)
        future1.add_done_callback(callback)
        return future2

    @staticmethod
    def __handle_error():
        import sys
        import traceback
        traceback.print_exc(file=sys.stderr)

    def __wrap_error(self, func):
        def wrapper(*args, **kwargs):
            try:
                return func(*args, **kwargs)
            except Exception as e:
                self.__handle_error()
                raise e
        return wrapper

    def __create_stream_handler(real_self, operation, on_stream_event, on_stream_error, on_stream_closed):
        stream_handler_type = type(operation + 'Handler', (getattr(client, operation + "StreamHandler"),), {})
        if on_stream_event is not None:
            on_stream_event = real_self.__wrap_error(on_stream_event)
            def handler(self, event):
                if real_self.executor is not None:
                    try:
                        real_self.executor.submit(on_stream_event, event)
                    except RuntimeError:
                        if not real_self.ignore_executor_exceptions:
                            raise
                else:
                    on_stream_event(event)
            setattr(stream_handler_type, "on_stream_event", handler)
        if on_stream_error is not None:
            on_stream_error = real_self.__wrap_error(on_stream_error)
            def handler(self, error):
                return on_stream_error(error)
            setattr(stream_handler_type, "on_stream_error", handler)
        if on_stream_closed is not None:
            on_stream_closed = real_self.__wrap_error(on_stream_closed)
            def handler(self):
                if real_self.executor is not None:
                    try:
                        real_self.executor.submit(on_stream_closed)
                    except RuntimeError:
                        if real_self.ignore_executor_exceptions:
                            raise
                else:
                    on_stream_closed()
            setattr(stream_handler_type, "on_stream_closed", handler)
        return stream_handler_type()

    def __handle_stream_handler(real_self, operation, stream_handler, on_stream_event, on_stream_error, on_stream_closed):
        if stream_handler is not None and (on_stream_event is not None or on_stream_error is not None or on_stream_closed is not None):
            raise ValueError("Must choose either stream_handler or on_stream_event/on_stream_error/on_stream_closed")
        if stream_handler is not None and real_self.executor is not None:
            return real_self.__create_stream_handler(operation, stream_handler.on_stream_event,
                                                     stream_handler.on_stream_error, stream_handler.on_stream_closed)
        if stream_handler is None:
            return real_self.__create_stream_handler(operation, on_stream_event, on_stream_error, on_stream_closed)
        return stream_handler

<#list operations as operation>
<#assign structure = fn_shapeId_to_shape.apply(operation.getInput().get())>
<#assign memberShapes = fn_structure_members.apply(structure)>
<#assign operationBaseName = operation.getId().getName()>
<#assign requestType = operation.getInput().get().getName()>
<#assign responseType = operation.getOutput().get().getName()>
<#assign streaming = context.getOutputEventStreamInfo(operation).isPresent()>
<#assign star_arg = memberShapes?has_content || streaming>
    def ${fn_camel_to_python.apply(operation.getId().getName())}(self<#if star_arg>, *</#if><#if memberShapes?has_content>,
        <#list memberShapes as memberShape>${fn_camel_to_python.apply(memberShape.getMemberName())}: typing.Optional[${context.getTypeName(memberShape, "model.")}] = None<#sep>,${"\n"}        </#sep></#list></#if><#if streaming>,
        stream_handler: typing.Optional[client.${operation.getId().getName()}StreamHandler] = None,
        <#assign responseStreamType = fn_shapeId_to_shape.apply(context.getOutputEventStreamInfo(operation).get().getEventStreamTarget().getId())>
        on_stream_event: typing.Optional[typing.Callable[[${context.getTypeName(responseStreamType, "model.")}], None]] = None,
        on_stream_error: typing.Optional[typing.Callable[[Exception], bool]] = None,
        on_stream_closed: typing.Optional[typing.Callable[[], None]] = None
</#if>) -> <#if streaming>typing.Tuple[</#if>model.${responseType}<#if streaming>, client.${operation.getId().getName()}Operation]</#if>:
        """
        Perform the ${operation.getId().getName()} operation synchronously.
        <#if streaming>
        The initial response or error will be returned synchronously, further events will arrive via the streaming
        callbacks
        </#if><#if star_arg>

        Args:</#if>
    <#list memberShapes as memberShape>
        <#assign memberName = fn_camel_to_python.apply(memberShape.getMemberName())>
        <#assign memberTargetShape = context.getShape(memberShape.getTarget())>
        <#if memberTargetShape.hasTrait("enum")>
            <#assign description = memberTargetShape.getId().name + " enum value">
        <#else>
            <#assign description = "">
        </#if>
            ${memberName}: ${description}
    </#list>
    <#if streaming>
            stream_handler: Methods on this object will be called as stream events happen on this operation. If an
                executor is provided, the on_stream_event and on_stream_closed methods will run in the executor.
            on_stream_event: Callback for stream events. Mutually exclusive with stream_handler. If an executor is
                provided, this method will run in the executor.
            on_stream_error: Callback for stream errors. Return true to close the stream, return false to keep the
                stream open. Mutually exclusive with stream_handler. Even if an executor is provided, this method
                will not run in the executor.
            on_stream_closed: Callback for when the stream closes. Mutually exclusive with stream_handler. If an
                executor is provided, this method will run in the executor.
    </#if>
        """
        <#if streaming>
        (fut, op) = self.${fn_camel_to_python.apply(operation.getId().getName())}_async(<#list memberShapes as memberShape>${fn_camel_to_python.apply(memberShape.getMemberName())}=${fn_camel_to_python.apply(memberShape.getMemberName())}<#sep>, </#sep></#list><#if streaming><#if memberShapes?has_content>, </#if>
            stream_handler=stream_handler, on_stream_event=on_stream_event, on_stream_error=on_stream_error,
            on_stream_closed=on_stream_closed</#if>)
        return fut.result(), op
        <#else>
        return self.${fn_camel_to_python.apply(operation.getId().getName())}_async(<#list memberShapes as memberShape>${fn_camel_to_python.apply(memberShape.getMemberName())}=${fn_camel_to_python.apply(memberShape.getMemberName())}<#sep>, </#sep></#list><#if streaming><#if memberShapes?has_content>, </#if>
            stream_handler=stream_handler, on_stream_event=on_stream_event, on_stream_error=on_stream_error,
            on_stream_closed=on_stream_closed</#if>).result()
        </#if>

    def ${fn_camel_to_python.apply(operation.getId().getName())}_async(self<#if star_arg>, *</#if><#if memberShapes?has_content>,
        <#list memberShapes as memberShape>${fn_camel_to_python.apply(memberShape.getMemberName())}: typing.Optional[${context.getTypeName(memberShape, "model.")}] = None<#sep>,${"\n"}        </#sep></#list></#if><#if
        streaming>,
        stream_handler: client.${operation.getId().getName()}StreamHandler = None,
        <#assign responseStreamType = fn_shapeId_to_shape.apply(context.getOutputEventStreamInfo(operation).get().getEventStreamTarget().getId())>
        on_stream_event: typing.Optional[typing.Callable[[${context.getTypeName(responseStreamType, "model.")}], None]] = None,
        on_stream_error: typing.Optional[typing.Callable[[Exception], bool]] = None,
        on_stream_closed: typing.Optional[typing.Callable[[], None]] = None
        </#if>):  # type: (...) -> <#if streaming>typing.Tuple[</#if>concurrent.futures.Future[model.${responseType}]<#if streaming>, client.${operation.getId().getName()}Operation]</#if>
        """
        Perform the ${operation.getId().getName()} operation asynchronously.
        <#if streaming>
        The initial response or error will be returned as the result of the asynchronous future, further events will
        arrive via the streaming callbacks
        </#if><#if star_arg>

        Args:</#if>
        <#list memberShapes as memberShape>
            <#assign memberName = fn_camel_to_python.apply(memberShape.getMemberName())>
            <#assign memberTargetShape = context.getShape(memberShape.getTarget())>
            <#if memberTargetShape.hasTrait("enum")>
                <#assign description = memberTargetShape.getId().name + " enum value">
            <#else>
                <#assign description = "">
            </#if>
            ${memberName}: ${description}
        </#list>
        <#if streaming>
            stream_handler: Methods on this object will be called as stream events happen on this operation. If an
                executor is provided, the on_stream_event and on_stream_closed methods will run in the executor.
            on_stream_event: Callback for stream events. Mutually exclusive with stream_handler. If an executor is
                provided, this method will run in the executor.
            on_stream_error: Callback for stream errors. Return true to close the stream, return false to keep the
                stream open. Mutually exclusive with stream_handler. Even if an executor is provided, this method
                will not run in the executor.
            on_stream_closed: Callback for when the stream closes. Mutually exclusive with stream_handler. If an
                executor is provided, this method will run in the executor.
        </#if>
        """
        <#if streaming>
        stream_handler = self.__handle_stream_handler("${operation.getId().getName()}", stream_handler,
            on_stream_event, on_stream_error, on_stream_closed)
        </#if>
        request = model.${requestType}(<#list memberShapes as memberShape>${fn_camel_to_python.apply(memberShape.getMemberName())}=${fn_camel_to_python.apply(memberShape.getMemberName())}<#sep>, </#sep></#list>)
        operation = self.client.new_${fn_camel_to_python.apply(operation.getId().getName())}(<#if streaming>stream_handler</#if>)
        write_future = operation.activate(request)
        return self.__combine_futures(write_future, operation.get_response())<#if streaming>, operation</#if>
<#sep>

</#sep>
</#list>
