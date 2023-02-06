package ru.yandex.market.wms.timetracker.json;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.response.SystemActivityResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class SystemActivityResponseTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void canSerialize() throws JsonProcessingException {

        final SystemActivityResponse response = SystemActivityResponse.builder()
                .userName("test")
                .process("process")
                .assigner("assigner")
                .createTime(LocalDateTime.of(2021, 12, 15, 12, 30, 50))
                .expectedEndTime(LocalDateTime.of(2021, 12, 15, 12, 30, 50))
                .zone("testZone")
                .userStartedActivity(true)
                .build();

        String content = mapper.writeValueAsString(response);

        String expected  = "{" +
                " \"userName\" : \"test\", " +
                "\"process\": \"process\", " +
                "\"assigner\": \"assigner\", " +
                "\"create_time\": \"2021-12-15T12:30:50\", " +
                "\"expected_end_time\": \"2021-12-15T12:30:50\", " +
                "\"zone\": \"testZone\", " +
                "\"user_started_activity\": true" +
                "}";

        assertEquals(
                JsonParser.parseString(expected),
                JsonParser.parseString(content));
    }
}
