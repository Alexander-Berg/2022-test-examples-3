package ru.yandex.market.tpl.core.domain.pickup.generator;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;

public final class PickupPointGenerator {

    private PickupPointGenerator() {

    }

    public static PickupPoint generatePickupPoint(Long logisticPointId) {
        PickupPoint pickupPoint = new PickupPoint();
        pickupPoint.setCode("test");
        pickupPoint.setPartnerSubType(PartnerSubType.LOCKER);
        pickupPoint.setType(PickupPointType.LOCKER);
        pickupPoint.setLogisticPointId(logisticPointId);
        return pickupPoint;
    }
}
