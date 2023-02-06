package ru.yandex.market.logistics.iris.core.index;

import org.assertj.core.api.Condition;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class JsonMatchers {

    public static Condition<? super String> matchingJson(String expectedJson) {
        return jsonMatchingInternal(expectedJson, JSONCompareMode.STRICT);
    }

    public static Condition<? super String> matchingJsonWithoutOrder(String expectedJson) {
        return jsonMatchingInternal(expectedJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    private static Condition<? super String> jsonMatchingInternal(String expectedJson, JSONCompareMode mode) {
        return new Condition<String>() {
            @Override
            public boolean matches(String actualJson) {
                try {
                    JSONAssert.assertEquals(expectedJson, actualJson, mode);
                    return true;
                } catch (JSONException e) {
                    return false;
                }
            }
        };
    }
}
