/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.java.client;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import software.amazon.smithy.eventstreamrpc.java.NameUtils;
import software.amazon.smithy.eventstreamrpc.java.PoetryWriter;
import software.amazon.smithy.eventstreamrpc.java.ServiceCodegenContext;
import software.amazon.smithy.model.shapes.ServiceShape;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.lang.model.element.Modifier;

public class EasyServiceClientClassBuilder extends ServiceClientClassBuilder {
    public EasyServiceClientClassBuilder(ServiceCodegenContext context) {
        super(context);
    }

    public ClassName getEasyClientImplClassName(final ServiceShape shape) {
        return ClassName.get(context.getBaseServicePackage(), shape.getId().getName() + "ClientV2");
    }

    @Override
    public Collection<JavaFile> apply(ServiceShape shape) {
        if (!shape.getId().getName().equals("GreengrassCoreIPC")) {
            throw new IllegalArgumentException("Only works for Greengrass, not " + context.getModelNamespaceSuffix() + "!");
        }
        final Collection<JavaFile> outputFiles = new LinkedList<>();

        final ClassName implClassName = getEasyClientImplClassName(shape);
        final TypeSpec.Builder implClassBuilder = TypeSpec.classBuilder(implClassName)
                .addModifiers(Modifier.PUBLIC).addSuperinterface(AutoCloseable.class)
                .addJavadoc("V2 Client for Greengrass.\n");

        ClassName clientInterfaceClassName = getClientInterfaceClassName(shape);
        implClassBuilder.addField(FieldSpec.builder(clientInterfaceClassName,
                "client", Modifier.PROTECTED).build());
        implClassBuilder.addField(FieldSpec.builder(ClassName.get(Executor.class),
                "executor", Modifier.PROTECTED).build());
        implClassBuilder.addField(FieldSpec.builder(
                ClassName.get("software.amazon.awssdk.eventstreamrpc", "EventStreamRPCConnection"),
                "connection", Modifier.PROTECTED).build());
        implClassBuilder.addMethod(MethodSpec.methodBuilder("close")
                .addModifiers(Modifier.PUBLIC)
                .addException(Exception.class)
                .addAnnotation(Override.class)
                .beginControlFlow("if (client instanceof $T)", AutoCloseable.class)
                .addStatement("(($T) client).close()", AutoCloseable.class).endControlFlow()
                .beginControlFlow("if (connection != null)")
                .addStatement("connection.close()").endControlFlow()
                .build());

        implClassBuilder.addMethod(MethodSpec.methodBuilder("getClient").addModifiers(Modifier.PUBLIC)
                .addStatement("return client").returns(clientInterfaceClassName).build());

        implClassBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(clientInterfaceClassName, "client")
                .addParameter(ClassName.get("software.amazon.awssdk.eventstreamrpc", "EventStreamRPCConnection"),
                        "connection")
                .addParameter(ClassName.get(Executor.class), "executor")
                .addStatement("this.client = client")
                .addStatement("this.connection = connection")
                .addStatement("this.executor = executor")
                .build());

