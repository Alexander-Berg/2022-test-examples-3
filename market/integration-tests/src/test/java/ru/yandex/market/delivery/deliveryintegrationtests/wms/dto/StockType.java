package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.Getter;

@Getter
public enum StockType {

    FIT(10),
    EXPIRED(30),
    DEFECT(50),
    PLAN_UTILIZATION(80);

    private final int code;

    StockType(int code) {
        this.code = code;
    }

    public String getValue() {
        return String.valueOf(code);
    }
}
