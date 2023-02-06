package ru.yandex.market.mbo.cms.api.servlets.export.util.changeset;

import java.util.Map;

import org.junit.Assert;

import ru.yandex.market.mbo.cms.core.models.NodeType;

public class NodeTypesEqualsTestHelper {

    private NodeTypesEqualsTestHelper() {
    }

    public static void assertEquals(NodeType n1, NodeType n2) {
        Assert.assertEquals(n1.getName(), n2.getName());
        Assert.assertEquals(n1.getParentNamesImmediate(), n2.getParentNamesImmediate());
        Assert.assertEquals(n1.getTemplates(), n2.getTemplates());
        Assert.assertEquals(n1.getPropertiesBranch(), n2.getPropertiesBranch());
        Assert.assertEquals(n1.getProperties(), n2.getProperties());
        FieldTypeEqualsTestHelper.assertEquals(n1.getFields(), n2.getFields());
    }

    public static void assertEquals(Map<String, NodeType> n1, Map<String, NodeType> n2) {
        if (n1 == null && n2 == null) {
            return;
        }
        Assert.assertTrue(n1 != null && n2 != null);
        Assert.assertTrue(n1.keySet().equals(n2.keySet()));
        for (Map.Entry<String, NodeType> entry : n1.entrySet()) {
            assertEquals(entry.getValue(), n2.get(entry.getKey()));
        }
    }
}
