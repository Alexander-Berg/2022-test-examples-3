package ru.yandex.market.wms.common.spring.dto;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.UserActivityStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IndirectActivityDtoTest extends IntegrationTest {
    @Autowired
    private ObjectMapper mapper;

    @Test
    public void canSerialize() throws IOException {
        final List<IndirectActivityDto> expected = List.of(
                IndirectActivityDto.builder()
                        .activityName("Обед")
                        .assigner("user")
                        .userName("test")
                        .endTime(Instant.parse("2021-12-03T12:00:00Z"))
                        .eventTime(Instant.parse("2021-12-03T12:00:00Z"))
                        .status(UserActivityStatus.PENDING)
                        .build()
        );

        final String content = mapper.writeValueAsString(expected);

        assertEquals(
                JsonParser.parseString("[" +
                        "{\"userName\":\"test\"," +
                        "\"activityName\":\"Обед\"," +
                        "\"assigner\":\"user\"," +
                        "\"status\":\"PENDING\"," +
                        "\"eventTime\":\"2021-12-03T12:00:00Z\"," +
                        "\"endTime\":\"2021-12-03T12:00:00Z\"}" +
                        "]"),
                JsonParser.parseString(content));
    }

    @Test
    public void canDeserialize() throws IOException {
        final IndirectActivityDto expected =
                IndirectActivityDto.builder()
                        .activityName("Обед")
                        .assigner("user")
                        .userName("test")
                        .endTime(Instant.parse("2021-12-03T12:00:00Z"))
                        .eventTime(Instant.parse("2021-12-03T12:00:00Z"))
                        .status(UserActivityStatus.PENDING)
                        .build();

        final String content =
                "{\"userName\":\"test\"," +
                "\"activityName\":\"Обед\"," +
                "\"assigner\":\"user\"," +
                "\"status\":\"PENDING\"," +
                "\"eventTime\":\"2021-12-03T12:00:00Z\"," +
                "\"endTime\":\"2021-12-03T12:00:00Z\"}";

        final IndirectActivityDto result = mapper.readValue(content, IndirectActivityDto.class);

        MatcherAssert.assertThat(result, samePropertyValuesAs(expected));
    }
}
