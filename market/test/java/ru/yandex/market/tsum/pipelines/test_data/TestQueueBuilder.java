package ru.yandex.market.tsum.pipelines.test_data;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.tsum.clients.startrek.StartrekApiObjectMapper;
import ru.yandex.startrek.client.model.Queue;

public class TestQueueBuilder {
    private final Map<String, Object> jsonMap = new HashMap<>();

    private TestQueueBuilder() {
    }

    public static TestQueueBuilder aQueue() {
        return new TestQueueBuilder();
    }

    public TestQueueBuilder withId(long id) {
        jsonMap.put("id", id);
        return this;
    }

    public TestQueueBuilder withName(long id) {
        jsonMap.put("name", id);
        return this;
    }

    public Queue build() {
        try {
            // Version doesn't have public constructor, it can be constructed from JSON only
            // That's why these perversions are necessary
            ObjectMapper objectMapper = StartrekApiObjectMapper.get();

            String versionJson = objectMapper.writeValueAsString(jsonMap);

            return objectMapper.readValue(versionJson, Queue.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
