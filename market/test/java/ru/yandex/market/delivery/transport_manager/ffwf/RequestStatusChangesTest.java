package ru.yandex.market.delivery.transport_manager.ffwf;

import java.io.IOException;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.market.ff.client.dto.RequestStatusChangeDto;
import ru.yandex.market.ff.client.dto.RequestStatusChangesDto;
import ru.yandex.market.ff.client.enums.RequestStatus;

public class RequestStatusChangesTest {
    private final ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().createXmlMapper(false).build();

    @Test
    void deserializeTest() throws IOException {
        String s = "{\"requestStatusChanges\":[{\"requestId\":318795,\"changedAt\":\"2020-11-11T16:23:00\"," +
            "\"receivedChangedAt\":\"2020-11-11T16:23:00\",\"oldStatus\":1,\"newStatus\":100}]}";
        RequestStatusChangesDto requestStatusChangesDto = objectMapper.readValue(s, RequestStatusChangesDto.class);
        Assertions.assertEquals(requestStatusChangesDto.getRequestStatusChanges().size(), 1);

        RequestStatusChangeDto changeDto = requestStatusChangesDto.getRequestStatusChanges().get(0);

        Assertions.assertEquals(changeDto.getRequestId(), 318795L);
        Assertions.assertEquals(changeDto.getChangedAt(), LocalDateTime.of(2020, 11, 11, 16, 23));
        Assertions.assertEquals(changeDto.getReceivedChangedAt(), LocalDateTime.of(2020, 11, 11, 16, 23));
        Assertions.assertEquals(changeDto.getOldStatus(), RequestStatus.VALIDATED);
        Assertions.assertEquals(changeDto.getNewStatus(), RequestStatus.CANCELLATION_REQUESTED);
    }
}
