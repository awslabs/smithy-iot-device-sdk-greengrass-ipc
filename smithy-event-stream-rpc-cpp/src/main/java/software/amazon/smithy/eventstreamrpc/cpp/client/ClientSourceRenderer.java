/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.cpp.client;

import freemarker.template.*;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.eventstreamrpc.cpp.ServiceCodegenContext;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ClientSourceRenderer {
    private final ServiceCodegenContext context;

    public ClientSourceRenderer(final ServiceCodegenContext context) {
        this.context = context;
    }

    /**
     * Output's client into single C++ source file
     * @return
     */
    public String renderClient() {
        try {
            final Configuration configuration = new Configuration(Configuration.VERSION_2_3_30);
            configuration.setEncoding(Locale.getDefault(), StandardCharsets.UTF_8.toString());
            configuration.setClassForTemplateLoading(ClientSourceRenderer.class,"");
            Template template = configuration.getTemplate("Client.cpp.ftl");

            final StringWriter stringWriter = new StringWriter();
            template.process(context.getTemplateInputMap(), stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            throw new CodegenException("Failed to process client Freemarker template: " + e.getMessage(), e);
        }
    }
}
