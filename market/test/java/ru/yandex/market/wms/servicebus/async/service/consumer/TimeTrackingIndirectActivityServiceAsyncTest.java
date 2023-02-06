package ru.yandex.market.wms.servicebus.async.service.consumer;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.wms.common.model.enums.UserActivityStatus;
import ru.yandex.market.wms.common.spring.dto.IndirectActivityDto;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.tts.TtsWebClient;
import ru.yandex.market.wms.shared.libs.employee.perfomance.model.IndirectActivityCompleteDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class TimeTrackingIndirectActivityServiceAsyncTest extends IntegrationTest {

    @SpyBean
    @Autowired
    private TtsWebClient ttsWebClient;

    @Autowired
    private TimeTrackingIndirectActivityServiceAsync service;

    @BeforeEach
    void init() {
        Mockito.reset(ttsWebClient);
    }

    @Test
    public void consumeIndirectActivityWhenValidationError() {
        final IndirectActivityDto dto = IndirectActivityDto.builder().build();
        service.consumeIndirectActivity(dto, null);
        Mockito.verify(ttsWebClient, times(0)).startIndirectActivity(any());
    }

    @Test
    public void consumeIndirectActivityWhenValidationOk() {
        final IndirectActivityDto dto = IndirectActivityDto.builder()
                .userName("testUserName")
                .activityName("testActivityName")
                .assigner("testassigner")
                .status(UserActivityStatus.IN_PROCESS)
                .eventTime(Instant.parse("2022-03-28T15:00:00Z"))
                .endTime(null)
                .build();
        service.consumeIndirectActivity(dto, null);
        Mockito.verify(ttsWebClient, times(1)).startIndirectActivity(any());
    }


    @Test
    public void consumeCompleteIndirectActivityWhenValidationError() {
        final IndirectActivityCompleteDto dto = IndirectActivityCompleteDto.builder().build();
        service.consumeCompleteIndirectActivity(dto, null);
        Mockito.verify(ttsWebClient, times(0)).completeIndirectActivityEvent(any());
    }

    @Test
    public void consumeCompleteIndirectActivityWhenValidationOk() {
        final IndirectActivityCompleteDto dto = IndirectActivityCompleteDto.builder()
                .warehouseName("test-warehouse")
                .finishedAt(Instant.parse("2022-06-01T18:00:00Z"))
                .startedAt(Instant.parse("2022-06-01T17:00:00Z"))
                .eventTime(Instant.parse("2022-06-01T18:03:03Z"))
                .userActivityKey("12345")
                .userName("userName")
                .activityName("activityName")
                .env("TESTING")
                .operationDay(LocalDate.of(2022, 06, 22))
                .build();
        service.consumeCompleteIndirectActivity(dto, null);
        Mockito.verify(ttsWebClient, times(1)).completeIndirectActivityEvent(any());
    }

}
