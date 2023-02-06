package ru.yandex.market.mbo.cms.core.json.utilites.schema;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.JsonSchemaParsed;
import ru.yandex.market.mbo.cms.core.models.NodeType;
import ru.yandex.market.mbo.cms.core.models.property.Property;

public class FlowSchemaParserTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String flowSchema;

    private FlowSchemaParser flowSchemaParser = new FlowSchemaParser();

    protected String getFileContents(String name) {
        try {
            URL url = this.getClass().getClassLoader().getResource(
                    "mbo-cms-core/assets/json_schema_validator/" + name
            );
            return new String(Files.readAllBytes(Paths.get(url.toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        flowSchema = getFileContents("flow_schema.json");
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void parse() {
        Map<String, String> acceptedTypes = new HashMap<>();
        acceptedTypes.put("#/definitions/Widget", "Widget");
        acceptedTypes.put("#/definitions/Box", "Box");
        JsonSchemaParsed result = flowSchemaParser.parse(flowSchema, acceptedTypes);
        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.getNodeTypes().size());
    }

    @Test
    public void testProcessEnumSimple() throws Exception {
        String path = "";
        String v1 = "abc";
        String v2 = "xyz";
        JsonNode enumValue = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(Arrays.asList(v1, v2)));

        FlowSchemaParser.SchemaNodeParsed result = new FlowSchemaParser.SchemaNodeParsed();
        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        Map<String, String> typeMappings = new HashMap<>();

        context.typeMappings = typeMappings;

        flowSchemaParser.processEnum(path, enumValue, result, context);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.properties.size());
        Assert.assertEquals(FlowSchemaParser.STRING, result.valueType);
        Assert.assertTrue(result.properties.containsKey(Property.OPTIONS.getName()));
        Assert.assertNotNull(result.properties.get(Property.OPTIONS.getName()));
        Assert.assertEquals(2, result.properties.get(Property.OPTIONS.getName()).size());
        Assert.assertEquals(v1, result.properties.get(Property.OPTIONS.getName()).get(0));
        Assert.assertEquals(v2, result.properties.get(Property.OPTIONS.getName()).get(1));

        Assert.assertTrue(result.properties.containsKey(Property.TYPE.getName()));
        Assert.assertNotNull(result.properties.get(Property.TYPE.getName()));
        Assert.assertEquals(1, result.properties.get(Property.TYPE.getName()).size());
        Assert.assertEquals(Constants.UI_CONTROL_LIST_BOX, result.properties.get(Property.TYPE.getName()).get(0));
        Assert.assertNotNull(context.result.getErrors());
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testProcessEnumNumber() throws Exception {
        String path = "";
        int v1 = 100;
        int v2 = 200;
        JsonNode enumValue = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(Arrays.asList(v1, v2)));

        FlowSchemaParser.SchemaNodeParsed result = new FlowSchemaParser.SchemaNodeParsed();
        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        Map<String, String> typeMappings = new HashMap<>();

        context.typeMappings = typeMappings;

        flowSchemaParser.processEnum(path, enumValue, result, context);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.properties.size());
        Assert.assertEquals(FlowSchemaParser.NUMBER, result.valueType);
        Assert.assertTrue(result.properties.containsKey(Property.OPTIONS.getName()));
        Assert.assertNotNull(result.properties.get(Property.OPTIONS.getName()));
        Assert.assertEquals(2, result.properties.get(Property.OPTIONS.getName()).size());
        Assert.assertEquals(String.valueOf(v1), result.properties.get(Property.OPTIONS.getName()).get(0));
        Assert.assertEquals(String.valueOf(v2), result.properties.get(Property.OPTIONS.getName()).get(1));

        Assert.assertTrue(result.properties.containsKey(Property.TYPE.getName()));
        Assert.assertNotNull(result.properties.get(Property.TYPE.getName()));
        Assert.assertEquals(1, result.properties.get(Property.TYPE.getName()).size());
        Assert.assertEquals(Constants.UI_CONTROL_LIST_BOX, result.properties.get(Property.TYPE.getName()).get(0));
        Assert.assertNotNull(context.result.getErrors());
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testProcessEnumLabels() throws Exception {
        String path = "";
        String v1 = "abc";
        String l1 = "абв";
        String v2 = "xyz";
        String l2 = "эюя";
        String v3 = "def";
        JsonNode enumValue = OBJECT_MAPPER.readTree(
                OBJECT_MAPPER.writeValueAsString(Arrays.asList(
                        makeMap(v1, l1),
                        makeMap(v2, l2),
                        v3
                ))
        );

        FlowSchemaParser.SchemaNodeParsed result = new FlowSchemaParser.SchemaNodeParsed();
        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        Map<String, String> typeMappings = new HashMap<>();

        context.typeMappings = typeMappings;

        flowSchemaParser.processEnum(path, enumValue, result, context);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.properties.containsKey(Property.OPTIONS.getName()));
        Assert.assertNotNull(result.properties.get(Property.OPTIONS.getName()));
        Assert.assertEquals(3, result.properties.get(Property.OPTIONS.getName()).size());
        Assert.assertEquals(v1 + Constants.OPTION_LABEL_DELIMITER + l1,
                result.properties.get(Property.OPTIONS.getName()).get(0));
        Assert.assertEquals(v2 + Constants.OPTION_LABEL_DELIMITER + l2,
                result.properties.get(Property.OPTIONS.getName()).get(1));
        Assert.assertEquals(v3, result.properties.get(Property.OPTIONS.getName()).get(2));

        Assert.assertTrue(result.properties.containsKey(Property.TYPE.getName()));
        Assert.assertNotNull(result.properties.get(Property.TYPE.getName()));
        Assert.assertEquals(1, result.properties.get(Property.TYPE.getName()).size());
        Assert.assertEquals(Constants.UI_CONTROL_LIST_BOX, result.properties.get(Property.TYPE.getName()).get(0));
    }

    private static <T> Map<String, T> makeMap(T v, T l) {
        HashMap<String, T> result = new HashMap<>();

        result.put(FlowSchemaParser.CONST, v);
        result.put(FlowSchemaParser.TITLE, l);

        return result;
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testProcessProperties() throws Exception {
        String path = "";
        String f1 = "abc";
        String p1 = "cmsEdf";
        String p1Internal = "xyz";
        List<String> p1v = Arrays.asList("v1", "v2");
        Map<String, Object> cmsProperties = new HashMap<>();
        cmsProperties.put(f1, makeField(p1, p1v));
        JsonNode properties = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(cmsProperties));
        JsonNode required = null;
        NodeType nodeType = new NodeType();

        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        Map<String, String> typeMappings = new HashMap<>();

        context.typeMappings = typeMappings;
        context.propertyMappings = Collections.singletonMap(p1, p1Internal);

        flowSchemaParser.processProperties(path, properties, required, nodeType, context);
        flowSchemaParser.postProcessFields(context);

        Assert.assertEquals(1, nodeType.getFields().size());
        Assert.assertTrue(nodeType.getFieldsNames().contains(f1));
        Assert.assertNotNull(nodeType.getField(f1));
        Assert.assertFalse(nodeType.getField(f1).getProperties().containsKey(p1));
        Assert.assertTrue(nodeType.getField(f1).getProperties().containsKey(p1Internal));
        Assert.assertEquals(p1v, nodeType.getField(f1).getProperties().get(p1Internal));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testProcessPropertiesSingle() throws Exception {
        String path = "";
        String f1 = "abc";
        String p1 = "cmsEdf";
        String p1Internal = "xyz";
        String p1v = "v1";
        Map<String, Object> cmsProperties = new HashMap<>();
        cmsProperties.put(f1, makeField(p1, p1v));
        JsonNode properties = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(cmsProperties));
        JsonNode required = null;
        NodeType nodeType = new NodeType();

        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        Map<String, String> typeMappings = new HashMap<>();

        context.typeMappings = typeMappings;
        context.propertyMappings = Collections.singletonMap(p1, p1Internal);

        flowSchemaParser.processProperties(path, properties, required, nodeType, context);
        flowSchemaParser.postProcessFields(context);

        Assert.assertEquals(1, nodeType.getFields().size());
        Assert.assertTrue(nodeType.getFieldsNames().contains(f1));
        Assert.assertNotNull(nodeType.getField(f1));
        Assert.assertFalse(nodeType.getField(f1).getProperties().containsKey(p1));
        Assert.assertTrue(nodeType.getField(f1).getProperties().containsKey(p1Internal));
        Assert.assertEquals(Arrays.asList(p1v), nodeType.getField(f1).getProperties().get(p1Internal));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testProcessPropertiesJson() throws Exception {
        String path = "";
        String f1 = "abc";
        String p1 = "cmsEdf";
        String p1Internal = "xyz";
        List<String> p1v = Arrays.asList("{\"a\":1}", "\"b\":2");
        Map<String, Object> cmsProperties = new HashMap<>();
        cmsProperties.put(f1, makeField(p1, p1v));
        JsonNode properties = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(cmsProperties));
        JsonNode required = null;
        NodeType nodeType = new NodeType();

        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        Map<String, String> typeMappings = new HashMap<>();

        context.typeMappings = typeMappings;
        context.propertyMappings = Collections.singletonMap(p1, p1Internal);

        flowSchemaParser.processProperties(path, properties, required, nodeType, context);
        flowSchemaParser.postProcessFields(context);

        Assert.assertEquals(1, nodeType.getFields().size());
        Assert.assertTrue(nodeType.getFieldsNames().contains(f1));
        Assert.assertNotNull(nodeType.getField(f1));
        Assert.assertFalse(nodeType.getField(f1).getProperties().containsKey(p1));
        Assert.assertTrue(nodeType.getField(f1).getProperties().containsKey(p1Internal));
        Assert.assertEquals(p1v, nodeType.getField(f1).getProperties().get(p1Internal));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testProcessPropertiesNodeSimple() throws Exception {
        String path = "#/definitions/typeName";
        String typeNameInternal = "typeNameInternal";
        String f1 = "abc";
        String p1 = "cmsEdf";
        String p1Internal = "xyz";
        String p1v = "v1";
        Map<String, Object> cmsNode = new HashMap<>();
        Map<String, Object> cmsProperties = new HashMap<>();
        cmsProperties.put(f1, makeField(p1, p1v));
        cmsNode.put(FlowSchemaParser.TYPE, FlowSchemaParser.OBJECT);
        cmsNode.put(FlowSchemaParser.PROPERTIES, cmsProperties);
        cmsNode.put(p1, p1v);
        JsonNode node = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(cmsNode));
        NodeType parent = null;

        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        Map<String, String> typeMappings = new HashMap<>();
        typeMappings.put(path, typeNameInternal);
        context.typeMappings = typeMappings;
        context.propertyMappings = Collections.singletonMap(p1, p1Internal);

        FlowSchemaParser.SchemaNodeParsed schemaNodeParsed =
                flowSchemaParser.parseActualDefinition(node, path, context);

        Assert.assertNotNull(schemaNodeParsed);
        Assert.assertNotNull(schemaNodeParsed.nodeType);
        Assert.assertFalse(schemaNodeParsed.nodeType.getProperties().containsKey(p1));
        Assert.assertTrue(schemaNodeParsed.nodeType.getProperties().containsKey(p1Internal));
        Assert.assertEquals(Arrays.asList(p1v), schemaNodeParsed.nodeType.getProperties().get(p1Internal));
    }

    @Test
    public void testProcessBranchPropertiesNodeSimple() throws Exception {
        String path = "#/definitions/typeName";
        String typeNameInternal = "typeNameInternal";
        String pPathPrefix = FlowSchemaParser.CMS_PROPERTY_PREFIX_BRANCH;
        String pPath = "Edf[dsr]/fds";
        String p1 = FlowSchemaParser.CMS_PROPERTY_PREFIX + "Edf";
        String bp1 = pPathPrefix + pPath + "." + p1;
        String p1Internal = "xyz";
        String p1v = "v1";
        Map<String, Object> cmsNode = new HashMap<>();
        cmsNode.put(FlowSchemaParser.TYPE, FlowSchemaParser.OBJECT);
        cmsNode.put(bp1, p1v);
        JsonNode node = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(cmsNode));
        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        Map<String, String> typeMappings = new HashMap<>();
        typeMappings.put(path, typeNameInternal);
        context.typeMappings = typeMappings;
        context.propertyMappings = Collections.singletonMap(p1, p1Internal);

        FlowSchemaParser.SchemaNodeParsed schemaNodeParsed =
            flowSchemaParser.parseActualDefinition(node, path, context);

        Assert.assertNotNull(schemaNodeParsed);
        Assert.assertNotNull(schemaNodeParsed.nodeType);
        Map<String, List<String>> branchProps = schemaNodeParsed.nodeType.getPropertiesBranch().get(pPath);
        Assert.assertNotNull(branchProps);
        Assert.assertFalse(branchProps.containsKey(p1));
        Assert.assertTrue(branchProps.containsKey(p1Internal));
        Assert.assertEquals(Arrays.asList(p1v), branchProps.get(p1Internal));
    }

    private static Map<String, Object> makeField(String p, Object v) {
        HashMap<String, Object> result = new HashMap<>();

        result.put(FlowSchemaParser.TYPE, FlowSchemaParser.STRING);
        result.put(p, v);

        return result;
    }

    @Test
    public void testRequiredArrayField() throws Exception {
        String path = "";
        String f1 = "a";
        String p1 = "p1";
        String f2 = "b";
        String f3 = "c";
        Map<String, String> f2p = new HashMap<>();
        f2p.put(FlowSchemaParser.TYPE, FlowSchemaParser.ARRAY);
        Map<String, Object> cmsProperties = new HashMap<>();
        cmsProperties.put(f1, makeField(p1, Collections.emptyMap()));
        cmsProperties.put(f3, makeField(p1, Collections.emptyMap()));
        cmsProperties.put(f2, f2p);
        JsonNode properties = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(cmsProperties));
        JsonNode required = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(Arrays.asList(f1, f2)));
        NodeType nodeType = new NodeType();

        FlowSchemaParser.Context context = new FlowSchemaParser.Context();
        context.typeMappings = Collections.emptyMap();
        context.propertyMappings = Collections.emptyMap();

        flowSchemaParser.processProperties(path, properties, required, nodeType, context);
        flowSchemaParser.postProcessFields(context);


        Assert.assertFalse(nodeType.getField(f1).getProperties().containsKey(p1));
        Assert.assertEquals("1",
                nodeType.getField(f1).getFirstPropertyOrNull(Property.ALLOWED_VALUES_MIN.getName()));
        Assert.assertEquals("1",
                nodeType.getField(f2).getFirstPropertyOrNull(Property.ALLOWED_VALUES_MIN.getName()));
        Assert.assertNull(nodeType.getField(f3).getFirstPropertyOrNull(Property.ALLOWED_VALUES_MIN.getName()));
    }
}
