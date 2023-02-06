package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.FieldType;
import ru.yandex.market.mbo.cms.core.models.NodeType;

public class NodeTypeOperationsMinusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusNull() {
        NodeTypeOperations.nodeTypeMinus(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusNodeType() {
        NodeTypeOperations.nodeTypeMinus(null, new NodeType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNodeTypeMinusNull() {
        NodeTypeOperations.nodeTypeMinus(new NodeType(), null);
    }

    @Test
    public void testCopyNullNodeType() {
        Assert.assertNull(NodeTypeOperations.copyOfNodeTypes(null));
    }

    @Test
    public void testEmptyMinusEmpty() {
        NodeType nt1 = new NodeType();
        NodeType nt2 = new NodeType();

        NodeType result = NodeTypeOperations.nodeTypeMinus(nt1, nt2);

        Assert.assertNull(result.getName());
        Assert.assertNull(result.getParentNamesImmediate());
        Assert.assertNull(result.getFields());
        Assert.assertNull(result.getTemplates());
        Assert.assertNull(result.getProperties());
        Assert.assertNull(result.getPropertiesBranch());
    }

    @Test
    public void testNodeTypeMinusEmpty() {
        NodeType nt1 = new NodeType();
        NodeType nt2 = new NodeType();

        nt1.setName("name");
        nt1.setParentNamesImmediate(Collections.singletonList("parent"));
        nt1.setFields(new LinkedHashMap<>());
        nt1.setTemplates(new HashMap<>());
        nt1.setProperties(new HashMap<>());
        nt1.setPropertiesBranch(new HashMap<>());

        NodeType result = NodeTypeOperations.nodeTypeMinus(nt1, nt2);

        Assert.assertNotNull(result.getName());
        Assert.assertEquals(nt1.getName(), result.getName());
        Assert.assertNotNull(result.getParentNamesImmediate());
        Assert.assertEquals(nt1.getParentNamesImmediate(), result.getParentNamesImmediate());
        Assert.assertNotNull(result.getFields());
        Assert.assertEquals(nt1.getFields(), result.getFields());
        Assert.assertNotNull(result.getTemplates());
        Assert.assertEquals(nt1.getTemplates(), result.getTemplates());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(nt1.getProperties(), result.getProperties());
        Assert.assertNotNull(result.getPropertiesBranch());
        Assert.assertEquals(nt1.getPropertiesBranch(), result.getPropertiesBranch());
    }

    @Test
    public void testEmptyMinusNodeType() {
        NodeType nt1 = new NodeType();
        NodeType nt2 = new NodeType();

        nt2.setName("name1");
        nt2.setName("name2");
        nt2.setParentNamesImmediate(Collections.singletonList("parent"));
        nt2.setFields(new LinkedHashMap<>());
        nt2.setTemplates(new HashMap<>());
        nt2.setProperties(new HashMap<>());
        nt2.setPropertiesBranch(new HashMap<>());

        NodeType result = NodeTypeOperations.nodeTypeMinus(nt1, nt2);

        Assert.assertNull(result.getName());
        Assert.assertEquals(nt1.getName(), result.getName());
        Assert.assertNull(result.getParentNamesImmediate());
        Assert.assertNull(result.getFields());
        Assert.assertNull(result.getTemplates());
        Assert.assertNull(result.getProperties());
        Assert.assertNull(result.getPropertiesBranch());
    }

    @Test
    public void testNodeTypeMinusNodeType() {
        NodeType nt1 = new NodeType();
        NodeType nt2 = new NodeType();


        LinkedHashMap<String, FieldType> fields = new LinkedHashMap<>();
        fields.put("fi", new FieldType("fi"));

        Map<Constants.Device, Map<Constants.Format, String>> templates = new HashMap<>();
        templates.computeIfAbsent(Constants.Device.DESKTOP, device -> new HashMap<>()).put(Constants.Format.JSON, "t");

        Map<String, List<String>> properties = new HashMap<>();
        properties.put("prop", Collections.singletonList("v"));

        Map<String, Map<String, List<String>>> propertiesBranch = new HashMap<>();
        propertiesBranch.computeIfAbsent("path", s -> new HashMap<>()).put("prop", Collections.singletonList("v"));


        nt1.setName("name");
        nt1.setParentNamesImmediate(Collections.singletonList("parent"));
        nt1.setFields(fields);
        nt1.setTemplates(templates);
        nt1.setProperties(properties);
        nt1.setPropertiesBranch(propertiesBranch);

        nt2.setName("name2");
        nt2.setParentNamesImmediate(Collections.singletonList("parent"));
        nt2.setFields(fields);
        nt2.setTemplates(templates);
        nt2.setProperties(properties);
        nt2.setPropertiesBranch(propertiesBranch);

        NodeType result = NodeTypeOperations.nodeTypeMinus(nt1, nt2);

        Assert.assertNotNull(result.getName());
        Assert.assertEquals(nt1.getName(), result.getName());
        Assert.assertNotNull(result.getParentNamesImmediate());
        Assert.assertEquals(1, result.getParentNamesImmediate().size());
        Assert.assertNotNull(result.getFields());
        Assert.assertEquals(0, result.getFields().size());
        Assert.assertNotNull(result.getTemplates());
        Assert.assertEquals(0, result.getTemplates().size());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(0, result.getProperties().size());
        Assert.assertNotNull(result.getPropertiesBranch());
        Assert.assertEquals(0, result.getPropertiesBranch().size());
    }

    @Test
    public void testNodeTypesMinus() {
        Assert.assertNull(NodeTypeOperations.nodeTypesMinus(null, null));
        Assert.assertEquals(
                1,
                NodeTypeOperations.nodeTypesMinus(
                        Collections.singletonMap("nt", new NodeType("nt")), null
                ).size()
        );
        Assert.assertNull(
                NodeTypeOperations.nodeTypesMinus(
                        null,
                        Collections.singletonMap("nt", new NodeType("nt"))
                )
        );
        Assert.assertEquals(
                0,
                NodeTypeOperations.nodeTypesMinus(
                        Collections.singletonMap("nt", new NodeType("nt")),
                        Collections.singletonMap("nt", new NodeType("nt"))
                ).size()
        );
        NodeType nodeType = new NodeType("nt");
        nodeType.addProperty("prop", Collections.emptyList());
        Assert.assertEquals(
                1,
                NodeTypeOperations.nodeTypesMinus(
                        Collections.singletonMap("nt", new NodeType("nt")),
                        Collections.singletonMap("nt", nodeType)
                ).size()
        );
        Assert.assertEquals(
                1,
                NodeTypeOperations.nodeTypesMinus(
                        Collections.singletonMap("nt1", new NodeType("nt1")),
                        Collections.singletonMap("nt2", new NodeType("nt2"))
                ).size()
        );
    }

}
