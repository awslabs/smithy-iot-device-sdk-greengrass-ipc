/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.javascript;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.traits.*;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Far simpler than the Java context as more work is being done in the template
 * rendering and this class is only a light support on top of heavier Smithy
 * selector usage from inside Freemarker templates
 */
public class ServiceCodegenContext {
    private static final Logger LOGGER = Logger.getLogger(ServiceCodegenContext.class.getName());

    private final PluginContext pluginContext;
    private final Model model;
    private final ServiceShape serviceShape;
    private final EventStreamIndex eventStreamIndex;

    public ServiceCodegenContext(PluginContext pluginContext, Model model, final String serviceName) {
        this.pluginContext = pluginContext;
        this.model = model;

        if (!model.getShape(ShapeId.from(serviceName)).isPresent()) {
            throw new RuntimeException("No service shape ID found for: " + serviceName);
        }
        this.serviceShape = (ServiceShape)model.getShape(ShapeId.from(serviceName)).get();
        this.eventStreamIndex = new EventStreamIndex(model);

        final Collection<OperationShape> missingIOOperations = this.serviceShape.getAllOperations()
                .stream()
                .map(shapeId -> (OperationShape)model.getShape(shapeId).get())
                .filter(opShape -> !opShape.getInput().isPresent() || !opShape.getOutput().isPresent())
                .collect(Collectors.toList());
        missingIOOperations.forEach(opShape -> LOGGER.severe(String.format("Operation {%s} must define both an input shape and output shape.", opShape.getId().toString())));
        if (!missingIOOperations.isEmpty()) {
            throw new RuntimeException("Operations found with no defined input or output shapes!");
        }
    }

    public Model getModel() {
        return model;
    }

    public ServiceShape getServiceShape() {
        return serviceShape;
    }

    public Map<String, Object> getTemplateInputMap() {
        final Map<String, Object> input = new HashMap<>();
        input.put("context", this);
        input.put("model", model);
        input.put("serviceShape", serviceShape);

        final SortedSet<OperationShape> operationShapeSortedSet = new TreeSet<>(
                (OperationShape lhs, OperationShape rhs) -> lhs.getId().toString().compareTo(rhs.getId().toString()));
        serviceShape.getAllOperations().stream().map(id -> (OperationShape) model.getShape(id).get())
                .forEach(operationShapeSortedSet::add);
        input.put("operations", operationShapeSortedSet);
        input.put("allShapes", getAllServiceShapes());
        input.put("topLevelOutboundShapes", getTopLevelOutboundShapes());
        input.put("topLevelInboundShapes", getTopLevelInboundShapes());

        //note: a lot of this functionality probably can be handled in Freemarker but in lacking an understanding
        //      of how to get it to evaluate certain kinds of expressions, putting the logic in Java is far easier
        final Function<String, String> to_lower = (String value) -> value.toLowerCase();
        input.put("fn_to_lower", to_lower);
        final Function<String, String> uncapitalize_first = NameUtils::uncapitalize;
        input.put("fn_uncapitalize_first", uncapitalize_first);
        final Function<Shape, StructureShape> shapeToStructure = (Shape shape) -> (StructureShape)shape;
        input.put("fn_to_structure_shape", shapeToStructure);
        final Function<StructureShape, Collection<MemberShape>> structureToMembers = (StructureShape shape) ->
                shape.getAllMembers()
                .values()
                .stream()
                .filter(member -> !model.getShape(member.getTarget()).get().hasTrait(StreamingTrait.class))
                .collect(Collectors.toList());
        input.put("fn_structure_members", structureToMembers);
        final Function<Shape, UnionShape> shapeToUnion = (Shape shape) -> (UnionShape) shape;
        input.put("fn_to_union_shape", shapeToUnion);
        final Function<UnionShape, Collection<MemberShape>> unionToMembers = (UnionShape shape) -> shape.getAllMembers().values();
        input.put("fn_union_members", unionToMembers);

        final Function<DataModelObject, Boolean> is_enum = (DataModelObject object) ->
                object.getDataShape().isPresent() && object.getDataShape().get().hasTrait(EnumTrait.class);
        input.put("fn_is_enum", is_enum);
        final Function<DataModelObject, Collection<EnumDefinition>> get_enum_defs = (DataModelObject object) ->
                object.getDataShape().get().getTrait(EnumTrait.class).get().getValues();
        input.put("fn_get_enum_defs", get_enum_defs);

        return input;
    }

