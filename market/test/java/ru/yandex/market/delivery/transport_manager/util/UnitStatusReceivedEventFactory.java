package ru.yandex.market.delivery.transport_manager.util;

import java.time.LocalDateTime;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.unit.status.UnitStatusReceivedEvent;

@UtilityClass
public class UnitStatusReceivedEventFactory {
    private static final long REQUEST_ID = 123L;
    private static final long UNIT_ID = 2L;

    public Transportation transportation(TransportationType type) {
        return new Transportation()
            .setId(1L)
            .setTransportationType(type);
    }

    public TransportationUnit unit(TransportationUnitType type, String externalId) {
        return new TransportationUnit()
            .setId(UNIT_ID)
            .setType(type)
            .setRequestId(REQUEST_ID)
            .setExternalId(externalId)
            .setPartnerId(3L);
    }

    public UnitStatusReceivedEvent event(
        Transportation transportation,
        TransportationUnit unit,
        boolean isFirstTime
    ) {
        return new UnitStatusReceivedEvent(
            new Object(),
            unit,
            transportation,
            unit.getStatus(),
            TransportationUnitStatus.ACCEPTED,
            LocalDateTime.now(),
            isFirstTime
        );
    }
}
