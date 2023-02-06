package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class PropertiesBranchOperationsMinus {

    @Test
    public void testNullMinusNull() {
        Assert.assertNull(PropertiesOperations.branchPropertiesMinus(null, null));
    }

    @Test
    public void testNullMinusProps() {
        Map<String, Map<String, List<String>>> props = PropertiesOperations.branchPropertiesMinus(
                null,
                createBranchProps("path", "prop")
        );
        Assert.assertNull(props);
    }

    @Test
    public void testPropsMinusNull() {
        Map<String, Map<String, List<String>>> props = PropertiesOperations.branchPropertiesMinus(
                createBranchProps("path", "prop"),
                null
        );
        Assert.assertTrue(props.containsKey("path"));
        Assert.assertTrue(props.get("path").containsKey("prop"));
        Assert.assertEquals("value", props.get("path").get("prop").get(0));
    }

    @Test
    public void testPropsMinusProps() {
        Map<String, Map<String, List<String>>> props = PropertiesOperations.branchPropertiesMinus(
                createBranchProps("path1", "prop1"),
                createBranchProps("path2", "prop2")
        );
        Assert.assertEquals(1, props.size());
        Assert.assertTrue(props.containsKey("path1"));
        Assert.assertTrue(props.get("path1").containsKey("prop1"));
        Assert.assertEquals(1, props.get("path1").get("prop1").size());
        Assert.assertEquals("value", props.get("path1").get("prop1").get(0));

        props = PropertiesOperations.branchPropertiesMinus(
                createBranchProps("path", "prop1"),
                createBranchProps("path", "prop2")
        );
        Assert.assertEquals(1, props.size());
        Assert.assertTrue(props.containsKey("path"));
        Assert.assertTrue(props.get("path").containsKey("prop1"));
        Assert.assertEquals(1, props.get("path").get("prop1").size());
        Assert.assertEquals("value", props.get("path").get("prop1").get(0));

        props = PropertiesOperations.branchPropertiesMinus(
                createBranchProps("path", "prop"),
                Collections.singletonMap("path", null)
        );
        Assert.assertEquals(0, props.size());
    }

    private Map<String, Map<String, List<String>>> createBranchProps(String path, String propName) {
        return Collections.singletonMap(path, createProps(propName));
    }

    private Map<String, List<String>> createProps(String propName, List<String> value) {
        return Collections.singletonMap(propName, value);
    }

    private Map<String, List<String>> createProps(String propName) {
        return Collections.singletonMap(propName, Collections.singletonList("value"));
    }
}
