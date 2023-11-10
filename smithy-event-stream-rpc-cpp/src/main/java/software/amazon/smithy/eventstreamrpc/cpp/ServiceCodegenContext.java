/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.eventstreamrpc.cpp;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

import java.io.File;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

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
        input.put("operations", serviceShape.getAllOperations().stream()
                .map(opShapeId -> (OperationShape)getShape(opShapeId)).collect(Collectors.toList()));
        input.put("allShapes", getAllServiceShapes());

        //note: a lot of this functionality probably can be handled in Freemarker but in lacking an understanding
        //      of how to get it to evaluate certain kinds of expressions, putting the logic in Java is far easier
        final Function<String, String> camel_to_pascal = (String opname) -> NameUtils.capitalize(opname);
        input.put("fn_camel_to_pascal", camel_to_pascal);
        final Function<String, String> pascal_to_camel = (String opname) -> NameUtils.uncapitalize(opname);
        input.put("fn_pascal_to_camel", pascal_to_camel);
        final Function<String, String> to_constant_case = (String opname) -> NameUtils.camelToConstantCase(opname);
        input.put("fn_to_constant_case", to_constant_case);
        final BiFunction<String, BigDecimal, String> tab_each_line = (String code, BigDecimal numTabs) -> FormatUtils.tabEachLine(code, numTabs.intValue());
        input.put("fn_tab_each_line", tab_each_line);
        final Function<Shape, OperationShape> shapeToOperation = (Shape shape) -> (OperationShape)shape;
        input.put("fn_to_operation_shape", shapeToOperation);
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

        final Function<Shape, Collection<EnumDefinition>> get_enum_defs_from_shape = (Shape shape) ->
            shape.getTrait(EnumTrait.class).get().getValues();
        input.put("fn_get_enum_defs_from_shape", get_enum_defs_from_shape);

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

    public enum CppFileType {
        CPP_SOURCE,
        CPP_INCLUDE
    }

    public String getModuleFileDirectory(CppFileType cppFileType) {
        String baseDir = pluginContext.getSettings().getStringMemberOrDefault("moduleOverrideDirectory",
                namespaceToFileDirectory(serviceShape.getId().getNamespace()));
        String subDir;
        if(cppFileType == CppFileType.CPP_SOURCE) {
            subDir = pluginContext.getSettings().getStringMemberOrDefault("sourceSubdirectory", "source");
            subDir = new File(subDir).toString();
        } else if(cppFileType == CppFileType.CPP_INCLUDE) {
            subDir = pluginContext.getSettings().getStringMemberOrDefault("includeSubdirectory", new File("include", "aws").toString());
            subDir = new File(subDir, getNamespaceAsCppPath()).toString();
        } else {
            throw new CodegenException("Unknown C++ filetype to generate");
        }
        if (baseDir.isEmpty()) {
            return subDir;
        }
        return new File(baseDir, subDir).toString();
    }

    public String getNamespaceAsCppPath() {
        return namespaceToFileDirectory(serviceShape.getId().getNamespace());
    }

    public Collection<String> getNamespaceAsList() {
        return Arrays.asList(serviceShape.getId().getNamespace().split("\\.")).stream().map(namespace -> NameUtils.capitalize(namespace)).collect( Collectors.toList() );
    }

    public static String namespaceToFileDirectory(String namespace) {
        return namespace.replace('.', '/');
    }

    public Collection<OperationShape> getAllOperations() {
        Comparator<OperationShape> byAlphabetical = Comparator.comparing(operationShape -> {
            return operationShape.getId().getName();
        });
        Supplier<TreeSet<OperationShape>> operations = () -> new TreeSet<OperationShape>(byAlphabetical);
        return serviceShape.getAllOperations().stream().map(id -> (OperationShape)model.getShape(id).get())
                .collect(Collectors.toCollection(operations));
    }

    public String getServiceShapeName() {
        return NameUtils.capitalizeAcronymSubstring(getServiceShape().getId().getName());
    }

    public String getExportName() {
        String serviceName = getServiceShapeName().toUpperCase();

        return "AWS_" + serviceName + "_API";
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
                if (shape.getId().getNamespace().startsWith(CppCodegenPlugin.SMITHY_NAMESPACE_PREFIX)) {
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

    // given modeled C++ object, return string that converts it to a Aws::Crt::JsonObject to be used as a value.
    public String generateSerializerForVar(Shape shape, String fromVar, String toObject) {
        // start with declaration
        String definition = MessageFormat.format("Aws::Crt::JsonObject {0};\n", toObject);
        switch (shape.getType()) {
            case BOOLEAN:
                definition += MessageFormat.format("{1}.AsBool({0});", fromVar, toObject);
                break;
            case BYTE:
                definition += MessageFormat.format("{1}.AsInteger(static_cast<int>({0}));", fromVar, toObject);
                break;
            case SHORT:
                definition += MessageFormat.format("{1}.AsInteger(static_cast<int>({0}));", fromVar, toObject);
                break;
            case INTEGER:
                definition += MessageFormat.format("{1}.AsInteger({0});", fromVar, toObject);
                break;
            case LONG:
                definition += MessageFormat.format("{1}.AsInt64({0});", fromVar, toObject);
                break;
            case FLOAT:
                definition += MessageFormat.format("{1}.AsDouble(static_cast<double>({0}));", fromVar, toObject);
            case DOUBLE:
                definition += MessageFormat.format("{1}.AsDouble({0});", fromVar, toObject);
                break;
            case BIG_INTEGER:
                definition += MessageFormat.format("{1}.AsInt64({0});", fromVar, toObject);
                break;
            case DOCUMENT:
                definition += MessageFormat.format("{1}.AsObject({0});", fromVar, toObject);
                break;
            case TIMESTAMP:
                definition += MessageFormat.format("{1}.AsDouble({0}.SecondsWithMSPrecision());", fromVar, toObject);
                break;
            case STRING:
                definition += MessageFormat.format("{1}.AsString({0});", fromVar, toObject);
                break;
            case BLOB:
                // trying to encode an empty vector as base64 hits an assert so need to check for that
                definition += MessageFormat.format("if ({0}.size() > 0) '{'\n" +
                                    "    {1}.AsString(Aws::Crt::Base64Encode({0}));\n" +
                                    "'}'", fromVar, toObject);
                break;

            case LIST:
                MemberShape listMemberShape = shape.asListShape().get().getMember();
                String camelCaseMemberName = NameUtils.uncapitalize(listMemberShape.getId().getName());
                String childToObject = camelCaseMemberName + "JsonArrayItem";
                String childFromVar = camelCaseMemberName + "Item";
                String transformCode = generateSerializerForVar(listMemberShape, childFromVar, childToObject);
                transformCode = transformCode.replace(" {\n", " '{'\n").replaceAll("(\n| )}", "$1'}'");
                // must iterate create a Aws::Crt::JsonObject from each memberVar and append to list
                definition += MessageFormat.format("Aws::Crt::Vector<Aws::Crt::JsonObject> {1}JsonArray;\n" +
                                            "for (const auto& {2} : {0}) '{'\n" +
                                            FormatUtils.tabEachLine(transformCode, 1) + "\n" +
                                            FormatUtils.spacedTab + "{1}JsonArray.emplace_back(std::move({3}));\n" +
                                            "'}'\n" +
                                            "{1}.AsArray(std::move({1}JsonArray));",
                                            fromVar, toObject, childFromVar, childToObject);
                break;

            case MAP:
                MapShape mapShape = shape.asMapShape().get();

                ShapeId keyTargetShapeId = mapShape.getKey().getTarget();
                Shape keyTargetShape = model.getShape(mapShape.getKey().getTarget()).orElseThrow(
                        () -> new CodegenException("Shape not found: " + keyTargetShapeId));
                ShapeId valueTargetShapeId = mapShape.getValue().getTarget();
                Shape valueTargetShape = model.getShape(mapShape.getValue().getTarget()).orElseThrow(
                        () -> new CodegenException("Shape not found: " + valueTargetShapeId));
                if (keyTargetShape.getType() != ShapeType.STRING) {
                    throw new CodegenException("Can't read payload from MAP whose keys aren't STRING: " + mapShape);
                }

                String keyValueIteratorVar = NameUtils.uncapitalize(mapShape.getKey().getId().getName()) + "Item";
                String valueFromVar = keyValueIteratorVar + ".second";
                String valueToObject = NameUtils.uncapitalize(mapShape.getValue().getId().getName()) + "JsonObject";
                String valueTransformCode = generateSerializerForVar(mapShape.getValue(), valueFromVar, valueToObject);
                valueTransformCode = valueTransformCode.replace(" {\n", " '{'\n").replaceAll("(\n| )}", "$1'}'");
                // not sure why triple quotes are needed here but not elsewhere...
                definition += MessageFormat.format("for (const auto& {2} : {0}) '''{'''\n" +
                                            FormatUtils.tabEachLine(valueTransformCode, 1) + "\n" +
                                            FormatUtils.spacedTab + "{1}.WithObject({2}.first, std::move({3}));\n" +
                                            "'}'",
                                            fromVar, toObject, keyValueIteratorVar, valueToObject);
                //definition += valueTransformCode;
                break;

            case UNION:
            case STRUCTURE:
                definition += MessageFormat.format("{0}.SerializeToJsonObject({1});", fromVar, toObject);
                break;

            case MEMBER:
                ShapeId targetShapeId = shape.asMemberShape().get().getTarget();
                Shape targetShape = model.getShape(targetShapeId).orElseThrow(
                        () -> new CodegenException("Shape not found: " + targetShapeId));
                return generateSerializerForVar(targetShape, fromVar, toObject);

            default:
                throw new CodegenException("Can't make payload from shape: " + shape + " type: " + shape.getType());
        }

        return definition;
    }

    /**
     * @param shape shape being converted
     * @param shapeVar variable name of shape being converted
     * @param payloadVar variable name of the Aws::Crt::JsonObject will be stored
     * @return String of Python code for converting modeled data to payload data
     */
    public String generateSerializerForVarWithKey(Shape shape, String key, String valueFromVar, String toObject) {
        String valueVar = NameUtils.uncapitalize(shape.getId().getName()) + "Value";
        switch (shape.getType()) {
            case BOOLEAN:
                return MessageFormat.format("{2}.WithBool(\"{0}\", {1});", key, valueFromVar, toObject);
            case BYTE:
                return MessageFormat.format("{2}.WithInteger(\"{0}\", static_cast<int>({1}));", key, valueFromVar, toObject);
            case SHORT:
                return MessageFormat.format("{2}.WithInteger(\"{0}\", static_cast<int>({1}));", key, valueFromVar, toObject);
            case INTEGER:
                return MessageFormat.format("{2}.WithInteger(\"{0}\", {1});", key, valueFromVar, toObject);
            case LONG:
                return MessageFormat.format("{2}.WithInt64(\"{0}\", {1});", key, valueFromVar, toObject);
            case FLOAT:
                return MessageFormat.format("{2}.WithDouble(\"{0}\", static_cast<double>({1}));", key, valueFromVar, toObject);
            case DOUBLE:
                return MessageFormat.format("{2}.WithDouble(\"{0}\", {1});", key, valueFromVar, toObject);
            case BIG_INTEGER:
                return MessageFormat.format("{2}.WithInt64(\"{0}\", {1});", key, valueFromVar, toObject);
            case DOCUMENT:
                return MessageFormat.format("{2}.WithObject(\"{0}\", {1});", key, valueFromVar, toObject);
            case TIMESTAMP:
                return MessageFormat.format("{2}.WithDouble(\"{0}\", {1}.SecondsWithMSPrecision());", key, valueFromVar, toObject);
            case STRING:
                return MessageFormat.format("{2}.WithString(\"{0}\", {1});", key, valueFromVar, toObject);
            case BLOB:
                // trying to encode an empty vector as base64 hits an assert so need to check for that
                return MessageFormat.format("if ({1}.size() > 0) '{'\n" +
                                            FormatUtils.spacedTab + "{2}.WithString(\"{0}\", Aws::Crt::Base64Encode({1}));\n" +
                                            "'}'",
                                            key, valueFromVar, toObject);

            case LIST:
                MemberShape listMemberShape = shape.asListShape().get().getMember();
                String itemVar = NameUtils.uncapitalize(listMemberShape.getId().getName());
                String transformCode = generateSerializerForVar(shape, valueFromVar, itemVar);
                transformCode = transformCode.replace(" {\n", " '{'\n").replaceAll("(\n| )}", "$1'}'");

                // must iterate create a Aws::Crt::JsonObject from each memberVar and append to list
                return MessageFormat.format(transformCode + "\n" +
                                            "{2}.WithObject(\"{0}\", std::move({1}));",
                                            key, itemVar, toObject);

            case MAP:
                MapShape mapShape = shape.asMapShape().get();
                String valueTransformCode = generateSerializerForVar(mapShape, valueFromVar, valueVar);

                return MessageFormat.format(valueTransformCode + "\n" +
                                            "{2}.WithObject(\"{0}\", std::move({1}));",
                                            key, valueVar, toObject);

            case UNION:
            case STRUCTURE:
                String structureTransformCode = generateSerializerForVar(shape, valueFromVar, valueVar);

                return MessageFormat.format(structureTransformCode + "\n" +
                                            "{2}.WithObject(\"{0}\", std::move({1}));",
                                            key, valueVar, toObject);

            case MEMBER:
                ShapeId targetShapeId = shape.asMemberShape().get().getTarget();
                Shape targetShape = model.getShape(targetShapeId).orElseThrow(
                        () -> new CodegenException("Shape not found: " + targetShapeId));
                return generateSerializerForVarWithKey(targetShape, key, valueFromVar, toObject);

            default:
                throw new CodegenException("Can't make payload from shape: " + shape + " type: " + shape.getType());
        }
    }

    public String generateDeserializerForMember(Shape shape, String assignToVar, String jsonViewVar)
    {
        String cppTypeName = getTypeName(shape);
        switch (shape.getType()) {
            case BOOLEAN:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>({1}.AsBool());", assignToVar, jsonViewVar, cppTypeName);
            case BYTE:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>(static_cast<int>({1}).AsInteger());", assignToVar, jsonViewVar, cppTypeName);
            case SHORT:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>(static_cast<int>({1}).AsInteger());", assignToVar, jsonViewVar, cppTypeName);
            case INTEGER:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>({1}.AsInteger());", assignToVar, jsonViewVar, cppTypeName);
            case LONG:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>({1}.AsInt64());", assignToVar, jsonViewVar, cppTypeName);
            case FLOAT:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>(static_cast<double>({1}).AsDouble());", assignToVar, jsonViewVar, cppTypeName);
            case DOUBLE:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>({1}.AsDouble());", assignToVar, jsonViewVar, cppTypeName);
            case BIG_INTEGER:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>({1}.AsInt64());", assignToVar, jsonViewVar, cppTypeName);
            case DOCUMENT:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>({1}.AsObject().Materialize());", assignToVar, jsonViewVar, cppTypeName);
            case TIMESTAMP:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>(Aws::Crt::DateTime({1}.AsDouble()));", assignToVar, jsonViewVar, cppTypeName);
            case STRING:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{2}>({1}.AsString());", assignToVar, jsonViewVar, cppTypeName);
            case BLOB:
                // trying to encode an empty vector as base64 hits an assert so need to check for that
                return MessageFormat.format("if ({1}.AsString().size() > 0) '{'\n" +
                                            FormatUtils.spacedTab + "{0} = Aws::Crt::Optional<{2}>(Aws::Crt::Base64Decode({1}.AsString()));\n" +
                                            "'}'",
                                            assignToVar, jsonViewVar, cppTypeName);

            case LIST:
                MemberShape listMemberShape = shape.asListShape().get().getMember();
                String listMemberShapeTypeName = getTypeName(listMemberShape);
                String camelCaseMemberName = NameUtils.uncapitalize(listMemberShape.getId().getName());

                String childJsonViewVar = camelCaseMemberName + "JsonView";
                String childAssignToVar = camelCaseMemberName + "Item";
                String itemAssignCode = generateDeserializerForMember(listMemberShape, childAssignToVar, childJsonViewVar);
                // braces that are escaped must remain escaped since we pass this string to MessageFormat.format(...) again
                itemAssignCode = itemAssignCode.replace(" {\n", " '{'\n").replaceAll("(\n| )}", "$1'}'");

                return MessageFormat.format("{0} = Aws::Crt::Vector<{3}>();\n" +
                                            "for (const Aws::Crt::JsonView& {5} : {1}.AsArray()) '{'\n" +
                                            FormatUtils.spacedTab + "Aws::Crt::Optional<{3}> {4};\n" + // declare
                                            FormatUtils.tabEachLine(itemAssignCode, 1) + "\n" + // assign
                                            FormatUtils.spacedTab + "{0}.push_back({4}.value());\n" + // append
                                            "'}'",
                                            assignToVar, jsonViewVar,
                                            listMemberShapeTypeName, childAssignToVar, childJsonViewVar);

            case MAP:
                MapShape mapShape = shape.asMapShape().get();

                ShapeId keyTargetShapeId = mapShape.getKey().getTarget();
                Shape keyTargetShape = model.getShape(mapShape.getKey().getTarget()).orElseThrow(
                        () -> new CodegenException("Shape not found: " + keyTargetShapeId));
                ShapeId valueTargetShapeId = mapShape.getValue().getTarget();
                Shape valueTargetShape = model.getShape(mapShape.getValue().getTarget()).orElseThrow(
                        () -> new CodegenException("Shape not found: " + valueTargetShapeId));
                if (keyTargetShape.getType() != ShapeType.STRING) {
                    throw new CodegenException("Can't read payload from MAP whose keys aren't STRING: " + mapShape);
                }

                String valueShapeTypeName = getTypeName(valueTargetShape);
                String camelCaseValueName = NameUtils.uncapitalize(mapShape.getValue().getId().getName());

                String jsonViewItemVar = camelCaseValueName + "Pair";
                String valueAssignToVar = camelCaseValueName + "Value";
                String valueJsonViewVar = jsonViewItemVar + ".second";
                String valueAssignCode = generateDeserializerForMember(valueTargetShape, valueAssignToVar, valueJsonViewVar);

                return MessageFormat.format("{0} = Aws::Crt::Map<Aws::Crt::String, {3}>();\n" +
                                            "for (const auto& {5} : {1}.GetAllObjects()) '{'\n" +
                                            FormatUtils.spacedTab + "Aws::Crt::Optional<{3}> {4};\n" + // declare
                                            FormatUtils.tabEachLine(valueAssignCode, 1) + "\n" + // assign
                                            FormatUtils.spacedTab + "{0}[{5}.first] = {4}.value();\n" + // set in map
                                            "'}'",
                                            assignToVar, jsonViewVar,
                                            valueShapeTypeName, valueAssignToVar, jsonViewItemVar);

            case UNION:
            case STRUCTURE:
                return MessageFormat.format("{0} = {2}();\n" +
                                            "{2}::s_loadFromJsonView({0}.value(), {1});",
                                            assignToVar, jsonViewVar, shape.getId().getName());

            case MEMBER:
                ShapeId targetShapeId = shape.asMemberShape().get().getTarget();
                Shape targetShape = model.getShape(targetShapeId).orElseThrow(
                        () -> new CodegenException("Shape not found: " + targetShapeId));
                return generateDeserializerForMember(targetShape, assignToVar, jsonViewVar);

            default:
                throw new CodegenException("Can't make payload from shape: " + shape + " type: " + shape.getType());
        }
    }

    /**
     * @param shape shape being converted
     * @param shapeVar variable name of shape being converted
     * @param payloadVar variable name of the Aws::Crt::JsonObject will be stored
     * @return String of Python code for converting modeled data to payload data
     */
    public String generateDeserializerForMemberWithKey(Shape shape, String assignToVar, String jsonViewVar, String jsonKey) {
        String cppTypeName = getTypeName(shape);
        switch (shape.getType()) {
            case BOOLEAN:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>({1}.GetBool(\"{2}\"));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case BYTE:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>(static_cast<int8_t>({1}.GetInteger(\"{2}\")));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case SHORT:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>(static_cast<int16_t>({1}.GetInteger(\"{2}\")));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case INTEGER:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>({1}.GetInteger(\"{2}\"));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case LONG:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>({1}.GetInt64(\"{2}\"));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case FLOAT:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>(static_cast<float>({1}.GetDouble(\"{2}\")));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case DOUBLE:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>({1}.GetDouble(\"{2}\"));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case BIG_INTEGER:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>({1}.GetInt64(\"{2}\"));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case DOCUMENT:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>({1}.GetJsonObject(\"{2}\").Materialize());", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case TIMESTAMP:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>(Aws::Crt::DateTime({1}.GetDouble(\"{2}\")));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case STRING:
                return MessageFormat.format("{0} = Aws::Crt::Optional<{3}>({1}.GetString(\"{2}\"));", assignToVar, jsonViewVar, jsonKey, cppTypeName);
            case BLOB:
                // trying to encode an empty vector as base64 hits an assert so need to check for that
                return MessageFormat.format("if ({1}.GetString(\"{2}\").size() > 0) '{'\n" +
                                            FormatUtils.spacedTab + "{0} = Aws::Crt::Optional<{3}>(Aws::Crt::Base64Decode({1}.GetString(\"{2}\")));\n" +
                                            "'}'",
                                            assignToVar, jsonViewVar, jsonKey, cppTypeName);

            case LIST:
                MemberShape listMemberShape = shape.asListShape().get().getMember();
                String listMemberShapeTypeName = getTypeName(listMemberShape);
                String camelCaseMemberName = NameUtils.uncapitalize(listMemberShape.getId().getName());

                String childJsonViewVar = camelCaseMemberName + "JsonView";
                String childAssignToVar = camelCaseMemberName + "Item";
                String itemAssignCode = generateDeserializerForMember(listMemberShape, childAssignToVar, childJsonViewVar);
                // braces that are escaped must remain escaped since we pass this string to MessageFormat.format(...) again
                itemAssignCode = itemAssignCode.replace(" {\n", " '{'\n").replaceAll("(\n| )}", "$1'}'");

                // must iterate create a Aws::Crt::JsonObject from each memberVar and append to list
                return MessageFormat.format("{0} = Aws::Crt::Vector<{3}>();\n" +
                                            "for (const Aws::Crt::JsonView& {5} : {1}.GetArray(\"{2}\")) '{'\n" +
                                            FormatUtils.spacedTab + "Aws::Crt::Optional<{3}> {4};\n" + // declare
                                            FormatUtils.tabEachLine(itemAssignCode, 1) + "\n" + // assign
                                            FormatUtils.spacedTab + "{0}.value().push_back({4}.value());\n" + // append
                                            "'}'",
                                            assignToVar, jsonViewVar, jsonKey,
                                            listMemberShapeTypeName, childAssignToVar, childJsonViewVar);

            case MAP:
                MapShape mapShape = shape.asMapShape().get();

                ShapeId keyTargetShapeId = mapShape.getKey().getTarget();
                Shape keyTargetShape = model.getShape(mapShape.getKey().getTarget()).orElseThrow(
                        () -> new CodegenException("Shape not found: " + keyTargetShapeId));
                ShapeId valueTargetShapeId = mapShape.getValue().getTarget();
                Shape valueTargetShape = model.getShape(mapShape.getValue().getTarget()).orElseThrow(
                        () -> new CodegenException("Shape not found: " + valueTargetShapeId));
                if (keyTargetShape.getType() != ShapeType.STRING) {
                    throw new CodegenException("Can't read payload from MAP whose keys aren't STRING: " + mapShape);
                }

                String valueShapeTypeName = getTypeName(valueTargetShape);
                String camelCaseValueName = NameUtils.uncapitalize(mapShape.getValue().getId().getName());

                String jsonViewItemVar = camelCaseValueName + "Pair";
                String valueAssignToVar = camelCaseValueName + "Value";
                String valueJsonViewVar = jsonViewItemVar + ".second";
                String valueAssignCode = generateDeserializerForMember(valueTargetShape, valueAssignToVar, valueJsonViewVar);

                return MessageFormat.format("{0} = Aws::Crt::Map<Aws::Crt::String, {3}>();\n" +
                                            "for (const auto& {5} : {1}.GetJsonObject(\"{2}\").GetAllObjects()) '{'\n" +
                                            FormatUtils.spacedTab + "Aws::Crt::Optional<{3}> {4};\n" + // declare
                                            FormatUtils.tabEachLine(valueAssignCode, 1) + "\n" + // assign
                                            FormatUtils.spacedTab + "{0}.value()[{5}.first] = {4}.value();\n" + // set in map
                                            "'}'",
                                            assignToVar, jsonViewVar, jsonKey,
                                            valueShapeTypeName, valueAssignToVar, jsonViewItemVar);

            case UNION:
            case STRUCTURE:
                return MessageFormat.format("{0} = {3}();\n" +
                                            "{3}::s_loadFromJsonView({0}.value(), {1}.GetJsonObject(\"{2}\"));",
                                            assignToVar, jsonViewVar, jsonKey, shape.getId().getName());

            case MEMBER:
                ShapeId targetShapeId = shape.asMemberShape().get().getTarget();
                Shape targetShape = model.getShape(targetShapeId).orElseThrow(
                        () -> new CodegenException("Shape not found: " + targetShapeId));
                return generateDeserializerForMemberWithKey(targetShape, assignToVar, jsonViewVar, jsonKey);

            default:
                throw new CodegenException("Can't make payload from shape: " + shape + " type: " + shape.getType());
        }
    }

    public String getTypeName(Shape shape) {
        String name;
        switch (shape.getType()) {
            case BOOLEAN:
                name = "bool";
                break;
            case STRING:
                name = "Aws::Crt::String";
                break;
            case TIMESTAMP:
                name = "Aws::Crt::DateTime";
                break;
            case BYTE:
                name = "int8_t";
                break;
            case SHORT:
                name = "int16_t";
                break;
            case INTEGER:
                name = "int";
                break;
            case LONG:
                name = "int64_t";
                break;
            case BIG_INTEGER:
                name = "int64_t";
                break;
            case FLOAT:
                name = "float";
                break;
            case DOUBLE:
                name = "double";
                break;
            case LIST:
                name = MessageFormat.format("Aws::Crt::Vector<{0}>", getTypeName(shape.asListShape().get().getMember()));
                break;
            case SET:
                name = MessageFormat.format("std::set<{0}>", getTypeName(shape.asSetShape().get().getMember()));
                break;
            case MAP:
                MapShape mapShape = shape.asMapShape().get();
                name = MessageFormat.format("Aws::Crt::Map<{0}, {1}>",
                        getTypeName(mapShape.getKey()), getTypeName(mapShape.getValue()));
                break;
            case BLOB:
                name = "Aws::Crt::Vector<uint8_t>";
                break;
            case DOCUMENT:
                name = "Aws::Crt::JsonObject";
                break;
            case STRUCTURE:
            case UNION:
                name = NameUtils.capitalize(shape.getId().getName());
                break;
            case MEMBER:
                ShapeId targetShapeId = shape.asMemberShape().get().getTarget();
                Shape targetShape = model.getShape(targetShapeId)
                        .orElseThrow(() -> new CodegenException("Shape not found: " + targetShapeId));
                return getTypeName(targetShape);
            default:
                throw new CodegenException("Cannot assign symbol name to: " + shape);
        }

        return name;
    }
}
