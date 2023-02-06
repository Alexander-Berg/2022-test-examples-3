package ru.yandex.market.checkout.util.report;

import java.time.Clock;
import java.time.LocalDate;

import ru.yandex.market.common.report.model.LocalDeliveryOption;

public enum ShipmentDayAndDateOption {
    ONLY_DAY,
    ONLY_DATE,
    BOTH;

    public boolean useDay() {
        return this == ONLY_DAY || this == BOTH;
    }

    public boolean useDate() {
        return this == ONLY_DATE || this == BOTH;
    }

    public void setupDeliveryOption(LocalDeliveryOption option, Clock clock, int shipmentDay) {
        option.setShipmentDay(useDay() ? shipmentDay : null);
        if (useDate()) {
            option.setShipmentDate(LocalDate.now(clock).plusDays(shipmentDay));
        }
    }
}
