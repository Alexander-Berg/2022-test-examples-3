package ru.yandex.market.mboc.common.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * Small helper because it's not only required to set field, but to also properly restore it.
 *
 * @author yuramalinov
 * @created 11.08.2019
 */
public class TestFieldReplacer {
    private Map<Object, Map<String, Object>> oldValues = new HashMap<>();

    public TestFieldReplacer replace(Object object, String field, Object value) {
        Object oldValue = ReflectionTestUtils.getField(object, field);
        oldValues.computeIfAbsent(object, b -> new HashMap<>()).putIfAbsent(field, oldValue);
        ReflectionTestUtils.setField(object, field, value);
        return this;
    }

    public void restore() {
        oldValues.forEach((object, map) ->
            map.forEach((field, value) ->
                ReflectionTestUtils.setField(object, field, value)));
    }
}
