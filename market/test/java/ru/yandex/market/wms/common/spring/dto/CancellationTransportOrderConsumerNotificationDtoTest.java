package ru.yandex.market.wms.common.spring.dto;

import java.time.Instant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CancellationTransportOrderConsumerNotificationDtoTest extends IntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void serializationTest() throws JsonProcessingException {
        CancellationTransportOrderConsumerNotificationDto dto =
                CancellationTransportOrderConsumerNotificationDto.builder()
                        .containerId("123123")
                        .time(Instant.ofEpochMilli(1622635351797L))
                        .build();
        final String expectedJsonString = "{\"containerId\":\"123123\",\"time\":\"2021-06-02T12:02:31.797Z\"}";

        String actualJsonString = objectMapper.writeValueAsString(dto);

        assertEquals(expectedJsonString, actualJsonString);
    }
}
