package ru.yandex.market.logistics.logistics4shops.service.lms;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Интеграционные тесты на LogisticsPointService")
class LogisticsPointServiceTest extends AbstractIntegrationTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LogisticsPointService logisticsPointService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешное получение логистической точки")
    void getLogisticsPointSuccess() {
        LogisticsPointResponse expectedLogisticsPoint =
            LogisticsPointResponse.newBuilder()
                .id(10000004403L)
                .partnerId(172L)
                .build();

        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(expectedLogisticsPoint));

        LogisticsPointResponse actualLogisticsPoint = logisticsPointService.getLogisticsPoint(1L);

        softly.assertThat(actualLogisticsPoint).usingRecursiveComparison().isEqualTo(expectedLogisticsPoint);
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Получение несуществующей логистической точки")
    void logisticsPointNotFound() {
        softly.assertThatThrownBy(() -> logisticsPointService.getLogisticsPoint(1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [LOGISTICS_POINT] with id [1]");
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Ошибка при получении логистической точки")
    void getLogisticsPointError() {
        when(lmsClient.getLogisticsPoint(any())).thenThrow(new RuntimeException("LMS is unreachable"));
        softly.assertThatThrownBy(() -> logisticsPointService.getLogisticsPoint(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("LMS is unreachable");
        verify(lmsClient).getLogisticsPoint(1L);
    }
}
