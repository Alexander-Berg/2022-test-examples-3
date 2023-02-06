package ru.yandex.market.delivery.transport_manager.service.event.ffwf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;

class RequestStatusConverterTest {
    @DisplayName("Конвертация любого transportation unit status в calendaring status не вызывает exception-а")
    @Test
    void convertToCalendaringSlotStatus() {
        for (TransportationUnitStatus status : TransportationUnitStatus.values()) {
            RequestStatusConverter.convertToCalendaringSlotStatus(status);
        }
    }
}