    public Shape getShape(final ShapeId shapeId) {
        return model.getShape(shapeId).get();
    }

    public boolean hasDocumentation(Shape shape) {
        final Optional<DocumentationTrait> docTrait = shape.getTrait(DocumentationTrait.class);
        return docTrait.isPresent();
    }

    public String getDocumentation(Shape shape) {
        final Optional<DocumentationTrait> docTrait = shape.getTrait(DocumentationTrait.class);
        if (docTrait.isPresent()) {
            return docTrait.get().getValue();
        }

        return null;
    }

    public static String getOperationResponseSuffix() {
        return "Response";
    }

    public static String getOperationRequestSuffix() {
        return "Request";
    }

    public String getModuleFileDirectory() {
        String baseDir = pluginContext.getSettings().getStringMemberOrDefault("moduleOverrideDirectory",
                namespaceToFileDirectory(serviceShape.getId().getNamespace()));
        String serviceDir = serviceShape.getId().getName().toLowerCase();
        if (baseDir.isEmpty()) {
            return serviceDir;
        }
        return baseDir + "/" + serviceDir;
    }

    public static String namespaceToFileDirectory(String namespace) {
        return namespace.replace('.', '/');
    }

    public Collection<OperationShape> getAllOperations() {
        return serviceShape.getAllOperations().stream().map(id -> (OperationShape)model.getShape(id).get())
                .collect(Collectors.toList());
    }

    public static String getEmptyRequestApplicationType(final OperationShape operationShape) {
        return operationShape.getId().getNamespace() + "#" +
                operationShape.getId().getName() + getOperationRequestSuffix();
    }

    public static String getEmptyResponseApplicationType(final OperationShape operationShape) {
        return operationShape.getId().getNamespace() + "#" +
                operationShape.getId().getName() + getOperationResponseSuffix();
    }

    public String getEmptyRequestClassName(final OperationShape operationShape) {
        return operationShape.getId().getName() + getOperationRequestSuffix();
    }

    public String getEmptyResponseClassName(OperationShape operationShape) {
        return operationShape.getId().getName() + getOperationResponseSuffix();
    }

    public String getRequestClassName(OperationShape operationShape) {
        return operationShape.getInput().isPresent() ? operationShape.getInput().get().getName() :
                operationShape.getId().getName();
    }

    public String getResponseClassName(OperationShape operationShape) {
        return operationShape.getOutput().isPresent() ? operationShape.getOutput().get().getName() :
                operationShape.getId().getName();
    }

    public String getRequestAppType(OperationShape operationShape) {
        return operationShape.getInput().isPresent() ? operationShape.getInput().get().toString() :
                getEmptyRequestApplicationType(operationShape);
    }

    public String getResponseAppType(OperationShape operationShape) {
        return operationShape.getOutput().isPresent() ? operationShape.getOutput().get().toString() :
                getEmptyResponseApplicationType(operationShape);
    }

    public String getStreamingRequestClassName(OperationShape operationShape) {
        return eventStreamIndex.getInputInfo(operationShape).get().getEventStreamTarget().getId().getName();
    }

    public String getStreamingResponseClassName(OperationShape operationShape) {
        return eventStreamIndex.getOutputInfo(operationShape).get().getEventStreamTarget().getId().getName();
    }

    public String getStreamingRequestAppType(OperationShape operationShape) {
        return eventStreamIndex.getInputInfo(operationShape).get().getEventStreamTarget().getId().toString();
    }

    public String getStreamingResponseAppType(OperationShape operationShape) {
        return eventStreamIndex.getOutputInfo(operationShape).get().getEventStreamTarget().getId().toString();
    }

    public String getClassName(Shape shape) {
        return shape.getId().getName(); //can Smithy name be used directly?
    }

