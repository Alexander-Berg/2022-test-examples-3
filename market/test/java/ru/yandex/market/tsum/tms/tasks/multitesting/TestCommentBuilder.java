package ru.yandex.market.tsum.tms.tasks.multitesting;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.tsum.clients.startrek.StartrekApiObjectMapper;
import ru.yandex.startrek.client.model.Comment;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 12.01.2018
 */
public class TestCommentBuilder {
    private static final ObjectMapper OBJECT_MAPPER = StartrekApiObjectMapper.get();
    private final Map<String, Object> jsonMap = new HashMap<>();

    public static TestCommentBuilder aComment() {
        return new TestCommentBuilder();
    }

    public TestCommentBuilder createdAt(Instant instant) {
        jsonMap.put("createdAt", new org.joda.time.Instant(instant.toEpochMilli()));
        return this;
    }

    public TestCommentBuilder withText(String text) {
        jsonMap.put("text", text);
        return this;
    }

    public Comment build() {
        try {
            // Comment doesn't have public constructor, it can be constructed from JSON only
            // That's why these perversions are necessary
            return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(jsonMap), Comment.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
