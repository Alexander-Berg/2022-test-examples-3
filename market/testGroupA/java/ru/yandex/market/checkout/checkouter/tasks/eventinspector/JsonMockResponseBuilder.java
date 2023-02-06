package ru.yandex.market.checkout.checkouter.tasks.eventinspector;

import java.util.HashMap;
import java.util.Map;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

/**
 * Формирователь мок-ответа на основе файла ресурсов и через изменение значений при помощи JsonPath.
 *
 * @author sergeykoles
 * Created on: 14.05.18
 */
public class JsonMockResponseBuilder {

    private final DocumentContext context;
    private final Map<String, Object> substitutions = new HashMap<>();

    private JsonMockResponseBuilder(DocumentContext context) {
        this.context = context;
    }

    public static JsonMockResponseBuilder createFrom(String resourceName) {
        return new JsonMockResponseBuilder(
                JsonPath.parse(JsonMockResponseBuilder.class.getResourceAsStream(resourceName))
        );
    }

    public static JsonMockResponseBuilder createFrom(Class<?> clz, String resourceName) {
        return new JsonMockResponseBuilder(
                JsonPath.parse(clz.getResourceAsStream(resourceName))
        );
    }

    public <T> JsonMockResponseBuilder set(String jsonPath, T value) {
        substitutions.put(jsonPath, value);
        return this;
    }

    public String build() {
        substitutions.forEach(
                (k, v) -> context.set(JsonPath.compile(k), v)
        );
        return context.jsonString();
    }
}
