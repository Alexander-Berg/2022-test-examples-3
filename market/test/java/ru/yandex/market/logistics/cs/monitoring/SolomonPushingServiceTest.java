package ru.yandex.market.logistics.cs.monitoring;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.JSONCompareMode;

import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.domain.dto.NotificationCounterDto;
import ru.yandex.market.logistics.cs.domain.dto.PartnerDto;
import ru.yandex.market.logistics.cs.service.impl.SolomonPushingServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Декодирование входных данных для соломон пушера")
class SolomonPushingServiceTest extends AbstractTest {

    @Mock
    SolomonClient solomonClient;

    @Mock
    Clock clock;

    @Captor
    ArgumentCaptor<String> contentCaptor;

    @InjectMocks
    private SolomonPushingServiceImpl pushingService;

    @BeforeEach
    public void setUp() {
        when(clock.millis()).thenReturn(1633705000L);
        doNothing().when(solomonClient).push(any());
        verifyNoMoreInteractions(solomonClient);
    }

    @Test
    @DisplayName("Пуш сенсора на перелив капасити")
    void pushOverflowMetricsTest() {
        pushingService.pushOverflowMetrics(
            PartnerDto.builder().id(1L).build(),
            NotificationCounterDto.builder()
                .date(LocalDate.of(2021, 10, 10))
                .capacityId(10L)
                .count(123L)
                .threshold(100L)
                .build()
        );
        verify(solomonClient).push(contentCaptor.capture());
        softly.assertThat(contentCaptor.getValue())
            .is(getCondition(
                extractFileContent("json/sensor/capacity_overflow_sensor.json"),
                JSONCompareMode.STRICT
            ));
    }

    @Test
    @DisplayName("Пуш сенсора на размер очереди dbQueue")
    void pushQueuesSizeTest() {
        pushingService.pushQueuesSize(Map.of(
            "QUEUE_1", 10L,
            "QUEUE_2", 20L,
            "QUEUE_3", 30L
        ));
        verify(solomonClient).push(contentCaptor.capture());
        softly.assertThat(contentCaptor.getValue())
            .is(getCondition(
                extractFileContent("json/sensor/db_queue_size.json"),
                JSONCompareMode.NON_EXTENSIBLE
            ));
    }
}
