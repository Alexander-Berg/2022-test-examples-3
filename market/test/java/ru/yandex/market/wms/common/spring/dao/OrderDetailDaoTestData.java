package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;

import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;

public interface OrderDetailDaoTestData {

    static OrderDetail orderB00000100100001() {
        return OrderDetail.builder()
                .orderKey("B000001001")
                .orderLineNumber("00001")
                .storerKey("465852")
                .sku("ROV0000000000000001456")
                .openQty(new BigDecimal("0.00000"))
                .isMaster(1)
                .build();
    }

    static OrderDetail orderB00000100100002() {
        return OrderDetail.builder()
                .orderKey("B000001001")
                .orderLineNumber("00002")
                .storerKey("465852")
                .sku("ROV0000000000000001459")
                .openQty(new BigDecimal("0.00000"))
                .isMaster(1)
                .build();
    }

    static OrderDetail orderB00000100200001() {
        return OrderDetail.builder()
                .orderKey("B000001002")
                .orderLineNumber("00001")
                .storerKey("465852")
                .sku("ROV0000000000000001456")
                .openQty(new BigDecimal("0.00000"))
                .isMaster(1)
                .build();
    }
}
