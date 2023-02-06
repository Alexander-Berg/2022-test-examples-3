package ru.yandex.common.util;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/**
 * @author btv (btv@yandex-team.ru)
 */
public class JsonUtilsTest extends TestCase {

    @Test
    public void testEmptyJsonGetByPath() throws Exception {
        final JSONObject json = new JSONObject("{}");
        assertNull(JsonUtils.getObjectByPath(json, "bla"));
        assertNull(JsonUtils.getObjectByPath(json, "bla", "tipa"));
    }

    @Test
    public void testFirstJsonGetByPath() throws Exception {
        final JSONObject json = new JSONObject("{'key': 'value', 'bool': true}");
        assertNull(JsonUtils.getObjectByPath(json, "bla"));
        assertEquals("value", JsonUtils.getObjectByPath(json, "key"));
        assertEquals(true, JsonUtils.getObjectByPath(json, "bool"));
    }

    @Test
    public void testDeepJsonGetByPath() throws Exception {
        final JSONObject json = new JSONObject("{'key': {'innerKey': 'value', 'null': null}}");
        assertNull(JsonUtils.getObjectByPath(json, "bla"));
        assertEquals("value", JsonUtils.getObjectByPath(json, "key", "innerKey"));
        assertEquals(JSONObject.NULL, JsonUtils.getObjectByPath(json, "key", "null"));
        assertEquals(null, JsonUtils.getObjectByPath(json, "key", "null", "null"));
    }

    @Test
    public void testArrayJsonGetByPath() throws Exception {
        final JSONObject json = new JSONObject("{'key': {'innerKey': [1, 2, 3], 'null': null}}");
        assertNull(JsonUtils.getObjectByPath(json, "bla"));
        assertTrue(JsonUtils.getObjectByPath(json, "key", "innerKey") instanceof JSONArray);
        assertEquals(1, ((JSONArray) JsonUtils.getObjectByPath(json, "key", "innerKey")).getInt(0));
    }

}