    public Optional<EventStreamInfo> getInputEventStreamInfo(ToShapeId toShapeId) {
        return eventStreamIndex.getInputInfo(toShapeId);
    }

    public Optional<EventStreamInfo> getOutputEventStreamInfo(ToShapeId toShapeId) {
        return eventStreamIndex.getOutputInfo(toShapeId);
    }

    public List<DataModelObject> getTopLevelOutboundShapes() {
        final Set<ShapeId> shapeIds = new HashSet<>();
        final List<DataModelObject> topLevelOutboundShapes = new LinkedList<>();

        getAllOperations().stream().forEach(operationShape -> {
            if (operationShape.getInput().isPresent()) {
                final ShapeId inputShapeId = operationShape.getInput().get();
                final Shape inputShape = model.getShape(inputShapeId).get();
                shapeIds.add(inputShapeId);

                Optional<EventStreamInfo> streamingInputInfo = getInputEventStreamInfo(operationShape);
                if (streamingInputInfo.isPresent()) {
                    final ShapeId streamingInputShapeId = streamingInputInfo.get().getEventStreamTarget().getId();
                    shapeIds.add(streamingInputShapeId);
                }

            } else {
                topLevelOutboundShapes.add(new DataModelObject(getEmptyRequestClassName(operationShape),
                        Optional.empty(), getEmptyRequestApplicationType(operationShape)));
            }
        });

        shapeIds.stream().forEach(shapeId -> {
            final Shape shape = model.getShape(shapeId).get();
            topLevelOutboundShapes.add(new DataModelObject(getClassName(shape), Optional.of(shape), shape.toString()));
        });

        return topLevelOutboundShapes;
    }

    public List<DataModelObject> getTopLevelInboundShapes() {
        final List<DataModelObject> topLevelInboundShapes = new LinkedList<>();
        final Set<ShapeId> shapeIds = new HashSet<ShapeId>();

        getAllOperations().stream().forEach(operationShape -> {

            if (operationShape.getOutput().isPresent()) {
                final ShapeId outputShapeId = operationShape.getOutput().get();
                final Shape outputShape = model.getShape(outputShapeId).get();

                shapeIds.add(outputShapeId);

                Optional<EventStreamInfo> streamingOutputInfo = getOutputEventStreamInfo(operationShape);
                if (streamingOutputInfo.isPresent()) {
                    final ShapeId streamingOutputShapeId = streamingOutputInfo.get().getEventStreamTarget().getId();
                    shapeIds.add(streamingOutputShapeId);
                }
            }
            else {
                topLevelInboundShapes.add(new DataModelObject(getEmptyResponseClassName(operationShape),
                        Optional.empty(), getEmptyResponseApplicationType(operationShape)));
            }

            operationShape.getErrors().stream().forEach(errorShapeId -> shapeIds.add(errorShapeId));
        });

        shapeIds.stream().forEach(shapeId -> {
            final Shape shape = model.getShape(shapeId).get();
            topLevelInboundShapes.add(new DataModelObject(getClassName(shape), Optional.of(shape), shapeId.toString()));
        });

        return topLevelInboundShapes;
    }

