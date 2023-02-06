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

public class NodeTypeOperationsPlusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusNull() {
        NodeTypeOperations.nodeTypePlus(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusNodeType() {
        NodeTypeOperations.nodeTypePlus(null, new NodeType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNodeTypePlusNull() {
        NodeTypeOperations.nodeTypePlus(new NodeType(), null);
    }

    @Test
    public void testEmptyPlusEmpty() {
        NodeType nt1 = new NodeType();
        NodeType nt2 = new NodeType();

        NodeType result = NodeTypeOperations.nodeTypePlus(nt1, nt2);

        Assert.assertNull(result.getName());
        Assert.assertNull(result.getParentNamesImmediate());
        Assert.assertNull(result.getFields());
        Assert.assertNull(result.getTemplates());
        Assert.assertNull(result.getProperties());
        Assert.assertNull(result.getPropertiesBranch());
    }

    @Test
    public void testNodeTypePlusEmpty() {
        NodeType nt1 = new NodeType();
        NodeType nt2 = new NodeType();

        nt1.setName("name");
        nt1.setParentNamesImmediate(Collections.singletonList("parent"));
        nt1.setFields(new LinkedHashMap<>());
        nt1.setTemplates(new HashMap<>());
        nt1.setProperties(new HashMap<>());
        nt1.setPropertiesBranch(new HashMap<>());

        NodeType result = NodeTypeOperations.nodeTypePlus(nt1, nt2);

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
    public void testEmptyPlusNodeType() {
        NodeType nt1 = new NodeType();
        NodeType nt2 = new NodeType();

        nt2.setName("name");
        nt2.setParentNamesImmediate(Collections.singletonList("parent"));
        nt2.setFields(new LinkedHashMap<>());
        nt2.setTemplates(new HashMap<>());
        nt2.setProperties(new HashMap<>());
        nt2.setPropertiesBranch(new HashMap<>());

        NodeType result = NodeTypeOperations.nodeTypePlus(nt1, nt2);

        Assert.assertNull(result.getName());
        Assert.assertEquals(nt1.getName(), result.getName());
        Assert.assertNotNull(result.getParentNamesImmediate());
        Assert.assertEquals(nt2.getParentNamesImmediate(), result.getParentNamesImmediate());
        Assert.assertNotNull(result.getFields());
        Assert.assertEquals(nt2.getFields(), result.getFields());
        Assert.assertNotNull(result.getTemplates());
        Assert.assertEquals(nt2.getTemplates(), result.getTemplates());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(nt2.getProperties(), result.getProperties());
        Assert.assertNotNull(result.getPropertiesBranch());
        Assert.assertEquals(nt2.getPropertiesBranch(), result.getPropertiesBranch());
    }

    @Test
    public void testNodeTypePlusNodeType() {
        NodeType nt1 = new NodeType();
        NodeType nt2 = new NodeType();

        nt1.setName("name");
        nt1.setParentNamesImmediate(Collections.singletonList("parent"));
        nt1.setFields(new LinkedHashMap<>());
        nt1.setTemplates(new HashMap<>());
        nt1.setProperties(new HashMap<>());
        nt1.setPropertiesBranch(new HashMap<>());

        LinkedHashMap<String, FieldType> fields = new LinkedHashMap<>();
        fields.put("fi", new FieldType("fi"));

        Map<Constants.Device, Map<Constants.Format, String>> templates = new HashMap<>();
        templates.computeIfAbsent(Constants.Device.DESKTOP, device -> new HashMap<>()).put(Constants.Format.JSON, "t");

        Map<String, List<String>> properties = new HashMap<>();
        properties.put("prop", Collections.singletonList("v"));

        Map<String, Map<String, List<String>>> propertiesBranch = new HashMap<>();
        propertiesBranch.computeIfAbsent("path", s -> new HashMap<>()).put("prop", Collections.singletonList("v"));

        nt2.setName("name2");
        nt2.setParentNamesImmediate(Collections.singletonList("parent2"));
        nt2.setFields(fields);
        nt2.setTemplates(templates);
        nt2.setProperties(properties);
        nt2.setPropertiesBranch(propertiesBranch);

        NodeType result = NodeTypeOperations.nodeTypePlus(nt1, nt2);

        Assert.assertNotNull(result.getName());
        Assert.assertEquals(nt1.getName(), result.getName());
        Assert.assertNotNull(result.getParentNamesImmediate());
        Assert.assertEquals(nt2.getParentNamesImmediate(), result.getParentNamesImmediate());
        Assert.assertNotNull(result.getFields());
        Assert.assertEquals(nt2.getFields(), result.getFields());
        Assert.assertNotNull(result.getTemplates());
        Assert.assertEquals(nt2.getTemplates(), result.getTemplates());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(nt2.getProperties(), result.getProperties());
        Assert.assertNotNull(result.getPropertiesBranch());
        Assert.assertEquals(nt2.getPropertiesBranch(), result.getPropertiesBranch());
    }

    @Test
    public void testNodeTypesPlus() {
        Assert.assertNull(NodeTypeOperations.nodeTypesPlus(null, null));
        Assert.assertEquals(
                1,
                NodeTypeOperations.nodeTypesPlus(
                        Collections.singletonMap("nt", new NodeType("nt")), null
                ).size()
        );
        Assert.assertEquals(
                1,
                NodeTypeOperations.nodeTypesPlus(
                        null,
                        Collections.singletonMap("nt", new NodeType("nt"))
                ).size()
        );
        Assert.assertEquals(
                1,
                NodeTypeOperations.nodeTypesPlus(
                        Collections.singletonMap("nt", new NodeType("nt")),
                        Collections.singletonMap("nt", new NodeType("nt"))
                ).size()
        );
        Assert.assertEquals(
                2,
                NodeTypeOperations.nodeTypesPlus(
                        Collections.singletonMap("nt1", new NodeType("nt1")),
                        Collections.singletonMap("nt2", new NodeType("nt2"))
                ).size()
        );
    }
}
