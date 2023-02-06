package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

public enum ShipmentType {
    STANDARD("Стандартная"),
    WITHDRAWAL("Изъятие"),
    DUTY("Дежурная");

    private final String value;

    ShipmentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
