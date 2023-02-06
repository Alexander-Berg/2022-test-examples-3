package ru.yandex.market.checkout.checkouter.order.changerequest;

import ru.yandex.market.checkout.util.report.ShipmentDayAndDateOption;

public class OrderEditTestWithShipmentDayAndDate extends OrderEditTest {

    public OrderEditTestWithShipmentDayAndDate() {
        this.shipmentDayAndDateOption = ShipmentDayAndDateOption.BOTH;
    }
}
