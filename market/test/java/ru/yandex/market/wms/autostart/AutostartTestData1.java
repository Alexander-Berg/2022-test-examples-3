package ru.yandex.market.wms.autostart;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.wms.autostart.autostartlogic.SortingStationTestData;
import ru.yandex.market.wms.common.spring.dao.entity.Order;

public interface AutostartTestData1 {

    String CARRIER_CODE_100 = "100";
    String CARRIER_CODE_101 = "101";
    String CARRIER_CODE_102 = "102";


    static List<String> exampleOrderKeys() {
        return exampleOrders().stream().map(Order::getOrderKey).collect(Collectors.toList());
    }

    static List<String> exampleWithdrawalOrdersKeys() {
        return exampleWithdrawalOrders().stream().map(Order::getOrderKey).collect(Collectors.toList());
    }

    static List<Order> exampleOrders() {
        return Arrays.asList(
                orderB000001003(),
                orderB000003001(),
                orderB000001004(),
                orderB000002002(),
                orderB000003002(),
                orderB000005002(),
                orderB000003003(),
                orderB000003004(),
                orderB000004001(),
                orderB000006001(),
                orderB000006002()
        );
    }

    static List<Order> exampleWithdrawalOrders() {
        return Arrays.asList(
                orderB000006001(),
                orderB000006002()
        );
    }

    // =========================================================================================
    // Orders that are already assigned to some sort stations (1, 2, 5)
    // =========================================================================================

    static Order orderB000001001() {
        return Order.builder()
                .orderKey("000001001")
                .externalOrderKey("WMSB000001001")
                .status("55")
                .door(SortingStationTestData.DOOR_S_01)
                .carrierCode(CARRIER_CODE_100)
                .build();
    }

    static Order orderB000001002() {
        return Order.builder()
                .orderKey("000001002")
                .externalOrderKey("WMSB000001002")
                .status("55")
                .door(SortingStationTestData.DOOR_S_01)
                .carrierCode(CARRIER_CODE_100)
                .build();
    }

    static Order orderB000002001() {
        return Order.builder()
                .orderKey("000002001")
                .externalOrderKey("WMSB000002001")
                .status("55")
                .door(SortingStationTestData.DOOR_S_02)
                .carrierCode(CARRIER_CODE_100)
                .build();
    }

    static Order orderB000005001() {
        return Order.builder()
                .orderKey("000005001")
                .externalOrderKey("WMSB000005001")
                .status("02")
                .door(SortingStationTestData.DOOR_S_05)
                .carrierCode(CARRIER_CODE_100)
                .build();
    }


    // =========================================================================================
    // CARRIER CODE 100
    // =========================================================================================

    // 2 orders that will be added to sort station #1 (non-DEFAULT, putWallsPerBatch:1, ordersPerPutWall:4)
    // (2 orders are already assigned to station #1)

    static Order orderB000001003() {
        return Order.builder()
                .orderKey("000001003")
                .externalOrderKey("WMSB000001003")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-02T10:00:00+03:00"))
                .build();
    }

    static Order orderB000001004() {
        return Order.builder()
                .orderKey("000001004")
                .externalOrderKey("WMSB000001004")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-02T10:00:00+03:00"))
                .build();
    }

    // 1 order that will be added to sort station #2 (non-DEFAULT)
    // (1 order is already assigned to station #2)

    static Order orderB000002002() {
        return Order.builder()
                .orderKey("000002002")
                .externalOrderKey("WMSB000002002")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-02T10:00:00+03:00"))
                .build();
    }

    // =========================================================================================
    // CARRIER CODE 101
    // =========================================================================================

    // 4 orders that can be added to sort station #3 (DEFAULT)
    // (station 3 is free)

    static Order orderB000003001() {
        return Order.builder()
                .orderKey("000003001")
                .externalOrderKey("WMSB000003001")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-02T10:00:00+03:00"))
                .build();
    }

    static Order orderB000003002() {
        return Order.builder()
                .orderKey("000003002")
                .externalOrderKey("WMSB000003002")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB000003003() {
        return Order.builder()
                .orderKey("000003003")
                .externalOrderKey("WMSB000003003")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .build();
    }

    static Order orderB000003004() {
        return Order.builder()
                .orderKey("000003004")
                .externalOrderKey("WMSB000003004")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .build();
    }

    // 1 order that will be added to sort station #4
    // (station 4 is free)

    static Order orderB000004001() {
        return Order.builder()
                .orderKey("000004001")
                .externalOrderKey("WMSB000004001")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .build();
    }

    // =========================================================================================
    // CARRIER CODE 102
    // =========================================================================================

    // 1 order that will be added to sort station #5

    static Order orderB000005002() {
        return Order.builder()
                .orderKey("000005002")
                .externalOrderKey("WMSB000005002")
                .status("02")
                .carrierCode(CARRIER_CODE_102)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .build();
    }

    // =========================================================================================
    // ORDER TYPE 101
    // =========================================================================================

    static Order orderB000006001() {
        return Order.builder()
                .orderKey("000006001")
                .externalOrderKey("WMSB000006001")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .type("101")
                .build();
    }

    // =========================================================================================
    // ORDER TYPE 102
    // =========================================================================================

    static Order orderB000006002() {
        return Order.builder()
                .orderKey("000006002")
                .externalOrderKey("WMSB000006002")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .type("102")
                .build();
    }
}
