package ru.yandex.market.tsum.tms.tasks.multitesting;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.tsum.clients.startrek.StartrekApiObjectMapper;
import ru.yandex.startrek.client.model.FieldRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.01.2018
 */
public class TestFieldRefBuilder {
    private static final ObjectMapper OBJECT_MAPPER = StartrekApiObjectMapper.get();
    private final Map<String, Object> jsonMap = new HashMap<>();

    public static TestFieldRefBuilder aFieldRef() {
        return new TestFieldRefBuilder();
    }

    public TestFieldRefBuilder id(String id) {
        jsonMap.put("id", id);
        return this;
    }

    public FieldRef build() {
        try {
            // FieldRef doesn't have public constructor, it can be constructed from JSON only
            // That's why these perversions are necessary
            return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(jsonMap), FieldRef.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