    public List<DataModelObject> getAllServiceShapes() {
        final List<DataModelObject> dataModelObjects = new LinkedList<>();  //use a stack to reverse the order of a list
        final List<Shape> shapesToGenerate = new LinkedList<>();

        //first collect all direct operation inputs, outputs, and errors from the top
        getAllOperations().stream().forEach(operationShape -> {
            if (operationShape.getInput().isPresent()) {
                final ShapeId inputShapeId = operationShape.getInput().get();
                final Shape inputShape = model.getShape(inputShapeId).get();
                shapesToGenerate.add(inputShape);
            }
            else {
                dataModelObjects.add(new DataModelObject(getEmptyRequestClassName(operationShape),
                        Optional.empty(), getEmptyRequestApplicationType(operationShape)));
            }

            if (operationShape.getOutput().isPresent()) {
                final ShapeId outputShapeId = operationShape.getOutput().get();
                final Shape outputShape = model.getShape(outputShapeId).get();
                shapesToGenerate.add(outputShape);
            }
            else {
                dataModelObjects.add(new DataModelObject(getEmptyResponseClassName(operationShape),
                        Optional.empty(), getEmptyResponseApplicationType(operationShape)));
            }

            //add all error shapes
            shapesToGenerate.addAll(operationShape.getErrors().stream()
                    .map(errorShapeId -> model.getShape(errorShapeId).get())
                    .collect(Collectors.toList()));
        });

        //now go through shapes to generate
        //sort of an unintentional breadth first search from the service operation input/output/errors and down
        while (!shapesToGenerate.isEmpty()) {
            final List<Shape> shapesToAdd = new LinkedList<>();
            shapesToGenerate.forEach(shape -> {
                if (shape.getId().getNamespace().startsWith(JavascriptCodegenPlugin.SMITHY_NAMESPACE_PREFIX)) {
                    //no need to generate for built in smithy types.
                    return; //continue for the forEach()
                }

                final Collection<MemberShape> memberShapes = new LinkedList<>();

                DataModelObject dataModelObject =
                        new DataModelObject(getClassName(shape), Optional.of(shape), shape.getId().toString());
                if (shape.getType().equals(ShapeType.STRUCTURE)) {
                    dataModelObjects.remove(dataModelObject);
                    dataModelObjects.add(dataModelObject);

                    final StructureShape structureShape = (StructureShape)shape;
                    memberShapes.addAll(structureShape.members());
                } else if (shape.getType().equals(ShapeType.UNION)) {
                    dataModelObjects.remove(dataModelObject);
                    dataModelObjects.add(dataModelObject);

                    final UnionShape unionShape = (UnionShape)shape;
                    memberShapes.addAll(unionShape.members());
                } else if (shape.getType().equals(ShapeType.STRING) && shape.hasTrait(EnumTrait.class)) {
                    dataModelObjects.remove(dataModelObject);
                    dataModelObjects.add(dataModelObject);

                    //nothing to do, no members
                } else if (shape.isListShape() || shape.isSetShape() || shape.isMapShape()) {
                    memberShapes.addAll(shape.members());
                } else if (shape.getId().getNamespace().equals(getServiceShape().getId().getNamespace())
                        && !(shape instanceof CollectionShape) &&  !(shape instanceof MapShape)) {
                    //we are concerned about shape definitions with local type names that we aren't generating
                    //anything for. Collection type aliases, we ignore and just cut through to their raw
                    //types always. Only their field names are meaningful. If we haven't caught a type defined
                    //in the model that may need generation otherwise, throw exception
                    throw new CodegenException("No generator for type with shape ID: " + shape.getId().toString());
                }

                memberShapes.forEach(memberShape -> {
                    shapesToAdd.add(model.getShape(memberShape.getTarget()).get());
                });
            });
            shapesToGenerate.clear();
            shapesToGenerate.addAll(shapesToAdd);
        }

        Collections.reverse(dataModelObjects);
        return dataModelObjects;
    }

    public boolean shouldDeserializeMember(MemberShape memberShape) {
        Shape targetShape = model.getShape((memberShape).getTarget()).get();

        switch (targetShape.getType()) {
            case TIMESTAMP:
            case BLOB:
                return true;

            case LIST: {
                ListShape listShape = targetShape.asListShape().get();
                MemberShape entryShape = listShape.getMember();

                return shouldDeserializeMember(entryShape);
            }

            case SET: {
                throw new CodegenException("Javascript codegen does not yet support Set-valued fields");
            }

            case MAP:
                MapShape mapShape = targetShape.asMapShape().get();
                MemberShape keyShape = mapShape.getKey();
                MemberShape valueShape = mapShape.getValue();
                return shouldDeserializeMember(keyShape) || shouldDeserializeMember(valueShape);

            case STRUCTURE:
            case UNION:
                return true; // we could check recursively but it's not critical if we're overly cautious

            case BIG_INTEGER:
                throw new CodegenException("Javascript codegen does not yet support big integer fields");

            default:
                return false;
        }
    }

