package ru.yandex.chemodan.util.test;

import java.util.Map;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * Test for JsonTestUtils class
 *
 * @author buberman
 */
public class JsonTestUtilsTest {
    @Test
    public void testParseJsonSimpleCase() {
        String simpleJson = "{\"field\":\"value\"}";
        Map<String, Object> result = JsonTestUtils.parseJsonToMap(simpleJson.getBytes());
        Assert.notNull(result);
        Assert.sizeIs(1, result);
        Assert.isTrue(result.containsKey("field"));
        Assert.equals("value", result.get("field"));
    }

    @Test
    public void testParseJsonSeveralRecords() {
        String simpleJson = "{\"field\":\"value\", \"field2\":\"value2\", \"field3\":\"value3\"}";
        Map<String, Object> result = JsonTestUtils.parseJsonToMap(simpleJson.getBytes());
        Assert.notNull(result);
        Assert.sizeIs(3, result);
        Assert.isTrue(result.containsKey("field"));
        Assert.equals("value", result.get("field"));
        Assert.isTrue(result.containsKey("field2"));
        Assert.equals("value2", result.get("field2"));
        Assert.isTrue(result.containsKey("field3"));
        Assert.equals("value3", result.get("field3"));
    }

    @Test
    public void testParseJsonEmptyObject() {
        String simpleJson = "{}";
        Map<String, Object> result = JsonTestUtils.parseJsonToMap(simpleJson.getBytes());
        Assert.notNull(result);
        Assert.isEmpty(result);
    }

    /**
     * Input is a byte array that contains the serialized JSON object followed by zeroes.
     */
    @Test
    public void testParseJsonArrayWithZeroTail() {
        String simpleJson = "{\"field\":\"value\"}";
        byte[] buffer = new byte[1024];
        byte[] simpleJsonBytes = simpleJson.getBytes();
        System.arraycopy(simpleJsonBytes, 0, buffer, 0, simpleJsonBytes.length);
        Map<String, Object> result = JsonTestUtils.parseJsonToMap(buffer);
        Assert.notNull(result);
        Assert.sizeIs(1, result);
        Assert.isTrue(result.containsKey("field"));
        Assert.equals("value", result.get("field"));
    }
}
