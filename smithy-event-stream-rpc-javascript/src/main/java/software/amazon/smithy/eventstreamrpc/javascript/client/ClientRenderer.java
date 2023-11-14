/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.javascript.client;

import freemarker.template.*;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.eventstreamrpc.javascript.ServiceCodegenContext;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ClientRenderer {
    private final ServiceCodegenContext context;
    private final String templatePath;

    public ClientRenderer(final ServiceCodegenContext context, final String templatePath) {
        this.context = context;
        this.templatePath = templatePath;
    }

    /**
     * Output's client into single Javascript source file
     * @return
     */
    public String renderClient() {
        try {
            final Configuration configuration = new Configuration(Configuration.VERSION_2_3_30);
            configuration.setEncoding(Locale.getDefault(), StandardCharsets.UTF_8.toString());
            configuration.setClassForTemplateLoading(ClientRenderer.class,"");
            final Template template = configuration.getTemplate(templatePath);

            final StringWriter stringWriter = new StringWriter();
            template.process(context.getTemplateInputMap(), stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            throw new CodegenException("Failed to process client Freemarker template: " + e.getMessage(), e);
        }
    }
}