    private String getFunctionOrUndefined(String function) {
        return (function != null) ? function : "undefined";
    }

    private String getDeserializerFunctionObject(MemberShape memberShape) {
        String functionName = null;
        Shape targetShape = model.getShape((memberShape).getTarget()).get();

        switch (targetShape.getType()) {
            case TIMESTAMP:
                functionName = "eventstream_rpc_utils.transformNumberAsDate";
                break;

            case BLOB:
                functionName = "eventstream_rpc_utils.transformStringAsPayload";
                break;

            case LIST: {
                ListShape listShape = targetShape.asListShape().get();
                MemberShape entryShape = listShape.getMember();
                String entryDeserializerFunction = getDeserializerFunctionObject(entryShape);
                functionName = String.format("(listValue) => { eventstream_rpc_utils.deserializeList(listValue, %s) ;}", entryDeserializerFunction);
                break;
            }

            case SET: {
                throw new CodegenException("Javascript codegen does not yet support Set-valued fields");
            }

            case MAP: {
                MapShape mapShape = targetShape.asMapShape().get();
                MemberShape keyShape = mapShape.getKey();
                MemberShape valueShape = mapShape.getValue();
                String keyDeserializerFunction = getDeserializerFunctionObject(keyShape);
                String valueDeserializerFunction = getDeserializerFunctionObject(valueShape);
                functionName = String.format("(mapValue) => { eventstream_rpc_utils.deserializeMapAsObject(mapValue, %s) ;}", getFunctionOrUndefined(keyDeserializerFunction), getFunctionOrUndefined(valueDeserializerFunction));
                break;
            }

            case STRUCTURE:
            case UNION:
                functionName = String.format("deserialize%s", getClassName(targetShape));
                break;

            case BIG_INTEGER:
                throw new CodegenException("Javascript codegen does not yet support big integer fields");

            default:
                break;
        }

        return getFunctionOrUndefined(functionName);
    }

    public String getDeserializeLine(MemberShape memberShape) {
        String memberName = memberShape.getMemberName();
        Shape targetShape = model.getShape((memberShape).getTarget()).get();

        switch (targetShape.getType()) {
            case TIMESTAMP:
                return String.format("eventstream_rpc_utils.setDefinedProperty(value, '%s', value.%s, eventstream_rpc_utils.transformNumberAsDate);", memberName, memberName);

            case BLOB:
                return String.format("eventstream_rpc_utils.setDefinedProperty(value, '%s', value.%s, eventstream_rpc_utils.transformStringAsPayload);", memberName, memberName);

            case LIST: {
                ListShape listShape = targetShape.asListShape().get();
                MemberShape entryShape = listShape.getMember();
                String entryDeserializerFunction = getDeserializerFunctionObject(entryShape);
                return String.format("eventstream_rpc_utils.setDefinedArrayProperty(value, '%s', value.%s, %s);", memberName, memberName, entryDeserializerFunction);
            }

            case SET: {
                throw new CodegenException("Javascript codegen does not yet support Set-valued fields");
            }

            case MAP:
                MapShape mapShape = targetShape.asMapShape().get();
                MemberShape keyShape = mapShape.getKey();
                MemberShape valueShape = mapShape.getValue();
                String keyDeserializerFunction = getDeserializerFunctionObject(keyShape);
                String valueDeserializerFunction = getDeserializerFunctionObject(valueShape);
                return String.format("eventstream_rpc_utils.setDefinedObjectPropertyAsMap(value, '%s', value.%s, %s, %s);", memberName, memberName, keyDeserializerFunction, valueDeserializerFunction);

            case STRUCTURE:
            case UNION:
                return String.format("eventstream_rpc_utils.setDefinedProperty(value, '%s', value.%s, %s);", memberName, memberName, getDeserializerFunctionObject(memberShape));

            case BIG_INTEGER:
                throw new CodegenException("Javascript codegen does not yet support big integer fields");

            default:
                throw new CodegenException("Could not compute deserialization line for member shape :" + memberShape.toString());
        }
    }

