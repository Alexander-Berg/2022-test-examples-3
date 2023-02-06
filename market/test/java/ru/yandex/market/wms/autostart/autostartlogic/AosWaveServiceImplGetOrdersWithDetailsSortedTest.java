package ru.yandex.market.wms.autostart.autostartlogic;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.autostartlogic.service.AosWaveServiceImpl;
import ru.yandex.market.wms.common.spring.dao.entity.AssigmentType;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


class AosWaveServiceImplGetOrdersWithDetailsSortedTest extends AutostartIntegrationTest {

    private static final String CARRIER_CODE_100 = "100";
    private static final String CARRIER_CODE_101 = "101";
    private static final String CARRIER_CODE_102 = "102";

    private static List<Order> getTestOrders() {
        return Arrays.asList(
                order000001003(),
                order000003001(),
                order000001004(),
                order000002002(),
                order000003002(),
                order000005002(),
                order000003003(),
                order000003004(),
                order000004001()
        );
    }

    private static Order order000001003() {
        return Order.builder()
                .orderKey("000001003")
                .externalOrderKey("WMSB000001003")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-03T10:00:01+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-03T10:00:01+03:00"))
                .build();
    }

    private static Order order000003001() {
        return Order.builder()
                .orderKey("000003001")
                .externalOrderKey("WMSB000003001")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-03T10:00:00+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-03T10:00:00+03:00"))
                .build();
    }

    private static Order order000001004() {
        return Order.builder()
                .orderKey("000001004")
                .externalOrderKey("WMSB000001004")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-02T10:00:00+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-02T10:00:00+03:00"))
                .build();
    }

    private static Order order000002002() {
        return Order.builder()
                .orderKey("000002002")
                .externalOrderKey("WMSB000002002")
                .status("02")
                .carrierCode(CARRIER_CODE_100)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-02T12:00:00+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-02T12:00:00+03:00"))
                .build();
    }

    private static Order order000003002() {
        return Order.builder()
                .orderKey("000003002")
                .externalOrderKey("WMSB000003002")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-01T10:20:00+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:20:00+03:00"))
                .build();
    }

    private static Order order000005002() {
        return Order.builder()
                .orderKey("000005002")
                .externalOrderKey("WMSB000005002")
                .status("02")
                .carrierCode(CARRIER_CODE_102)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-01T11:00:01+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T11:00:01+03:00"))
                .build();
    }

    private static Order order000003003() {
        return Order.builder()
                .orderKey("000003003")
                .externalOrderKey("WMSB000003003")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T10:00:00+03:00"))
                .build();
    }

    private static Order order000003004() {
        return Order.builder()
                .orderKey("000003004")
                .externalOrderKey("WMSB000003004")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-01T11:20:00+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T11:20:00+03:00"))
                .build();
    }

    private static Order order000004001() {
        return Order.builder()
                .orderKey("000004001")
                .externalOrderKey("WMSB000004001")
                .status("02")
                .carrierCode(CARRIER_CODE_101)
                .scheduledShipDate(OffsetDateTime.parse("2020-01-01T11:00:00+03:00"))
                .shipmentDateTime(OffsetDateTime.parse("2020-01-01T11:00:00+03:00"))
                .build();
    }

    private static List<OrderDetail> getTestOrderDetails() {
        return Arrays.asList(
                orderDetail_000001003_00001_ROV1_2(),
                orderDetail_000001004_00001_ROV1_1(),
                orderDetail_000001003_00002_ROV2_3(),
                orderDetail_000002002_00001_ROV2_1(),
                orderDetail_000003001_00001_ROV3_1(),
                orderDetail_000003003_00001_ROV5_1(),
                orderDetail_000004001_00001_ROV7_1(),
                orderDetail_000001003_00003_ROV9_1(),
                orderDetail_000005002_00001_ROV8_1(),
                orderDetail_000003004_00001_ROV6_1(),
                orderDetail_000003002_00001_ROV4_1()
        );
    }

    static OrderDetail orderDetail_000001003_00001_ROV1_2() {
        return orderDetailTemplate()
                .orderKey("000001003")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000001")
                .openQty(new BigDecimal("2.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000001003_00002_ROV2_3() {
        return orderDetailTemplate()
                .orderKey("000001003")
                .orderLineNumber("00002")
                .storerKey("100")
                .sku("ROV0000000000000000002")
                .openQty(new BigDecimal("3.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000001003_00003_ROV9_1() {
        return orderDetailTemplate()
                .orderKey("000001003")
                .orderLineNumber("00003")
                .storerKey("100")
                .sku("ROV0000000000000000009")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000001004_00001_ROV1_1() {
        return orderDetailTemplate()
                .orderKey("000001004")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000001")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000002002_00001_ROV2_1() {
        return orderDetailTemplate()
                .orderKey("000002002")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000002")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000003001_00001_ROV3_1() {
        return orderDetailTemplate()
                .orderKey("000003001")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000003")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000003002_00001_ROV4_1() {
        return orderDetailTemplate()
                .orderKey("000003002")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000004")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000003003_00001_ROV5_1() {
        return orderDetailTemplate()
                .orderKey("000003003")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000005")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000003004_00001_ROV6_1() {
        return orderDetailTemplate()
                .orderKey("000003004")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000006")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000004001_00001_ROV7_1() {
        return orderDetailTemplate()
                .orderKey("000004001")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000007")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail orderDetail_000005002_00001_ROV8_1() {
        return orderDetailTemplate()
                .orderKey("000005002")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000008")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    private static OrderDetail.OrderDetailBuilder orderDetailTemplate() {
        return OrderDetail.builder()
                .isMaster(1)
                .rotation("1")
                .skuRotation("1")
                .packKey("STD")
                .cartonGroup("BC1")
                .shelfLife(BigDecimal.ZERO);
    }

    private static List<OrderWithDetails> getExpectedOrdersWithDetails() {
        return Arrays.asList(
                orderWithDetails(order000003003(), orderDetail_000003003_00001_ROV5_1()),
                orderWithDetails(order000003002(), orderDetail_000003002_00001_ROV4_1()),
                orderWithDetails(order000004001(), orderDetail_000004001_00001_ROV7_1()),
                orderWithDetails(order000005002(), orderDetail_000005002_00001_ROV8_1()),
                orderWithDetails(order000003004(), orderDetail_000003004_00001_ROV6_1()),
                orderWithDetails(order000001004(), orderDetail_000001004_00001_ROV1_1()),
                orderWithDetails(order000002002(), orderDetail_000002002_00001_ROV2_1()),
                orderWithDetails(order000003001(), orderDetail_000003001_00001_ROV3_1()),
                orderWithDetails(
                        order000001003(),
                        orderDetail_000001003_00001_ROV1_2(),
                        orderDetail_000001003_00002_ROV2_3(),
                        orderDetail_000001003_00003_ROV9_1()
                )
        );
    }

    private static OrderWithDetails orderWithDetails(Order o, OrderDetail... details) {
        return OrderWithDetails.builder().order(o).orderDetails(Arrays.asList(details)).build();
    }

    @Test
    void getOrdersWithDetailsSorted() {
        List<OrderWithDetails> result = AosWaveServiceImpl.getOrdersWithDetailsSorted(
                getTestOrders(),
                getTestOrderDetails()
        );

        assertThat(result, is(equalTo(getExpectedOrdersWithDetails())));
    }
}
