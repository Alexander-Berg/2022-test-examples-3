package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class PropertiesBranchOperationsPlus {

    @Test
    public void testNullPlusNull() {
        Assert.assertNull(PropertiesOperations.branchPropertiesPlus(null, null));
    }

    @Test
    public void testNullPlusProps() {
        Map<String, Map<String, List<String>>> props = PropertiesOperations.branchPropertiesPlus(
                null,
                createBranchProps("path", "prop")
        );
        Assert.assertTrue(props.containsKey("path"));
        Assert.assertTrue(props.get("path").containsKey("prop"));
        Assert.assertEquals("value", props.get("path").get("prop").get(0));
    }

    @Test
    public void testPropsPlusNull() {
        Map<String, Map<String, List<String>>> props = PropertiesOperations.branchPropertiesPlus(
                createBranchProps("path", "prop"),
                null
        );
        Assert.assertTrue(props.containsKey("path"));
        Assert.assertTrue(props.get("path").containsKey("prop"));
        Assert.assertEquals("value", props.get("path").get("prop").get(0));
    }

    @Test
    public void testPropsPlusProps() {

        Map<String, Map<String, List<String>>> props = PropertiesOperations.branchPropertiesPlus(
                createBranchProps("path1", "prop1"),
                createBranchProps("path2", "prop2")
        );

        Assert.assertEquals(2, props.size());
        Assert.assertTrue(props.containsKey("path1"));
        Assert.assertTrue(props.get("path1").containsKey("prop1"));
        Assert.assertEquals("value", props.get("path1").get("prop1").get(0));
        Assert.assertTrue(props.containsKey("path2"));
        Assert.assertTrue(props.get("path2").containsKey("prop2"));
        Assert.assertEquals("value", props.get("path2").get("prop2").get(0));

        props = PropertiesOperations.branchPropertiesPlus(
                createBranchProps("path", "prop1"),
                createBranchProps("path", "prop2")
        );

        Assert.assertEquals(1, props.size());
        Assert.assertTrue(props.containsKey("path"));
        Assert.assertTrue(props.get("path").containsKey("prop1"));
        Assert.assertEquals("value", props.get("path").get("prop1").get(0));
        Assert.assertTrue(props.get("path").containsKey("prop2"));
        Assert.assertEquals("value", props.get("path").get("prop2").get(0));
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