    private String getNormalizerFunctionObject(Shape shape) {
        switch (shape.getType()) {
            case TIMESTAMP:
                return "eventstream_rpc_utils.encodeDateAsNumber";

            case BLOB:
                return "eventstream_rpc_utils.encodePayloadAsString";

            case LIST: {
                ListShape listShape = shape.asListShape().get();
                MemberShape entryShape = listShape.getMember();
                String entryNormalizer = getNormalizerFunctionObject(entryShape);
                if (entryNormalizer != null) {
                    return String.format("(arrayValue) => { return eventstream_rpc_utils.normalizeArrayValue(arrayValue, %s); }", entryNormalizer);
                } else {
                    return null;
                }
            }

            case SET: {
                throw new CodegenException("Javascript codegen does not yet support Sets as interior values");
            }

            case MAP: {
                MapShape mapShape = shape.asMapShape().get();
                MemberShape keyShape = mapShape.getKey();
                MemberShape valueShape = mapShape.getValue();
                String keyNormalizer = getNormalizerFunctionObject(keyShape);
                String valueNormalizer = getNormalizerFunctionObject(valueShape);
                return String.format("(mapValue) => { return eventstream_rpc_utils.normalizeMapValueAsObject(mapValue, %s, %s); }",
                        getFunctionOrUndefined(keyNormalizer), getFunctionOrUndefined(valueNormalizer));
            }

            case STRUCTURE:
            case UNION:
                return String.format("normalize%s", getClassName(shape));

            case MEMBER:
                return getNormalizerFunctionObject(model.getShape(((MemberShape)shape).getTarget()).get());

            case BIG_INTEGER:
                throw new CodegenException("Javascript codegen does not yet support big integer fields");

            default:
                return null;
        }
    }

    public String getNormalizerMemberLine(Shape shape) {
        String memberName = ((MemberShape)shape).getMemberName();
        Shape memberShape = model.getShape(((MemberShape)shape).getTarget()).get();
        switch (memberShape.getType()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case FLOAT:
            case DOUBLE:
            case LONG:
            case BOOLEAN:
            case STRING:
            case DOCUMENT:
                return String.format("eventstream_rpc_utils.setDefinedProperty(normalizedValue, '%s', value.%s);", memberName, memberName);

            case TIMESTAMP:
                return String.format("eventstream_rpc_utils.setDefinedProperty(normalizedValue, '%s', value.%s, eventstream_rpc_utils.encodeDateAsNumber);", memberName, memberName);

            case BLOB:
                return String.format("eventstream_rpc_utils.setDefinedProperty(normalizedValue, '%s', value.%s, eventstream_rpc_utils.encodePayloadAsString);", memberName, memberName);

            case LIST: {
                ListShape listShape = memberShape.asListShape().get();
                MemberShape entryShape = listShape.getMember();
                String entryNormalizerFunction = getNormalizerFunctionObject(entryShape);

                return String.format("eventstream_rpc_utils.setDefinedArrayProperty(normalizedValue, '%s', value.%s, %s);", memberName, memberName, getFunctionOrUndefined(entryNormalizerFunction));
            }

            case SET: {
                throw new CodegenException("Javascript codegen does not yet support Set-valued fields");
            }

            case MAP:
                MapShape mapShape = memberShape.asMapShape().get();
                MemberShape keyShape = mapShape.getKey();
                MemberShape valueShape = mapShape.getValue();
                String keyNormalizerFunction = getNormalizerFunctionObject(keyShape);
                String valueNormalizerFunction = getNormalizerFunctionObject(valueShape);

                return String.format("eventstream_rpc_utils.setDefinedMapPropertyAsObject(normalizedValue, '%s', value.%s, %s, %s);", memberName, memberName,
                        getFunctionOrUndefined(keyNormalizerFunction), getFunctionOrUndefined(valueNormalizerFunction));

            case STRUCTURE:
            case UNION:
                return String.format("eventstream_rpc_utils.setDefinedProperty(normalizedValue, '%s', value.%s, %s);", memberName, memberName, getNormalizerFunctionObject(memberShape));

            case BIG_INTEGER:
                throw new CodegenException("Javascript codegen does not yet support big integer fields");

            default:
                throw new CodegenException("Javascript codegen does not know how to normalize: " + shape);
        }
    }

