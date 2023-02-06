package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class PropertiesOperationsPlusTest {

    @Test
    public void testNullPlusNull() {
        Assert.assertNull(PropertiesOperations.propertiesPlus(null, null));
    }

    @Test
    public void testNullPlusProps() {
        Assert.assertTrue(
                PropertiesOperations.propertiesPlus(null, createProps("prop"))
                        .containsKey("prop")
        );
        Assert.assertEquals(
                "value",
                PropertiesOperations.propertiesPlus(null, createProps("prop"))
                        .get("prop").get(0)
        );
    }

    @Test
    public void testPropsPlusNull() {
        Assert.assertTrue(
                PropertiesOperations.propertiesPlus(createProps("prop"), null)
                        .containsKey("prop")
        );
        Assert.assertEquals(
                "value",
                PropertiesOperations.propertiesPlus(createProps("prop"), null)
                        .get("prop").get(0)
        );
    }

    @Test
    public void testPropsPlusProps() {
        Map<String, List<String>> props = PropertiesOperations.propertiesPlus(
                createProps("prop1"), createProps("prop2")
        );
        Assert.assertEquals(2, props.size());
        Assert.assertTrue(props.containsKey("prop1"));
        Assert.assertEquals("value", props.get("prop1").get(0));
        Assert.assertTrue(props.containsKey("prop2"));
        Assert.assertEquals("value", props.get("prop2").get(0));

        props = PropertiesOperations.propertiesPlus(
                createProps("prop", Collections.singletonList("v1")),
                createProps("prop", Collections.singletonList("v2"))
        );
        Assert.assertEquals(1, props.size());
        Assert.assertTrue(props.containsKey("prop"));
        Assert.assertEquals(1, props.get("prop").size());
        Assert.assertEquals("v2", props.get("prop").get(0));

    }

    private Map<String, List<String>> createProps(String propName, List<String> value) {
        return Collections.singletonMap(propName, value);
    }

    private Map<String, List<String>> createProps(String propName) {
        return Collections.singletonMap(propName, Collections.singletonList("value"));
    }
}
