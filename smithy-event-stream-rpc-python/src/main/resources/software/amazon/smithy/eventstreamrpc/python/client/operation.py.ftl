<#assign operationBaseName = operation.getId().getName()>
<#assign requestType = operation.getInput().get().getName()>
<#assign responseType = operation.getOutput().get().getName()>
<#if context.getOutputEventStreamInfo(operation).isPresent()>
class ${operationBaseName}StreamHandler(rpc.StreamResponseHandler):
    """
    Event handler for ${operationBaseName}Operation

    Inherit from this class and override methods to handle
    stream events during a ${operationBaseName}Operation.
    """

    <#assign responseStreamType = context.getOutputEventStreamInfo(operation).get().getEventStreamTarget().getId().getName()>
    def on_stream_event(self, event: model.${responseStreamType}) -> None:
        """
        Invoked when a ${responseStreamType} is received.
        """
        pass

    def on_stream_error(self, error: Exception) -> bool:
        """
        Invoked when an error occurs on the operation stream.

        Return True if operation should close as a result of this error,
        """
        return True

    def on_stream_closed(self) -> None:
        """
        Invoked when the stream for this operation is closed.
        """
        pass


</#if>
class ${operationBaseName}Operation(model._${operation.getId().getName()}Operation):
    """
    ${operationBaseName}Operation

    Create with ${context.getServiceShape().getId().getName()}Client.new_${fn_camel_to_python.apply(operation.getId().getName())}()
    """

    def activate(self, request: model.${requestType}):  # type: (...) -> concurrent.futures.Future[None]
        """
        Activate this operation by sending the initial ${requestType} message.

        Returns a Future which completes with a result of None if the
        request is successfully written to the wire, or an exception if
        the request fails to send.
        """
        return self._activate(request)

<#if context.getInputEventStreamInfo(operation).isPresent()>
    <#assign requestStreamType = context.getInputEventStreamInfo(operation).get().getEventStreamTarget().getId().getName()>
    def send_stream_event(self, event: model.${requestStreamType}) -> concurrent.futures.Future:
        """
        Send next ${requestStreamType} stream event.

        activate() must be called before send_stream_event().

        Returns a Future which completes with a result of None if the
        event is successfully written to the wire, or an exception if
        the event fails to send.
        """
        return self._send_stream_event(event)

</#if>
    def get_response(self):  # type: (...) -> concurrent.futures.Future[model.${responseType}]
        """
        Returns a Future which completes with a result of ${responseType},
        when the initial response is received, or an exception.
        """
        return self._get_response()

    def close(self):  # type: (...) -> concurrent.futures.Future[None]
        """
        Close the operation, whether or not it has completed.

        Returns a Future which completes with a result of None
        when the operation has closed.
        """
        return super().close()