    public String getValidationFunctionObject(Shape shape) {
        switch (shape.getType()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                // TODO: Integer range validation
                return "eventstream_rpc_utils.validateValueAsInteger";

            case FLOAT:
            case DOUBLE:
                return "eventstream_rpc_utils.validateValueAsNumber";

            case STRING:
                return "eventstream_rpc_utils.validateValueAsString";

            case TIMESTAMP:
                return "eventstream_rpc_utils.validateValueAsDate";

            case BLOB:
                return "eventstream_rpc_utils.validateValueAsBlob";

            case DOCUMENT:
                return "eventstream_rpc_utils.validateValueAsAny";

            case LIST: {
                ListShape listShape = shape.asListShape().get();
                MemberShape entryShape = listShape.getMember();
                String entryValidator = getValidationFunctionObject(entryShape);
                if (entryValidator != null) {
                    return String.format("(arrayValue) => { eventstream_rpc_utils.validateValueAsArray(arrayValue, %s); }", entryValidator);
                } else {
                    throw new CodegenException("Could not determine validation function for: " + entryShape.toString());
                }
            }

            case SET: {
                throw new CodegenException("Javascript codegen does not yet support Sets as interior values");
            }

            case MAP: {
                MapShape mapShape = shape.asMapShape().get();
                MemberShape keyShape = mapShape.getKey();
                MemberShape valueShape = mapShape.getValue();
                String keyValidator = getValidationFunctionObject(keyShape);
                String valueValidator = getValidationFunctionObject(valueShape);
                return String.format("(mapValue) => { return eventstream_rpc_utils.validateValueAsMap(mapValue, %s, %s); }",
                        getFunctionOrUndefined(keyValidator), getFunctionOrUndefined(valueValidator));
            }

            case STRUCTURE:
            case UNION:
                return String.format("validate%s", getClassName(shape));

            case MEMBER:
                return getValidationFunctionObject(model.getShape(((MemberShape)shape).getTarget()).get());

            case BIG_INTEGER:
                throw new CodegenException("Javascript codegen does not yet support big integer fields");

            default:
                throw new CodegenException("Unable to determinate validator for:" + shape.toString());
        }
    }

    public String getValidateMemberLine(MemberShape shape, String parentShapeName) {
        String memberName = shape.getMemberName();
        Shape targetShape = model.getShape(shape.getTarget()).get();
        String optionalTag = "Optional";
        if (shape.hasTrait(RequiredTrait.class)) {
            optionalTag = "";
        }

        switch (targetShape.getType()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                // TODO: Integer range validation
                return String.format("eventstream_rpc_utils.validateValueAs%sInteger(value.%s, '%s', '%s');", optionalTag, memberName, memberName, parentShapeName);

            case FLOAT:
            case DOUBLE:
                return String.format("eventstream_rpc_utils.validateValueAs%sNumber(value.%s, '%s', '%s');", optionalTag, memberName, memberName, parentShapeName);

            case BOOLEAN:
                return String.format("eventstream_rpc_utils.validateValueAs%sBoolean(value.%s, '%s', '%s');", optionalTag, memberName, memberName, parentShapeName);

            case STRING:
                // Both strings and enums are checked here.  Enum values are intentionally not validated.
                return String.format("eventstream_rpc_utils.validateValueAs%sString(value.%s, '%s', '%s');", optionalTag, memberName, memberName, parentShapeName);

            case DOCUMENT:
                return String.format("eventstream_rpc_utils.validateValueAs%sAny(value.%s, '%s', '%s');", optionalTag, memberName, memberName, parentShapeName);

            case TIMESTAMP:
                return String.format("eventstream_rpc_utils.validateValueAs%sDate(value.%s, '%s', '%s');", optionalTag, memberName, memberName, parentShapeName);

            case BLOB:
                return String.format("eventstream_rpc_utils.validateValueAs%sBlob(value.%s, '%s', '%s');", optionalTag, memberName, memberName, parentShapeName);

            case LIST: {
                ListShape listShape = targetShape.asListShape().get();
                MemberShape entryShape = listShape.getMember();
                String entryValidationFunction = getValidationFunctionObject(entryShape);

                return String.format("eventstream_rpc_utils.validateValueAs%sArray(value.%s, %s, '%s', '%s');", optionalTag, memberName, entryValidationFunction, memberName, parentShapeName);
            }

            case SET: {
                throw new CodegenException("Javascript codegen does not yet support Set-valued fields");
            }

            case MAP:
                MapShape mapShape = targetShape.asMapShape().get();
                MemberShape keyShape = mapShape.getKey();
                MemberShape valueShape = mapShape.getValue();
                String keyValidationFunction = getValidationFunctionObject(keyShape);
                String valueValidationFunction = getValidationFunctionObject(valueShape);

                return String.format("eventstream_rpc_utils.validateValueAs%sMap(value.%s, %s, %s, '%s', '%s');", optionalTag, memberName, keyValidationFunction, valueValidationFunction, memberName, parentShapeName);

            case STRUCTURE:
                return String.format("eventstream_rpc_utils.validateValueAs%sObject(value.%s, %s, '%s', '%s');", optionalTag, memberName, getValidationFunctionObject(targetShape), memberName, parentShapeName);

            case UNION:
                return String.format("eventstream_rpc_utils.validateValueAsUnion(value.%s, _%sPropertyValidators);", memberName, getClassName(targetShape));

            case BIG_INTEGER:
                throw new CodegenException("Javascript codegen does not yet support big integer fields");

            default:
                throw new CodegenException("Cannot determine validation line for: " + shape);
        }
    }

