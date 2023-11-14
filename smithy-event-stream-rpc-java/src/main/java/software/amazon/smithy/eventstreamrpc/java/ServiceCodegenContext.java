/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.eventstreamrpc.java.model.ClassNameShapeVisitor;
import software.amazon.smithy.eventstreamrpc.java.model.MemberTypeNameShapeVisitor;
import software.amazon.smithy.eventstreamrpc.java.model.TypeNameShapeVisitor;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.EnumTrait;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServiceCodegenContext {
    private static final Logger LOGGER = Logger.getLogger(ServiceCodegenContext.class.getName());

    private final PluginContext pluginContext;
    private final Model model;
    private final TypeNameShapeVisitor typeNameShapeVisitor;
    private final MemberTypeNameShapeVisitor memberTypeNameShapeVisitor;
    private final ClassNameShapeVisitor classNameShapeVisitor;
    private final ServiceShape serviceShape;
    private final EventStreamIndex eventStreamIndex;
    private final TopDownIndex topDownIndex;
    private final Map<ShapeId, ServiceShape> errorToServiceMapping;

    public ServiceCodegenContext(PluginContext pluginContext, Model model, final String serviceName) {
        this.pluginContext = pluginContext;
        this.model = model;
        if (!model.getShape(ShapeId.from(serviceName)).isPresent()) {
            throw new RuntimeException("No service shape ID found for: " + serviceName);
        }
        this.serviceShape = (ServiceShape) model.getShape(ShapeId.from(serviceName)).get();

        typeNameShapeVisitor = new TypeNameShapeVisitor(this, model);
        memberTypeNameShapeVisitor = new MemberTypeNameShapeVisitor(this, model);
        classNameShapeVisitor = new ClassNameShapeVisitor(typeNameShapeVisitor);

        eventStreamIndex = new EventStreamIndex(model);
        topDownIndex = new TopDownIndex(model);
        errorToServiceMapping = new HashMap<>();

        final Collection<OperationShape> missingIOOperations = this.serviceShape.getAllOperations()
                .stream()
                .map(shapeId -> (OperationShape)model.getShape(shapeId).get())
                .filter(opShape -> !opShape.getInput().isPresent() || !opShape.getOutput().isPresent())
                .collect(Collectors.toList());
        missingIOOperations.forEach(opShape -> LOGGER.severe(String.format("Operation {%s} must define both an input shape and output shape.", opShape.getId().toString())));
        if (!missingIOOperations.isEmpty()) {
            throw new RuntimeException("Operations found with no defined input or output shapes!");
        }

        model.shapes().forEach(shape -> {
            if (shape.isServiceShape()) {
                final ServiceShape serviceShape = (ServiceShape) shape;
                topDownIndex.getContainedOperations(serviceShape).stream().forEach(operationShape -> {
                    operationShape.getErrors().stream().forEach(errorShapeId -> {
                        errorToServiceMapping.put(errorShapeId, serviceShape);
                    });
                });
            }
        });
    }

    public String getBaseModelPackage() {
        return getBaseServicePackage() + "." + getModelNamespaceSuffix();
    }

    public String getBaseServicePackage() {
        return getBaseNamespace() + "." + serviceShape.getId().getNamespace();
    }

    public Collection<OperationShape> getAllOperations() {
        final SortedSet<OperationShape> operationShapeSortedSet = new TreeSet<>(
                (OperationShape lhs, OperationShape rhs) -> lhs.getId().toString().compareTo(rhs.getId().toString()));
        serviceShape.getAllOperations().stream().map(id -> (OperationShape) model.getShape(id).get())
                .forEach(shape -> operationShapeSortedSet.add(shape));
        return operationShapeSortedSet;
    }

    public Optional<EventStreamInfo> getInputEventStreamInfo(ToShapeId toShapeId) {
        return eventStreamIndex.getInputInfo(toShapeId);
    }

    public Optional<EventStreamInfo> getOutputEventStreamInfo(ToShapeId toShapeId) {
        return eventStreamIndex.getOutputInfo(toShapeId);
    }

    public Model getModel() {
        return model;
    }

    public TypeName getTypeName(final Shape shape) {
        return shape.accept(typeNameShapeVisitor);
    }

    public TypeName getFieldTypeName(final Shape shape) {
        return shape.accept(memberTypeNameShapeVisitor);
    }

    public ClassName getClassName(final Shape shape) {
        return shape.accept(classNameShapeVisitor);
    }

    public String getGetterPrefix(final Shape shape) {
        if (shape instanceof BooleanShape) {
            return "is";
        }
        return "get";
    }

    public String getSetterPrefix() {
        return "set";
    }

    public String getWitherPrefix() {
        return "with";
    }

    public ShapeId getServiceName() {
        return serviceShape.getId();
    }

    public ServiceShape getServiceShape() {
        return serviceShape;
    }

    public String getOperationResponseSuffix() {
        return "Response";
    }

    public String getOperationRequestSuffix() {
        return "Request";
    }

    public String getBaseServiceErrorSuffix() {
        return "Error";
    }

    public String getBaseNamespace() {
        // TODO: likely remove this default
        return pluginContext.getSettings().getStringMemberOrDefault("javaBasePackage", "software.amazon.awssdk.iot");
    }

    public String getModelNamespaceSuffix() {
        return pluginContext.getSettings().getStringMemberOrDefault("modelRelativePackage", "model");
    }

    public Optional<ServiceShape> getServiceShapeForError(final ShapeId shapeId) {
        if (errorToServiceMapping.containsKey(shapeId)) {
            return Optional.of(errorToServiceMapping.get(shapeId));
        }
        return Optional.empty();
    }

    public ClassName getServiceBaseErrorClassName() {
        return ClassName.get(getBaseModelPackage(), serviceShape.getId().getName() + getBaseServiceErrorSuffix());
    }

    public String getEmptyRequestApplicationType(final OperationShape operationShape) {
        return operationShape.getId().getNamespace() + "#" + operationShape.getId().getName()
                + getOperationRequestSuffix();
    }

    public ClassName getEmptyRequestClassName(final OperationShape operationShape) {
        return ClassName.get(getBaseModelPackage(), operationShape.getId().getName() + getOperationRequestSuffix());
    }

    public ClassName getOperationRequestClassName(final OperationShape operationShape) {
        if (operationShape.getInput().isPresent()) {
            return ClassName.get(getBaseModelPackage(), operationShape.getInput().get().getName());
        } else {
            return getEmptyRequestClassName(operationShape);
        }
    }

    public String getEmptyResponseApplicationType(final OperationShape operationShape) {
        return operationShape.getId().getNamespace() + "#" + operationShape.getId().getName()
                + getOperationResponseSuffix();
    }

    public ClassName getEmptyResponseClassName(OperationShape operationShape) {
        return ClassName.get(getBaseModelPackage(), operationShape.getId().getName() + getOperationResponseSuffix());
    }

    public ClassName getOperationResponseClassName(final OperationShape operationShape) {
        if (operationShape.getOutput().isPresent()) {
            return ClassName.get(getBaseModelPackage(), operationShape.getOutput().get().getName());
        } else {
            return getEmptyResponseClassName(operationShape);
        }
    }

    public ClassName getOperationStreamingRequestClassName(OperationShape operationShape) {
        final Optional<EventStreamInfo> requestStreamInfo = getInputEventStreamInfo(operationShape);
        if (requestStreamInfo.isPresent()) {
            final EventStreamInfo info = requestStreamInfo.get();
            return info.getEventStreamTarget().accept(classNameShapeVisitor);
        } else {
            return PoetryWriter.CN_EVENT_STREAMABLE_JSON_MESSAGE;
        }
    }

    public ClassName getOperationStreamingResponseClassName(OperationShape operationShape) {
        final Optional<EventStreamInfo> responseStreamInfo = getOutputEventStreamInfo(operationShape);
        if (responseStreamInfo.isPresent()) {
            final EventStreamInfo info = responseStreamInfo.get();
            return info.getEventStreamTarget().accept(classNameShapeVisitor);
        } else {
            return PoetryWriter.CN_EVENT_STREAMABLE_JSON_MESSAGE;
        }
    }

    public ClassName getOperationModelContextClassName(OperationShape operationShape) {
        return ClassName.get(getBaseServicePackage(), operationShape.getId().getName() + "OperationContext");
    }

    public ClassName getServiceModelClassName() {
        return ClassName.get(getBaseServicePackage(), serviceShape.getId().getName() + "ServiceModel");
    }

    public String getOperationConstantName(final OperationShape operationShape) {
        return NameUtils.camelToConstantCase(operationShape.getId().getName());
    }

    public Iterator<DataModelObject> getAllShapesIterator() {
        final Set<ShapeId> visitedShapes = new HashSet<>();
        final SortedSet<DataModelObject> dataModelObjects = new TreeSet<>(
                (DataModelObject lhs, DataModelObject rhs) -> lhs.getApplicationModelType().compareTo(rhs.getApplicationModelType()));
        final Collection<Shape> shapesToGenerate = new LinkedList<>();

        // first collect all direct operation inputs, outputs, and errors from the top
        getAllOperations().stream().forEach(operationShape -> {
            if (operationShape.getInput().isPresent()) {
                final ShapeId inputShapeId = operationShape.getInput().get();
                final Shape inputShape = model.getShape(inputShapeId).get();
                shapesToGenerate.add(inputShape);
            } else {
                dataModelObjects.add(new DataModelObject(getEmptyRequestClassName(operationShape), Optional.empty(),
                        getEmptyRequestApplicationType(operationShape)));
            }

            if (operationShape.getOutput().isPresent()) {
                final ShapeId outputShapeId = operationShape.getOutput().get();
                final Shape outputShape = model.getShape(outputShapeId).get();
                shapesToGenerate.add(outputShape);
            } else {
                dataModelObjects.add(new DataModelObject(getEmptyResponseClassName(operationShape), Optional.empty(),
                        getEmptyResponseApplicationType(operationShape)));
            }

            // add all error shapes
            shapesToGenerate.addAll(operationShape.getErrors().stream()
                    .map(errorShapeId -> model.getShape(errorShapeId).get()).collect(Collectors.toList()));

        });

        // now go through shapes to generate
        // sort of an unintentional breadth first search from the service operation
        // input/output/errors and down
        while (!shapesToGenerate.isEmpty()) {
            final Collection<Shape> shapesToAdd = new LinkedList<>();
            shapesToGenerate.forEach(shape -> {
                if (shape.getId().getNamespace().startsWith(JavaCodegenPlugin.SMITHY_NAMESPACE_PREFIX)) {
                    // no need to generate for built in smithy types.
                    return; // continue for the forEach()
                }
                final boolean newShape = visitedShapes.add(shape.getId());

                final Collection<MemberShape> memberShapes = new LinkedList<>();

                if (shape.getType().equals(ShapeType.STRUCTURE)) {
                    if (newShape) {
                        dataModelObjects.add(
                                new DataModelObject(getClassName(shape), Optional.of(shape), shape.getId().toString()));
                    }
                    final StructureShape structureShape = (StructureShape) shape;
                    memberShapes.addAll(structureShape.members());
                } else if (shape.getType().equals(ShapeType.LIST)) {
                    final ListShape listShape = (ListShape) shape;
                    shapesToAdd.add(this.model.getShape(listShape.getMember().getTarget()).get());
                } else if (shape.getType().equals(ShapeType.MAP)) {
                    final MapShape mapShape = (MapShape) shape;
                    shapesToAdd.add(this.model.getShape(mapShape.getKey().getTarget()).get());
                    shapesToAdd.add(this.model.getShape(mapShape.getValue().getTarget()).get());
                   } else if (shape.getType().equals(ShapeType.UNION)) {
                    if (newShape) {
                        dataModelObjects.add(
                                new DataModelObject(getClassName(shape), Optional.of(shape), shape.getId().toString()));
                    }
                    final UnionShape unionShape = (UnionShape) shape;
                    memberShapes.addAll(unionShape.members());
                } else if (shape.getType().equals(ShapeType.STRING) && shape.hasTrait(EnumTrait.class)) {
                    if (newShape) {
                        dataModelObjects.add(
                                new DataModelObject(getClassName(shape), Optional.of(shape), shape.getId().toString()));
                    }
                    // nothing to do, no members
                } else if (shape.getId().getNamespace().equals(getServiceShape().getId().getNamespace())
                        && !(shape instanceof CollectionShape) && !(shape instanceof MapShape)) {
                    // we are concerned about shape definitions with local type names that we aren't
                    // generating
                    // anything for. Collection type aliases, we ignore and just cut through to
                    // their raw
                    // types always. Only their field names are meaningful. If we haven't caught a
                    // type defined
                    // in the model that may need generation otherwise, throw exception
                    throw new CodegenException("No generator for type with shape ID: " + shape.getId().toString());
                }

                memberShapes.forEach(memberShape -> {
                    shapesToAdd.add(model.getShape(memberShape.getTarget()).get());
                });
            });
            shapesToGenerate.clear();
            // add new shapes only if they haven't been visited yet
            shapesToAdd.stream().filter(shape -> !visitedShapes.contains(shape.getId())).forEach(shape -> {
                shapesToGenerate.add(shape);
            });
        }

        return dataModelObjects.iterator();
    }
}
