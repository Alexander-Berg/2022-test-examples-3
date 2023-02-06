package ru.yandex.market.wms.autostart.dao;

import ru.yandex.market.wms.common.spring.dao.entity.Order;

public interface WithdrawalOrdersTestData {

    static Order withdrawalOrder900001003() {
        return Order.builder()
                .orderKey("900001003")
                .externalOrderKey("WMS900001003")
                .status("02")
                .build();
    }
}