    public String getTypeName(Shape shape) {
        return getTypeName(shape, "");
    }

    public String getTypeName(Shape shape, String packageImportPrefix) {
        return getTypeName(shape, packageImportPrefix, true);
    }

    public String getTypeName(Shape shape, String packageImportPrefix, boolean bytesUnionStr) {
        String name;
        switch (shape.getType()) {
            case BOOLEAN:
                name = "boolean";
                break;
            case STRING:
                if (shape.hasTrait(EnumTrait.class)) {
                    name = getClassName(shape);
                } else {
                    name = "string";
                }
                break;
            case TIMESTAMP:
                name = "Date";
                break;
            case BYTE:
            case SHORT:
            case INTEGER:
            case FLOAT:
            case DOUBLE:
            case LONG: // TODO: this should be a bigint
                name = "number";
                break;
            case LIST:
                name = MessageFormat.format("{0}[]", getTypeName(shape.asListShape().get().getMember(),
                        packageImportPrefix, bytesUnionStr));
                break;
            case SET:
                name = MessageFormat.format("Set<{0}>", getTypeName(shape.asSetShape().get().getMember(),
                        packageImportPrefix, bytesUnionStr));
                break;
            case MAP:
                MapShape mapShape = shape.asMapShape().get();
                name = MessageFormat.format("Map<{0}, {1}>",
                        getTypeName(mapShape.getKey(), packageImportPrefix, bytesUnionStr), getTypeName(mapShape.getValue(),
                                packageImportPrefix, bytesUnionStr));
                break;
            case BLOB:
                name = "eventstream.Payload";
                break;
            case DOCUMENT:
                name = "any";
                break;
            case STRUCTURE:
            case UNION:
                name = NameUtils.capitalize(shape.getId().getName());
                if (packageImportPrefix != null) {
                    name = packageImportPrefix + name;
                }
                break;
            case MEMBER:
                ShapeId targetShapeId = shape.asMemberShape().get().getTarget();
                Shape targetShape = model.getShape(targetShapeId)
                        .orElseThrow(() -> new CodegenException("Shape not found: " + targetShapeId));
                return getTypeName(targetShape, packageImportPrefix, bytesUnionStr);
            case BIG_INTEGER:
                throw new CodegenException("Javascript codegen does not yet support big integer fields");
            default:
                throw new CodegenException("Cannot assign symbol name to: " + shape);
        }

        return name;
    }
}
