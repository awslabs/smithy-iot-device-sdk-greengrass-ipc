/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.cpp.model;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.eventstreamrpc.cpp.ServiceCodegenContext;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ModelHeaderRenderer {
    private final ServiceCodegenContext context;

    public ModelHeaderRenderer(final ServiceCodegenContext context) {
        this.context = context;
    }

    /**
     * Output's model contents into a single C++ header file
     * @return
     */
    public String renderServiceModel() {
        try {
            final Configuration configuration = new Configuration(Configuration.VERSION_2_3_30);
            configuration.setEncoding(Locale.getDefault(), StandardCharsets.UTF_8.toString());

            configuration.setClassForTemplateLoading(ModelHeaderRenderer.class,"");
            Template template;
            template = configuration.getTemplate("Model.h.ftl");

            final StringWriter stringWriter = new StringWriter();
            template.process(context.getTemplateInputMap(), stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            throw new CodegenException("Failed to process client Freemarker template: " + e.getMessage(), e);
        }
    }
}
