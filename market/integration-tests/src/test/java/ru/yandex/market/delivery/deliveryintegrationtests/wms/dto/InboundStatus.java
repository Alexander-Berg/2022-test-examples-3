package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

public enum InboundStatus {
    UNKNOWN("-1"),    // Неизвестный статус
    CANCELLED("0"),   // Отменена
    CREATED("1"),     // Создана
    ARRIVED("20"),    // Прибыла на склад
    ACCEPTANCE("30"), // Приемка
    ACCEPTED("40"),   // Оприходована
    SHIPPED("50");    // Отгружено

    private final String id;

    InboundStatus(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
