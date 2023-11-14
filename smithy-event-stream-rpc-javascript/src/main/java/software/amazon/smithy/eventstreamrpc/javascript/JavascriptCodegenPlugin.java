/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.javascript;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.eventstreamrpc.javascript.client.ClientRenderer;
import software.amazon.smithy.eventstreamrpc.javascript.model.ModelRenderer;
import software.amazon.smithy.model.Model;

import java.io.File;
import java.util.logging.Logger;

public class JavascriptCodegenPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(JavascriptCodegenPlugin.class.getSimpleName());

    public static final String SMITHY_NAMESPACE_PREFIX = "smithy";

    @Override
    public String getName() {
        return "event-stream-rpc-javascript";
    }

    @Override
    public void execute(PluginContext pluginContext) {
        LOGGER.info("EventStream RPC Javascript code generation plugin running");

        final Model model = pluginContext.getModel();
        final ServiceCodegenContext context = new ServiceCodegenContext(pluginContext, model,
                pluginContext.getSettings().getStringMember("serviceShapeId").get().getValue());

        final ModelRenderer modelRenderer = new ModelRenderer(context);
        pluginContext.getFileManifest().writeFile(
                new File(context.getModuleFileDirectory(), "model.ts").toPath(), modelRenderer.renderServiceModel());

        pluginContext.getFileManifest().writeFile(
                new File(context.getModuleFileDirectory(), "model_utils.ts").toPath(), modelRenderer.renderServiceModelUtils());

        if (pluginContext.getSettings()
                .getBooleanMemberOrDefault("generateClientStubs", Boolean.FALSE).booleanValue()) {
            final ClientRenderer clientRenderer = new ClientRenderer(context, "client.ts.ftl");
            pluginContext.getFileManifest().writeFile(
                    new File(context.getModuleFileDirectory(), "client.ts").toPath(), clientRenderer.renderClient());
        }
        if (pluginContext.getSettings()
                .getBooleanMemberOrDefault("generateServerStubs", Boolean.FALSE).booleanValue()) {
            throw new UnsupportedOperationException("Server stub generation not implemented yet!");
        }
    }
}
