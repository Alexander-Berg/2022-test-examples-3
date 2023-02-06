package ru.yandex.market.mboc.app.util;

import java.util.Arrays;

import net.minidev.json.JSONArray;

/**
 * @author yuramalinov
 * @created 25.09.18
 */
public class JsonPathUtils {
    private JsonPathUtils() {
    }

    public static JSONArray jsonArray(Object... objects) {
        JSONArray array = new JSONArray();
        array.addAll(Arrays.asList(objects));
        return array;
    }
}
