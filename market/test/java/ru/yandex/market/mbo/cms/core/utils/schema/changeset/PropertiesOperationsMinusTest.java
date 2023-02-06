package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class PropertiesOperationsMinusTest {

    @Test
    public void testNullMinusNull() {
        Assert.assertNull(PropertiesOperations.propertiesMinus(null, null));
    }

    @Test
    public void testNullMinusProps() {
        Assert.assertNull(PropertiesOperations.propertiesMinus(null, createProps("prop")));
    }

    @Test
    public void testPropsMinusNull() {
        Map<String, List<String>> prop = PropertiesOperations.propertiesMinus(createProps("prop"), null);
        Assert.assertTrue(prop.containsKey("prop"));
        Assert.assertEquals("value", prop.get("prop").get(0));
    }

    @Test
    public void testPropsMinusProps() {
        Map<String, List<String>> props = PropertiesOperations.propertiesMinus(
                createProps("prop1"), createProps("prop2")
        );
        Assert.assertEquals(1, props.size());
        Assert.assertTrue(props.containsKey("prop1"));
        Assert.assertEquals("value", props.get("prop1").get(0));

        props = PropertiesOperations.propertiesMinus(
                createProps("prop", Collections.singletonList("v1")),
                createProps("prop", Collections.singletonList("v2"))
        );
        Assert.assertEquals(0, props.size());
    }

    private Map<String, List<String>> createProps(String propName, List<String> value) {
        return Collections.singletonMap(propName, value);
    }

    private Map<String, List<String>> createProps(String propName) {
        return Collections.singletonMap(propName, Collections.singletonList("value"));
    }
}
