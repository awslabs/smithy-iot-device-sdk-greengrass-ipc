/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.java.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import software.amazon.smithy.eventstreamrpc.java.ServiceCodegenContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.StringShape;

public class MemberTypeNameShapeVisitor extends TypeNameShapeVisitor {
    public MemberTypeNameShapeVisitor(ServiceCodegenContext context, Model model) {
        super(context, model);
    }

    @Override
    public TypeName stringShape(StringShape shape) {
        return ClassName.get(java.lang.String.class);
    }
}
