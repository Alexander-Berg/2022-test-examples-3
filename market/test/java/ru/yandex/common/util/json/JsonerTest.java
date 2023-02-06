package ru.yandex.common.util.json;

import junit.framework.TestCase;
import org.json.JSONException;
import org.json.JSONObject;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.common.util.collections.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static ru.yandex.common.util.json.Jsoner.object;
import static ru.yandex.common.util.json.Jsoner.pair;

/**
 * Date: May 6, 2011
 * Time: 7:18:42 PM
 *
 * @author Dima Schitinin, dimas@yandex-team.ru
 */
public class JsonerTest extends TestCase {

    public void testJsoner() throws Exception {
        final String myJsonAsString = getJsonerJsonAsString();
        final JSONObject standardJson = getStandardJson();
        final JSONObject myJson = new JSONObject(myJsonAsString);
        assertEquals(standardJson.toString(), myJson.toString());
    }

    public void testEmptyMap() throws Exception {
        final String myJsonAsString = toJsonString(object(Cf.<String, Object>newHashMap()));
        final JSONObject standardJson = new JSONObject();
        final JSONObject myJson = new JSONObject(myJsonAsString);
        assertEquals(standardJson.toString(), myJson.toString());
    }

    public void testMap() throws Exception {
        Map<String, Object> map = Collections.<String, Object>singletonMap("God Is an Astronaut'\"", new Integer(1));
        String myJsonAsString = toJsonString(object(map));
        JSONObject standardJson = new JSONObject(map);
        JSONObject myJson = new JSONObject(myJsonAsString);
        assertEquals(standardJson.toString(), myJson.toString());

        map = Cu.zipMap(Pair.<String, Object>of("here", null), Pair.<String, Object>of("we", "go!"));
        myJsonAsString = toJsonString(object(map));
        standardJson = new JSONObject(map);
        myJson = new JSONObject(myJsonAsString);
        assertEquals(standardJson.toString(), myJson.toString());
    }

    private JSONObject getStandardJson() throws JSONException {
        final JSONObject phone = new JSONObject();
        phone.put("code", 812);
        phone.put("number", "123");
        phone.put("public", true);

        final JSONObject json = new JSONObject();
        json.put("name", "Vasya");
        json.put("phone", phone);
        json.put("array", Arrays.asList(1, 2, 3, 4));
        return json;
    }

    private String getJsonerJsonAsString() {
        final Jsoner.JsonObject jsonObject = object(
                pair("name", "Vasya"),
                pair("phone",
                        object(
                                pair("code", 812),
                                pair("number", "123"),
                                pair("public", true))),
                pair("array", Jsoner.arrayOfSimples(1, 2, 3, 4)));
        return toJsonString(jsonObject);
    }

    private String toJsonString(final Jsoner.JsonObject jsonObject) {
        final StringBuilder sb = new StringBuilder();
        jsonObject.toJson(sb);
        return sb.toString();
    }
}
