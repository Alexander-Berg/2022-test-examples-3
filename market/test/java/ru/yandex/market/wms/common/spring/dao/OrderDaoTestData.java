package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ru.yandex.market.wms.common.spring.dao.entity.Order;

public interface OrderDaoTestData {

    String DOOR_S_01 = "S01";
    String DOOR_S_02 = "S02";
    String DOOR_S_03 = "S03";
    String DOOR_S_04 = "S04";
    String DOOR_S_05 = "S05";
    String CARRIER_CODE_100 = "100";

    // Orders that are already assigned to some sort stations

    static Order orderB000001001() {
        return Order.builder()
                .orderKey("B000001001")
                .externalOrderKey("WMSB000001001")
                .status("55")
                .door(DOOR_S_01)
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB000001002() {
        return Order.builder()
                .orderKey("B000001002")
                .externalOrderKey("WMSB000001002")
                .status("55")
                .door(DOOR_S_01)
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB000002001() {
        return Order.builder()
                .orderKey("B000002001")
                .externalOrderKey("WMSB000002001")
                .status("55")
                .door(DOOR_S_02)
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB000005001() {
        return Order.builder()
                .orderKey("B000005001")
                .externalOrderKey("WMSB000005001")
                .status("02")
                .door(DOOR_S_05)
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }


    // 2 orders that will be added to sort station #1

    static Order orderB000001003() {
        return Order.builder()
                .orderKey("B000001003")
                .externalOrderKey("WMSB000001003")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB000001004() {
        return Order.builder()
                .orderKey("B000001004")
                .externalOrderKey("WMSB000001004")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    // 3 orders that will be added to sort station #2

    static Order orderB000002002() {
        return Order.builder()
                .orderKey("B000002002")
                .externalOrderKey("WMSB000002002")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB000002003() {
        return Order.builder()
                .orderKey("B000002003")
                .externalOrderKey("WMSB000002003")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB000002004() {
        return Order.builder()
                .orderKey("B000002004")
                .externalOrderKey("WMSB000002004")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    // 1 order that will be added to sort station #3

    static Order orderB000003001() {
        return Order.builder()
                .orderKey("B000003001")
                .externalOrderKey("WMSB000003001")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB100003001() {
        return Order.builder()
                .orderKey("B100003001")
                .externalOrderKey("WMSB100003001")
                .status("55")
                .carrierCode("CARRIER_CODE_100")
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .carrierName("PickPoint")
                .totalqty(BigDecimal.ONE)
                .type("14")
                .storerKey("1559")
                .build();
    }

    static Order orderB100003002() {
        return Order.builder()
                .orderKey("B100003002")
                .externalOrderKey("WMSB100003002")
                .status("55")
                .carrierCode("CARRIER_CODE_100")
                .scheduledShipDate(OffsetDateTime.parse("2020-12-01T10:00:00+03:00"))
                .carrierName("PickPoint")
                .totalqty(BigDecimal.ONE)
                .type("14")
                .storerKey("1559")
                .maxAbsentItemsPricePercent(BigDecimal.ONE)
                .build();
    }
}
