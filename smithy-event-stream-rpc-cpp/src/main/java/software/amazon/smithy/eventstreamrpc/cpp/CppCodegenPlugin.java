/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.cpp;

import java.io.File;
import java.util.logging.Logger;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.eventstreamrpc.cpp.ServiceCodegenContext.CppFileType;
import software.amazon.smithy.eventstreamrpc.cpp.client.*;
import software.amazon.smithy.eventstreamrpc.cpp.model.*;
import software.amazon.smithy.model.Model;

public class CppCodegenPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(CppCodegenPlugin.class.getSimpleName());

    public static final String SMITHY_NAMESPACE_PREFIX = "smithy";

    @Override
    public String getName() {
        return "event-stream-rpc-cpp";
    }

    @Override
    public void execute(PluginContext pluginContext) {
        LOGGER.info("Greengrass IPC C++ code generation plugin running");

        final Model model = pluginContext.getModel();
        final ServiceCodegenContext context = new ServiceCodegenContext(pluginContext, model,
                pluginContext.getSettings().getStringMember("serviceShapeId").get().getValue());
        final ModelHeaderRenderer modelHeaderRenderer = new ModelHeaderRenderer(context);
        final ModelSourceRenderer modelSourceRenderer = new ModelSourceRenderer(context);
        pluginContext.getFileManifest().writeFile(
                new File(context.getModuleFileDirectory(CppFileType.CPP_INCLUDE), context.getServiceShapeName() + "Model.h").toPath(), modelHeaderRenderer.renderServiceModel());
        pluginContext.getFileManifest().writeFile(
            new File(context.getModuleFileDirectory(CppFileType.CPP_SOURCE), context.getServiceShapeName() + "Model.cpp").toPath(), modelSourceRenderer.renderServiceModel());

        if (pluginContext.getSettings()
                .getBooleanMemberOrDefault("generateClientStubs", Boolean.FALSE).booleanValue()) {
            final ClientHeaderRenderer clientHeaderRenderer = new ClientHeaderRenderer(context);
            final ClientSourceRenderer clientSourceRenderer = new ClientSourceRenderer(context);
            pluginContext.getFileManifest().writeFile(
                    new File(context.getModuleFileDirectory(CppFileType.CPP_INCLUDE), context.getServiceShapeName() + "Client.h").toPath(), clientHeaderRenderer.renderClient());
            pluginContext.getFileManifest().writeFile(
                    new File(context.getModuleFileDirectory(CppFileType.CPP_SOURCE), context.getServiceShapeName() + "Client.cpp").toPath(), clientSourceRenderer.renderClient());
        }
        if (pluginContext.getSettings()
                .getBooleanMemberOrDefault("generateServerStubs", Boolean.FALSE).booleanValue()) {
            throw new UnsupportedOperationException("Server stub generation not implemented yet!");
        }
    }
}
