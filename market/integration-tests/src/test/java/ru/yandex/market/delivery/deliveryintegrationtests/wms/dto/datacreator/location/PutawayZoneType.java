package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

public enum PutawayZoneType {
    /**
     * Тип не задан
     */
    UNDEFINED("UNDEFINED"),
    TRANSPORTER("TRANSPORTER"),
    BBXD_SHIP("BBXD_SHIP"),
    BBXD_SORTER("BBXD_SORTER");

    private final String value;

    PutawayZoneType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
