package ru.yandex.market.checkout.checkouter.order.changerequest;

import ru.yandex.market.checkout.util.report.ShipmentDayAndDateOption;

public class OrderEditTestWithShipmentDate extends OrderEditTest {

    public OrderEditTestWithShipmentDate() {
        this.shipmentDayAndDateOption = ShipmentDayAndDateOption.ONLY_DATE;
    }
}
