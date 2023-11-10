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

public class ClientHeaderRenderer {
    private final ServiceCodegenContext context;

    public ClientHeaderRenderer(final ServiceCodegenContext context) {
        this.context = context;
    }

    /**
     * Output's client into single C++ header file
     * @return
     */
    public String renderClient() {
        try {
            final Configuration configuration = new Configuration(Configuration.VERSION_2_3_30);
            configuration.setEncoding(Locale.getDefault(), StandardCharsets.UTF_8.toString());
            configuration.setClassForTemplateLoading(ClientHeaderRenderer.class,"");
            Template template = configuration.getTemplate("Client.h.ftl");

            final StringWriter stringWriter = new StringWriter();
            template.process(context.getTemplateInputMap(), stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            throw new CodegenException("Failed to process client Freemarker template: " + e.getMessage(), e);
        }
    }
}