        ClassName streamerResponseTypeName = implClassName.nestedClass("StreamingResponse");
        implClassBuilder.addType(TypeSpec.classBuilder("StreamingResponse")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(TypeVariableName.get("T"))
                .addTypeVariable(TypeVariableName.get("U"))
                .addField(FieldSpec.builder(TypeVariableName.get("T"), "r").addModifiers(Modifier.PROTECTED, Modifier.FINAL).build())
                .addField(FieldSpec.builder(TypeVariableName.get("U"), "h").addModifiers(Modifier.PROTECTED, Modifier.FINAL).build())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addParameter(TypeVariableName.get("T"), "r").addParameter(TypeVariableName.get("U"), "h").addStatement("this.r = r").addStatement("this.h = h").build())
                .addMethod(MethodSpec.methodBuilder("getResponse").addModifiers(Modifier.PUBLIC).addStatement("return r").returns(TypeVariableName.get("T")).build())
                .addMethod(MethodSpec.methodBuilder("getHandler").addModifiers(Modifier.PUBLIC).addStatement("return h").returns(TypeVariableName.get("U")).build())
                .build());

        ClassName builderClassName = implClassName.nestedClass("Builder");
        implClassBuilder.addType(TypeSpec.classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(clientInterfaceClassName, "client", Modifier.PROTECTED)
                .addField(ClassName.get(Executor.class), "executor", Modifier.PROTECTED)
                .addField(
                        FieldSpec.builder(TypeName.BOOLEAN, "useExecutor", Modifier.PROTECTED)
                                .initializer("true").build())
                .addField(
                        FieldSpec.builder(String.class, "socketPath", Modifier.PROTECTED).initializer(
                                "System.getenv(\"AWS_GG_NUCLEUS_DOMAIN_SOCKET_FILEPATH_FOR_COMPONENT\")").build())
                .addField(
                        FieldSpec.builder(String.class, "authToken", Modifier.PROTECTED).initializer(
                                "System.getenv(\"SVCUID\")").build())
                .addField(
                        FieldSpec.builder(TypeName.INT, "port", Modifier.PROTECTED).initializer("8888").build())
                .addField(FieldSpec.builder(
                        ClassName.get("software.amazon.awssdk.eventstreamrpc", "EventStreamRPCConnection"),
                        "connection", Modifier.PROTECTED).initializer("null").build())
                .addField(FieldSpec.builder(ClassName.get("software.amazon.awssdk.crt.io.SocketOptions",
                                "SocketDomain"), "socketDomain",
                        Modifier.PROTECTED).initializer("SocketDomain.LOCAL").build())
                .addMethod(MethodSpec.methodBuilder("build")
                        .addModifiers(Modifier.PUBLIC)
                        .beginControlFlow("if (client == null)")
                        .addStatement("String ipcServerSocketPath = this.socketPath")
                        .addStatement("String authToken = this.authToken")
                        .beginControlFlow("try ($T elGroup = new EventLoopGroup(1);\n"
                                        + "     $T clientBootstrap = new ClientBootstrap(elGroup, null);\n"
                                        + "     $T socketOptions = new SocketOptions())",
                                ClassName.get("software.amazon.awssdk.crt.io", "EventLoopGroup"),
                                ClassName.get("software.amazon.awssdk.crt.io", "ClientBootstrap"),
                                ClassName.get("software.amazon.awssdk.crt.io", "SocketOptions"))
                        .addStatement("socketOptions.connectTimeoutMs = 3000")
                        .addStatement("socketOptions.domain = this.socketDomain")
                        .addStatement("socketOptions.type = SocketOptions.SocketType.STREAM")
                        .addCode("\n")
                        .addStatement("final $T config = new EventStreamRPCConnectionConfig(clientBootstrap, elGroup,"
                                        + " socketOptions, null, ipcServerSocketPath, this.port, "
                                        + "$T.connectMessageSupplier(authToken))",
                                    ClassName.get("software.amazon.awssdk.eventstreamrpc",
                                            "EventStreamRPCConnectionConfig"),
                                    ClassName.get("software.amazon.awssdk.eventstreamrpc",
                                            "GreengrassConnectMessageSupplier"))
                        .addStatement("connection = new EventStreamRPCConnection(config)")
                        .addStatement("$T connected = new CompletableFuture<>()", ParameterizedTypeName.get(
                                CompletableFuture.class, Void.class))
                        .addCode(CodeBlock.builder()
                                .beginControlFlow("connection.connect(new EventStreamRPCConnection.LifecycleHandler()")
                                .beginControlFlow("@Override public void onConnect()")
                                    .addStatement("connected.complete(null)")
                                .endControlFlow()
                                .beginControlFlow("@Override public void onDisconnect(int errorCode)").endControlFlow()
                                .beginControlFlow("@Override public boolean onError(Throwable t)")
                                    .addStatement("connected.completeExceptionally(t)")
                                    .addStatement("return true")
                                .endControlFlow()
                                .endControlFlow(")").build())
                        .beginControlFlow("try")
                        .addStatement("connected.get()")
                        .endControlFlow()
                        .beginControlFlow("catch ($T | $T e)", ExecutionException.class, InterruptedException.class)
                        .addStatement("connection.close()")
                        .addStatement("throw new $T(e)", IOException.class)
                        .endControlFlow()
                        .addStatement("this.client = new $T(connection)", getClientImplClassName(shape))
                        .endControlFlow()
                        .endControlFlow()
                        .beginControlFlow("if (this.useExecutor && this.executor == null)")
                        .addStatement("this.executor = $T.newCachedThreadPool()", ClassName.get(Executors.class))
                        .endControlFlow()
                        .addStatement("return new $T(this.client, this.connection, this.executor)", implClassName)
                        .addException(IOException.class)
                        .returns(implClassName).build())
                .addMethod(MethodSpec.methodBuilder("withClient")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(clientInterfaceClassName, "client")
                        .addStatement("this.client = client")
                        .addStatement("return this")
                        .returns(builderClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("withAuthToken")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "authToken")
                        .addStatement("this.authToken = authToken")
                        .addStatement("return this")
                        .returns(builderClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("withSocketPath")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "socketPath")
                        .addStatement("this.socketPath = socketPath")
                        .addStatement("return this")
                        .returns(builderClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("withSocketDomain")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.get("software.amazon.awssdk.crt.io.SocketOptions",
                                "SocketDomain"), "domain")
                        .addStatement("this.socketDomain = domain")
                        .addStatement("return this")
                        .returns(builderClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("withPort")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(TypeName.INT, "port")
                        .addStatement("this.port = port")
                        .addStatement("return this")
                        .returns(builderClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("withExecutor")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.get(Executor.class), "executor")
                        .addStatement("this.useExecutor = true")
                        .addStatement("this.executor = executor")
                        .addStatement("return this")
                        .returns(builderClassName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("withoutExecutor")
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("this.useExecutor = false")
                        .addStatement("this.executor = null")
                        .addStatement("return this")
                        .returns(builderClassName).build())
                .build());

        implClassBuilder.addMethod(MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return new $T()", builderClassName)
                .returns(builderClassName)
                .build());

        context.getAllOperations().forEach(operationShape -> {
            final ClassName requestClassName = context.getOperationRequestClassName(operationShape);
            final ClassName streamingResponseClassName = context.getOperationStreamingResponseClassName(operationShape);

            final TypeName streamingResponseHandlerType =
                    ParameterizedTypeName.get(PoetryWriter.CN_STREAM_RESPONSE_HANDLER, streamingResponseClassName);

            final String operationName = NameUtils.uncapitalize(operationShape.getId().getName());

            final String requestParamName = "request";
            String streamHandlerParamName = "streamResponseHandler";
            final ParameterSpec.Builder requestParam = ParameterSpec
                    .builder(requestClassName, requestParamName, Modifier.FINAL)
                    .addJavadoc("request object\n");
            final ParameterSpec.Builder streamingHandlerParam = ParameterSpec.builder(
                    streamingResponseHandlerType, streamHandlerParamName, Modifier.FINAL)
                    .addJavadoc("Methods on this object will be called as stream events happen on this operation.\n  "
                            + "  If an executor is provided, the onStreamEvent and onStreamClosed methods will "
                            + "run in the executor.\n");

            boolean isStreaming =
                    context.getOutputEventStreamInfo(operationShape).orElse(null) != null;

            final TypeName syncReturnType = isStreaming ? ParameterizedTypeName.get(streamerResponseTypeName,
                    ClassName.get(context.getBaseModelPackage(), operationShape.getOutput().get().getName()),
                    getOperationResponseType(operationShape))
                    : ClassName.get(context.getBaseModelPackage(), operationShape.getOutput().get().getName());
            final TypeName asyncReturnType = isStreaming ? ParameterizedTypeName.get(streamerResponseTypeName,
                    ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                            ClassName.get(context.getBaseModelPackage(), operationShape.getOutput().get().getName())),
                            getOperationResponseType(operationShape))
                    : ParameterizedTypeName.get(ClassName.get(CompletableFuture.class), syncReturnType);

            MethodSpec.Builder operationSyncBuilder = MethodSpec.methodBuilder(operationName)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Perform the $L operation synchronously.\n", operationName)
                    .addParameter(requestParam.build());
            String asyncOperationName = operationName + "Async";
            MethodSpec.Builder operationAsyncBuilder = MethodSpec.methodBuilder(asyncOperationName)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Perform the $L operation asynchronously.\n", operationName)
                    .addParameter(requestParam.build());

            if (isStreaming) {
                operationSyncBuilder.addParameter(streamingHandlerParam.build());
                operationAsyncBuilder.addParameter(streamingHandlerParam.build());
                operationAsyncBuilder
                        .addJavadoc("The initial response or error will be returned as the result of the "
                                + "asynchronous future, further events will\narrive via the streaming callbacks.\n")
                        .addStatement("$T r = client.$L($L, $T.ofNullable(getStreamingResponseHandler($L)))",
                                getOperationResponseType(operationShape),
                                operationName, requestParamName, Optional.class, streamHandlerParamName)
                        .addStatement("return new $T<>(r.getResponse(), r)", streamerResponseTypeName);
                operationSyncBuilder
                        .addJavadoc("The initial response or error will be returned synchronously, further events "
                                + "will\narrive via the streaming callbacks.\n")
                        .addStatement("$T r = this.$L($L, $L)", asyncReturnType, asyncOperationName, requestParamName,
                                streamHandlerParamName)
                        .addStatement("return new $T<>(getResponse(r.getResponse()), r.getHandler())",
                                streamerResponseTypeName);

                implClassBuilder.addMethod(MethodSpec.methodBuilder(asyncOperationName).addModifiers(Modifier.PUBLIC)
                        .addJavadoc("Perform the $L operation asynchronously.\nThe initial response or error will "
                                        + "be returned as the result of the asynchronous future, further events will\n"
                                        + "arrive via the streaming callbacks.\n",
                                operationName)
                        .addParameter(requestParam.build())
                        .addParameter(ParameterSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Consumer.class), streamingResponseClassName),
                                        "onStreamEvent")
                                .addJavadoc("Callback for stream events. If an executor is provided, this method will"
                                        + " run in the executor.\n")
                                .build())
                        .addParameter(ParameterSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Optional.class),
                                                ParameterizedTypeName.get(ClassName.get(Function.class),
                                                        ClassName.get(Throwable.class), TypeName.BOOLEAN.box())),
                                        "onStreamError")
                                .addJavadoc("Callback for stream errors. Return true to close the stream,\n    return "
                                        + "false to keep the stream open. Even if an executor is provided,\n    "
                                        + "this method will not run in the executor.\n")
                                .build())
                        .addParameter(ParameterSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Optional.class), ClassName.get(Runnable.class)),
                                        "onStreamClosed")
                                .addJavadoc("Callback for when the stream closes. If an executor is provided, this "
                                        + "method will run in the executor.\n")
                                .build())
                        .addStatement("return this.$L($L, getStreamingResponseHandler(onStreamEvent, "
                                + "onStreamError, onStreamClosed))", asyncOperationName, requestParamName)
                        .addJavadoc("\n@return a future which resolves to the response\n")
                        .returns(asyncReturnType)
                        .build());

                // Sync version
                implClassBuilder.addMethod(MethodSpec.methodBuilder(operationName)
                        .addJavadoc("Perform the $L operation synchronously.\nThe initial response or error will be "
                                + "returned synchronously,\nfurther events will arrive via the streaming callbacks.\n",
                                operationName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(requestParam.build())
                        .addParameter(ParameterSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Consumer.class), streamingResponseClassName),
                                "onStreamEvent")
                                .addJavadoc("Callback for stream events. If an executor is provided, this method will"
                                        + " run in the executor.\n")
                                .build())
                        .addParameter(ParameterSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Optional.class),
                                        ParameterizedTypeName.get(ClassName.get(Function.class),
                                                ClassName.get(Throwable.class), TypeName.BOOLEAN.box())),
                                "onStreamError")
                                .addJavadoc("Callback for stream errors. Return true to close the stream,\n    return "
                                        + "false to keep the stream open. Even if an executor is provided,\n    "
                                        + "this method will not run in the executor.\n")
                                .build())
                        .addParameter(ParameterSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Optional.class), ClassName.get(Runnable.class)),
                                "onStreamClosed")
                                .addJavadoc("Callback for when the stream closes. If an executor is provided, this "
                                        + "method will run in the executor.\n")
                                .build())
                        .addStatement("$T r = this.$L($L, onStreamEvent, onStreamError, onStreamClosed)",
                                asyncReturnType, asyncOperationName, requestParamName)
                        .addStatement("return new $T<>(getResponse(r.getResponse()), r.getHandler())",
                                streamerResponseTypeName)
                        .addException(InterruptedException.class)
                        .addJavadoc("\n@throws InterruptedException if thread is interrupted while waiting for the "
                                + "response\n")
                        .addJavadoc("@return the response\n")
                        .returns(syncReturnType)
                        .build());
            } else {
                operationAsyncBuilder.addStatement("return client.$L($L, $T.empty()).getResponse()",
                        operationName, requestParamName, Optional.class);
                operationSyncBuilder.addStatement("return getResponse(this.$L($L))",
                        asyncOperationName, requestParamName);
            }
            operationSyncBuilder
                    .addException(InterruptedException.class)
                    .addJavadoc("\n@throws InterruptedException if thread is interrupted while waiting for the "
                            + "response\n")
                    .addJavadoc("@return the response\n")
                    .returns(syncReturnType);
            operationAsyncBuilder.addJavadoc("\n@return a future which resolves to the response\n").returns(asyncReturnType);

            implClassBuilder.addMethod(operationSyncBuilder.build());
            implClassBuilder.addMethod(operationAsyncBuilder.build());
        });

        implClassBuilder.addMethod(
                MethodSpec.methodBuilder("getResponse").addModifiers(Modifier.PROTECTED, Modifier.STATIC)
                        .addTypeVariable(TypeVariableName.get("T")).addParameter(ParameterSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Future.class), TypeVariableName.get("T")), "fut")
                                .build()).addException(InterruptedException.class).beginControlFlow("try")
                        .addStatement("return fut.get()").endControlFlow()
                        .beginControlFlow("catch ($T e)", ExecutionException.class)
                        .beginControlFlow("if (e.getCause() instanceof $T)", RuntimeException.class)
                        .addStatement("throw (($T) e.getCause())", RuntimeException.class).endControlFlow()
                        .addComment("the cause should always be $T, but we will handle this case anyway",
                                RuntimeException.class)
                        .addStatement("throw new $T(e.getCause())", RuntimeException.class).endControlFlow()
                        .returns(TypeVariableName.get("T")).build());

        implClassBuilder.addMethod(
                MethodSpec.methodBuilder("getStreamingResponseHandler").addModifiers(Modifier.PROTECTED)
                        .addTypeVariable(TypeVariableName.get("T"))
                        .addParameter(ParameterizedTypeName.get(PoetryWriter.CN_STREAM_RESPONSE_HANDLER,
                                TypeVariableName.get("T")), "h")
                        .beginControlFlow("if (h == null || executor == null)")
                        .addStatement("return h")
                        .endControlFlow()
                        .addCode(CodeBlock.builder()
                                .beginControlFlow("return new $T()", ParameterizedTypeName.get(PoetryWriter.CN_STREAM_RESPONSE_HANDLER,
                                        TypeVariableName.get("T")))
                                .beginControlFlow("@Override public void onStreamEvent($T event)",
                                        TypeVariableName.get("T"))
                                .addStatement("executor.execute(() -> h.onStreamEvent(event))")
                                .endControlFlow()
                                .beginControlFlow("@Override public boolean onStreamError($T error)",
                                        ClassName.get(Throwable.class))
                                .addStatement("return h.onStreamError(error)")
                                .endControlFlow()
                                .beginControlFlow("@Override public void onStreamClosed()")
                                .addStatement("executor.execute(h::onStreamClosed)")
                                .endControlFlow()
                                .endControlFlow()
                                .addStatement("")
                                .build())
                        .returns(ParameterizedTypeName.get(PoetryWriter.CN_STREAM_RESPONSE_HANDLER,
                                TypeVariableName.get("T"))).build());

        implClassBuilder.addMethod(
                MethodSpec.methodBuilder("getStreamingResponseHandler").addModifiers(Modifier.PROTECTED)
                        .addTypeVariable(TypeVariableName.get("T"))
                        .addParameter(ParameterSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Consumer.class), TypeVariableName.get("T")),
                                "onStreamEvent").build())
                        .addParameter(ParameterSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Optional.class),
                                        ParameterizedTypeName.get(ClassName.get(Function.class),
                                        ClassName.get(Throwable.class), TypeName.BOOLEAN.box())),
                                "onStreamError").build())
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Optional.class),
                                        ClassName.get(Runnable.class)),
                                "onStreamClosed")
                        .addCode(CodeBlock.builder()
                                .beginControlFlow("return new $T()", ParameterizedTypeName.get(PoetryWriter.CN_STREAM_RESPONSE_HANDLER,
                                        TypeVariableName.get("T")))
                                .beginControlFlow("@Override public void onStreamEvent($T event)",
                                        TypeVariableName.get("T"))
                                .addStatement("onStreamEvent.accept(event)")
                                .endControlFlow()
                                .beginControlFlow("@Override public boolean onStreamError($T error)",
                                        ClassName.get(Throwable.class))
                                .beginControlFlow("if (onStreamError != null && onStreamError.isPresent())")
                                .addStatement("return onStreamError.get().apply(error)")
                                .endControlFlow()
                                .addStatement("return true")
                                .endControlFlow()
                                .beginControlFlow("@Override public void onStreamClosed()")
                                .beginControlFlow("if (onStreamClosed != null && onStreamClosed.isPresent())")
                                .addStatement("onStreamClosed.get().run()")
                                .endControlFlow()
                                .endControlFlow()
                                .endControlFlow()
                                .addStatement("")
                                .build())
                        .returns(ParameterizedTypeName.get(PoetryWriter.CN_STREAM_RESPONSE_HANDLER,
                                TypeVariableName.get("T"))).build());

        outputFiles.add(JavaFile.builder(implClassName.packageName(), implClassBuilder.build()).build());

        return outputFiles;
    }
}
