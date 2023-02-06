package ru.yandex.market.logistics.logistics4shops.service.transport_manager;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Интеграционные тесты на TransportManagerService")
class TransportManagerServiceTest extends AbstractIntegrationTest {
    @Autowired
    private TransportManagerClient transportManagerClient;

    @Autowired
    private TransportManagerService transportManagerService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(transportManagerClient);
    }

    @Test
    @DisplayName("Успешное получение отгрузки")
    void getTransportationSuccess() {
        TransportationDto expectedTransportation = new TransportationDto().setId(1L);

        when(transportManagerClient.getTransportation(1L)).thenReturn(Optional.of(expectedTransportation));

        TransportationDto actualTransportation = transportManagerService.getTransportation(1L);

        softly.assertThat(actualTransportation).usingRecursiveComparison().isEqualTo(expectedTransportation);
        verify(transportManagerClient).getTransportation(1L);
    }

    @Test
    @DisplayName("Получение несуществующей отгрузки")
    void transportationNotFound() {
        softly.assertThatThrownBy(() -> transportManagerService.getTransportation(1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [SHIPMENT] with id [1]");
        verify(transportManagerClient).getTransportation(1L);
    }

    @Test
    @DisplayName("Ошибка при получении отгрузки")
    void getTransportationError() {
        when(transportManagerClient.getTransportation(any())).thenThrow(new RuntimeException("TM is unreachable"));
        softly.assertThatThrownBy(() -> transportManagerService.getTransportation(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("TM is unreachable");
        verify(transportManagerClient).getTransportation(1L);
    }
}
