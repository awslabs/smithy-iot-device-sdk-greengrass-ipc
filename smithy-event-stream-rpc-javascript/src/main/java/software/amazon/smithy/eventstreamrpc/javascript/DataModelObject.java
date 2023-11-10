/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.javascript;

import software.amazon.smithy.model.shapes.Shape;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a shape that exists in the service that may be real or implicit.
 *
 * A real shape is explicitly defined in the Smithy service model, and an implicit
 * one is automatically generated for an input or output for an operation that does
 * not have an explicitly defined shaped.
 */
public class DataModelObject {
    private final String className;
    private final Optional<Shape> dataShape;
    private final String applicationModelType;

    public DataModelObject(String className, Optional<Shape> dataShape, String applicationModelType) {
        this.className = className;
        this.dataShape = dataShape;
        this.applicationModelType = applicationModelType;
    }

    public String getClassName() {
        return className;
    }

    public Optional<Shape> getDataShape() {
        return dataShape;
    }

    public String getApplicationModelType() {
        return applicationModelType;
    }

    //below implementations were added when at one point

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs == null) return false;
        if (!(rhs instanceof DataModelObject)) return false;
        final DataModelObject other = (DataModelObject)rhs;
        return Objects.equals(className, other.className);
    }
}
