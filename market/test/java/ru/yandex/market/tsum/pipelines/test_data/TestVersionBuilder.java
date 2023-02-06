package ru.yandex.market.tsum.pipelines.test_data;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.tsum.clients.startrek.StartrekApiObjectMapper;
import ru.yandex.startrek.client.model.Version;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.05.17
 */
public class TestVersionBuilder {
    private final Map<String, Object> jsonMap = new HashMap<>();

    private TestVersionBuilder() {
    }

    public static TestVersionBuilder aVersion() {
        return new TestVersionBuilder();
    }

    public TestVersionBuilder withId(long id) {
        jsonMap.put("id", id);
        return this;
    }

    public TestVersionBuilder withName(String name) {
        jsonMap.put("name", name);
        return this;
    }

    public Version build() {
        try {
            // Version doesn't have public constructor, it can be constructed from JSON only
            // That's why these perversions are necessary
            ObjectMapper objectMapper = StartrekApiObjectMapper.get();

            String versionJson = objectMapper.writeValueAsString(jsonMap);

            return objectMapper.readValue(versionJson, Version.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
