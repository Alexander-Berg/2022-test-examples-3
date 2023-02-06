package ru.yandex.market.common.report.parser.json;

import java.math.BigDecimal;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONParser;

import static org.junit.Assert.*;

/**
 * @author dzvyagin
 */
public class JsonObjectWrapperTest {

    //language=JSON
    private static final String BIGDECIMAL_MAP_JSON = "{\n" +
            "\"map\": {" +
            "\"key1\": \"17.23\",\n" +
            "\"key2\": \"18.55\",\n" +
            "\"key3\": \"121.55\"\n" +
            "}}";


    @Test
    public void has() throws Exception {
        JSONObject jsonObject = (JSONObject) JSONParser.parseJSON(BIGDECIMAL_MAP_JSON);
        JsonObjectWrapper wrapper = new JsonObjectWrapper(jsonObject);
        assertTrue(wrapper.has("map.key1"));
        assertTrue(wrapper.has("map.key2"));
        assertTrue(wrapper.has("map.key3"));
        assertFalse(wrapper.has("map.key4"));
        assertFalse(wrapper.has("map.key1.subkey"));
    }

    @Test
    public void getBigDecimalMap() throws Exception {
        JSONObject jsonObject = (JSONObject) JSONParser.parseJSON(BIGDECIMAL_MAP_JSON);
        JsonObjectWrapper wrapper = new JsonObjectWrapper(jsonObject);
        Map<String, BigDecimal> map = wrapper.getBigDecimalMap("map");
        assertEquals(new BigDecimal("17.23"), map.get("key1"));
        assertEquals(new BigDecimal("18.55"), map.get("key2"));
        assertEquals(new BigDecimal("121.55"), map.get("key3"));
    }
}