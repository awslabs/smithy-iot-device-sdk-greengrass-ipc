/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.python;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.CollectionShape;
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
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

        //note: a lot of this functionality probably can be handled in Freemarker but in lacking an understanding
        //      of how to get it to evaluate certain kinds of expressions, putting the logic in Java is far easier
        final Function<String, String> camel_to_python = NameUtils::camelToSnakeLower;
        input.put("fn_camel_to_python", camel_to_python);
        final Function<Shape, OperationShape> shapeToOperation = (Shape shape) -> (OperationShape)shape;
        input.put("fn_to_operation_shape", shapeToOperation);
        final Function<Shape, StructureShape> shapeToStructure = (Shape shape) -> (StructureShape)shape;
        input.put("fn_to_structure_shape", shapeToStructure);
        input.put("fn_shapeId_to_shape",
                (Function<ShapeId, Shape>) (ShapeId shapeId) -> (Shape) model.getShape(shapeId).get());
        final Function<StructureShape, Collection<MemberShape>> structureToMembers = (StructureShape shape) ->
                shape.getAllMembers()
                .values()
                .stream()
                .filter(member -> !model.getShape(member.getTarget()).get().hasTrait(StreamingTrait.class))
                .collect(Collectors.toList());
        input.put("fn_structure_members", structureToMembers);
        final Function<MemberShape, Shape> memberToMemberType =
                (shape) -> model.getShape(shape.getTarget()).orElse(null);
        input.put("fn_member_to_member_type", memberToMemberType);
        final Function<Shape, UnionShape> shapeToUnion = (Shape shape) -> (UnionShape) shape;
        input.put("fn_to_union_shape", shapeToUnion);
        final Function<UnionShape, Collection<MemberShape>> unionToMembers = (UnionShape shape) -> shape.getAllMembers().values();
        input.put("fn_union_members", unionToMembers);

        final Predicate<OperationShape> has_streaming = (operationShape) ->
                operationShape.getInput().isPresent() ?
                        getInputEventStreamInfo(operationShape)
                                .isPresent() : false ||
                operationShape.getOutput().isPresent() ?
                        getOutputEventStreamInfo(
                                operationShape)
                                .isPresent() : false;
        input.put("fn_has_streaming", has_streaming);

        final Function<DataModelObject, Boolean> is_enum = (DataModelObject object) ->
                object.getDataShape().isPresent() && object.getDataShape().get().hasTrait(EnumTrait.class);
        input.put("fn_is_enum", is_enum);
        final Function<DataModelObject, Collection<EnumDefinition>> get_enum_defs = (DataModelObject object) ->
                object.getDataShape().get().getTrait(EnumTrait.class).get().getValues();
        input.put("fn_get_enum_defs", get_enum_defs);

        return input;
    }

    public Set<Shape> selectFromModel(String expression) {
        return Selector.parse(expression).select(model);
    }

    public Shape getShape(final ShapeId shapeId) {
        return model.getShape(shapeId).get();
    }

    public String getOperationResponseSuffix() {
        return "Response";
    }

    public String getOperationRequestSuffix() {
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

    public String getEmptyRequestApplicationType(final OperationShape operationShape) {
        return operationShape.getId().getNamespace() + "#" +
                operationShape.getId().getName() + getOperationRequestSuffix();
    }

    public String getEmptyResponseApplicationType(final OperationShape operationShape) {
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

    /**
     * Note: Below return type MUST be a List type because Python interpreter will encounter
     * types in order and will have errors if types have members of types not yet declared.
     * The implementation is a breadth-first traversal from the operations on down, and thus
     * the last elements must be out put first
     * @return
     */
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
                if (shape.getId().getNamespace().startsWith(PythonCodegenPlugin.SMITHY_NAMESPACE_PREFIX)) {
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

    /**
     * @param shape shape being converted
     * @param var variable name of shape being converted
     * @return String of Python code for converting payload data to modeled data
     */
    // Given modeled python object, return string of python code which converts it to payload object.
    public String getPayloadToShapeCode(Shape shape, String var) {
        switch (shape.getType()) {
            case BOOLEAN:
                return var;

            case DOCUMENT:
                return var;

            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case BIG_INTEGER:
                // json only has "number" type, might be written as 1 or 1.0
                // make sure python int comes back
                return MessageFormat.format("int({0})", var);

            case FLOAT:
            case DOUBLE:
                // json only has "number" type, might serialize 1.0 as 1
                // make sure python float comes back
                return MessageFormat.format("float({0})", var);

            case TIMESTAMP:
                return MessageFormat.format("datetime.datetime.fromtimestamp({0}, datetime.timezone.utc)", var);

            case STRING:
                return var;

            case BLOB:
                return MessageFormat.format("base64.b64decode({0})", var);

            case LIST:
                Shape listMemberShape = shape.asListShape().get().getMember();
                String transformCode = getPayloadToShapeCode(listMemberShape, "i");

                // if we don't need to transform values, just return the original list
                if (transformCode.equals("i")) {
                    return var;
                }

                return MessageFormat.format("[{0} for i in {1}]", transformCode, var);

            case MAP:
                MapShape mapShape = shape.asMapShape().get();

                ShapeId keyTargetShapeId = mapShape.getKey().getTarget();
                Shape keyTargetShape = model.getShape(keyTargetShapeId)
                        .orElseThrow(() -> new CodegenException("Shape not found: " + keyTargetShapeId));
                if (keyTargetShape.getType() != ShapeType.STRING) {
                    throw new CodegenException("Can't make payload from MAP whose keys aren't STRING: " + mapShape);
                }

                String valueTransformExpression = getPayloadToShapeCode(mapShape.getValue(), "v");

                // if we don't need to transform the values, then just return the original dict
                if (valueTransformExpression.equals("v")) {
                    return var;
                }

                return MessageFormat.format("'{'k: {0} for k,v in {1}.items()'}'",
                        valueTransformExpression, var);

            case UNION:
            case STRUCTURE:
                return MessageFormat.format("{0}._from_payload({1})", shape.getId().getName(), var);

            case MEMBER:
                ShapeId targetShapeId = shape.asMemberShape().get().getTarget();
                Shape targetShape = model.getShape(targetShapeId)
                        .orElseThrow(() -> new CodegenException("Shape not found: " + targetShapeId));
                return getPayloadToShapeCode(targetShape, var);

            default:
                throw new CodegenException("Can't make shape from payload: " + shape + " type: " + shape.getType());
        }
    }

    /**
     * @param shape shape being converted
     * @param var variable name of shape being converted
     * @return String of Python code for converting modeled data to payload data
     */
    public String getShapeToPayloadCode(Shape shape, String var) {
        switch (shape.getType()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case DOCUMENT:
                return var;

            case TIMESTAMP:
                return MessageFormat.format("{0}.timestamp()", var);

            case STRING:
                return var;

            case BLOB:
                // transform into Base64 string
                return MessageFormat.format("base64.b64encode({0}).decode()", var);

            case LIST:
                MemberShape listMemberShape = shape.asListShape().get().getMember();
                String transformCode = getShapeToPayloadCode(listMemberShape, "i");

                // if we don't need to transform values, just return the original list
                if (transformCode.equals("i")) {
                    return var;
                }

                return MessageFormat.format("[{0} for i in {1}]", transformCode, var);

            case MAP:
                MapShape mapShape = shape.asMapShape().get();

                ShapeId keyTargetShapeId = mapShape.getKey().getTarget();
                Shape keyTargetShape = model.getShape(mapShape.getKey().getTarget()).orElseThrow(
                        () -> new CodegenException("Shape not found: " + keyTargetShapeId));
                if (keyTargetShape.getType() != ShapeType.STRING) {
                    throw new CodegenException("Can't read payload from MAP whose keys aren't STRING: " + mapShape);
                }

                String valueTransformCode = getShapeToPayloadCode(mapShape.getValue(), "v");

                // if we don't need to transform anything, just return the original dict
                if (valueTransformCode == "v") {
                    return var;
                }

                return MessageFormat.format("'{'k: {0} for k, v in {1}.items()'}'",
                        valueTransformCode, var);

            case UNION:
            case STRUCTURE:
                return MessageFormat.format("{0}._to_payload()", var);

            case MEMBER:
                ShapeId targetShapeId = shape.asMemberShape().get().getTarget();
                Shape targetShape = model.getShape(targetShapeId).orElseThrow(
                        () -> new CodegenException("Shape not found: " + targetShapeId));
                return getShapeToPayloadCode(targetShape, var);

            default:
                throw new CodegenException("Can't make payload from shape: " + shape + " type: " + shape.getType());
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
                name = "bool";
                break;
            case STRING:
                name = "str";
                break;
            case TIMESTAMP:
                name = "datetime.datetime";
                break;
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case BIG_INTEGER:
                name = "int";
                break;
            case FLOAT:
            case DOUBLE:
                name = "float";
                break;
            case LIST:
                name = MessageFormat.format("typing.List[{0}]", getTypeName(shape.asListShape().get().getMember(),
                        packageImportPrefix, bytesUnionStr));
                break;
            case SET:
                name = MessageFormat.format("typing.Set[{0}]", getTypeName(shape.asSetShape().get().getMember(),
                        packageImportPrefix, bytesUnionStr));
                break;
            case MAP:
                MapShape mapShape = shape.asMapShape().get();
                name = MessageFormat.format("typing.Dict[{0}, {1}]",
                        getTypeName(mapShape.getKey(), packageImportPrefix, bytesUnionStr), getTypeName(mapShape.getValue(),
                                packageImportPrefix, bytesUnionStr));
                break;
            case BLOB:
                if (bytesUnionStr) {
                    name = "typing.Union[bytes, str]";
                } else {
                    name = "bytes";
                }
                break;
            case DOCUMENT:
                name = "typing.Dict[str, typing.Any]";
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
            default:
                throw new CodegenException("Cannot assign symbol name to: " + shape);
        }

        return name;
    }
}
