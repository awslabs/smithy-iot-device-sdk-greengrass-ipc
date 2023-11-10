/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.python;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.eventstreamrpc.python.client.ClientRenderer;
import software.amazon.smithy.eventstreamrpc.python.model.ModelRenderer;
import software.amazon.smithy.model.Model;

import java.io.File;
import java.util.logging.Logger;

public class PythonCodegenPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(PythonCodegenPlugin.class.getSimpleName());

    public static final String SMITHY_NAMESPACE_PREFIX = "smithy";

    @Override
    public String getName() {
        return "event-stream-rpc-python";
    }

    @Override
    public void execute(PluginContext pluginContext) {
        LOGGER.info("EventStream RPC Python code generation plugin running");

        final Model model = pluginContext.getModel();
        final ServiceCodegenContext context = new ServiceCodegenContext(pluginContext, model,
                pluginContext.getSettings().getStringMember("serviceShapeId").get().getValue());
        final ModelRenderer modelRenderer = new ModelRenderer(context);
        pluginContext.getFileManifest().writeFile(
                new File(context.getModuleFileDirectory(), "model.py").toPath(), modelRenderer.renderServiceModel());

        if (pluginContext.getSettings()
                .getBooleanMemberOrDefault("generateClientStubs", Boolean.FALSE).booleanValue()) {
            final ClientRenderer clientRenderer = new ClientRenderer(context, "client.py.ftl");
            pluginContext.getFileManifest().writeFile(
                    new File(context.getModuleFileDirectory(), "client.py").toPath(), clientRenderer.renderClient());

            final ClientRenderer easyClientRenderer = new ClientRenderer(context, "clientv2.py.ftl");
            pluginContext.getFileManifest().writeFile(
                    new File(context.getModuleFileDirectory(), "clientv2.py").toPath(),
                    easyClientRenderer.renderClient());
        }
        if (pluginContext.getSettings()
                .getBooleanMemberOrDefault("generateServerStubs", Boolean.FALSE).booleanValue()) {
            throw new UnsupportedOperationException("Server stub generation not implemented yet!");
        }
        /* Not appropriate to generate and overwrite this file at the destination
        pluginContext.getFileManifest().writeFile(
                new File(context.getModuleFileDirectory(), "__init__.py").toPath(), "");
         */
    }
}
