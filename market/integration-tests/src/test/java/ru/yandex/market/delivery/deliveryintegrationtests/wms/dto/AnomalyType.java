package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import java.util.Set;

public enum AnomalyType {
    INCORRECT_REQUIRED_CIS,
    INCORRECT_OPTIONAL_CIS,
    INCORRECT_IMEI,
    INCORRECT_SERIAL_NUMBER;

    public static Set<AnomalyType> getCisAnomalies() {
        return Set.of(INCORRECT_REQUIRED_CIS, INCORRECT_OPTIONAL_CIS);
    }
}
