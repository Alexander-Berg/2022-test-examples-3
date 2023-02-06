package ru.yandex.market.wms.common.spring.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.dto.OrdersToBoxesDto;
import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Loc;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class BbxdServiceTest extends IntegrationTest {

    @Autowired
    private BbxdService bbxdService;

    @Autowired
    private BbxdShortService bbxdShortService;

    @Test
    @DatabaseSetup("/db/service/bbxd-service/map-boxes-to-orders/1/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/map-boxes-to-orders/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void mapBoxesToOrders1() {
        OrdersToBoxesDto mapping = bbxdService.mapBoxesToOrders(List.of("BOX1"), "DRP123");
        assertions.assertThat(mapping.getBoxToOrder()).containsExactly(Map.entry("BOX1", "ORD0001"));
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/map-boxes-to-orders/2/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/map-boxes-to-orders/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void mapBoxesToOrders2() {
        OrdersToBoxesDto mapping = bbxdService.mapBoxesToOrders(List.of("BOX1", "BOX2"), "DRP123");
        assertions.assertThat(mapping.getBoxToOrder()).containsOnlyKeys("BOX1", "BOX2");
        assertions.assertThat(mapping.getBoxToOrder()).containsValues("ORD0001", "ORD0002");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/map-boxes-to-orders/6/db.xml")
    void mapBoxesToOrders6() {
        OrdersToBoxesDto mapping = bbxdService.mapBoxesToOrders(List.of("BOX1", "BOX2"), "DRP123");
        assertions.assertThat(mapping.getBoxToOrder())
                .containsOnly(Map.entry("BOX1", "ORD0001"), Map.entry("BOX2", "ORD0001"));
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/map-boxes-to-orders/8/db.xml")
    void mapBoxesToOrders8() {
        OrdersToBoxesDto mapping = bbxdService.mapBoxesToOrders(List.of("BOX1"), "DRP123");
        assertions.assertThat(mapping.getBoxToOrder()).containsExactly(Map.entry("BOX1", "ORD0001"));
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/get-receipt/1/db.xml")
    void getReceipt1() {
        String receipt = bbxdService.getReceipt(List.of("BOX1", "BOX2")).get();
        assertions.assertThat(receipt).isEqualTo("0000012345");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/get-receipt/2/db.xml")
    void getReceipt2() {
        assertions.assertThat(bbxdService.getReceipt(List.of("BOX1", "BOX2"))).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/get-box-sku-id/1/db.xml")
    void getBoxSkuId1() {
        SkuId skuId = bbxdService.getBoxSkuId(List.of("BOX1", "BOX2")).get();
        assertions.assertThat(skuId.getSku()).isEqualTo("ROV0000000000000000358");
        assertions.assertThat(skuId.getStorerKey()).isEqualTo("465852");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/get-box-sku-id/2/db.xml")
    void getBoxSkuId2() {
        assertions.assertThat(bbxdService.getBoxSkuId(List.of("BOX1", "BOX2"))).isEmpty();
    }

    @Test
    void getSortedLocs1() {
        List<Loc> sortedDropLocs = bbxdService.getSortedLocs(List.of(
                Loc.builder().loc("DROP2").logicalLocation("2").build(),
                Loc.builder().loc("DROP3").logicalLocation("1").build(),
                Loc.builder().loc("DROP1").logicalLocation("").build()), true);
        assertions.assertThat(sortedDropLocs.get(0).getLoc()).isEqualTo("DROP3");
        assertions.assertThat(sortedDropLocs.get(1).getLoc()).isEqualTo("DROP2");
        assertions.assertThat(sortedDropLocs.get(2).getLoc()).isEqualTo("DROP1");
    }

    @Test
    void getSortedLocs2() {
        List<Loc> sortedDropLocs = bbxdService.getSortedLocs(List.of(
                Loc.builder().loc("DROP2").logicalLocation("2").build(),
                Loc.builder().loc("DROP3").logicalLocation("1").build(),
                Loc.builder().loc("DROP1").logicalLocation("").build()), false);
        assertions.assertThat(sortedDropLocs.get(0).getLoc()).isEqualTo("DROP1");
        assertions.assertThat(sortedDropLocs.get(1).getLoc()).isEqualTo("DROP2");
        assertions.assertThat(sortedDropLocs.get(2).getLoc()).isEqualTo("DROP3");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/move-order-qty-and-status/1/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/move-order-qty-and-status/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void moveOrderQtyAndStatus1() {
        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey("ORD0001")
                .orderLineNumber("00001")
                .openQty(BigDecimal.valueOf(6))
                .pickedQty(BigDecimal.valueOf(3))
                .shippedQty(BigDecimal.ZERO)
                .sku("ROV0000000000000000358")
                .storerKey("465852")
                .status(OrderStatus.PACKED)
                .build();
        bbxdShortService.moveOrderQtyAndStatus(orderDetail, "TEST");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/move-order-qty-and-status/2/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/move-order-qty-and-status/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void moveOrderQtyAndStatus2() {
        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey("ORD0001")
                .orderLineNumber("00001")
                .openQty(BigDecimal.valueOf(6))
                .pickedQty(BigDecimal.valueOf(3))
                .shippedQty(BigDecimal.ZERO)
                .sku("ROV0000000000000000358")
                .storerKey("465852")
                .status(OrderStatus.PACKED)
                .build();
        bbxdShortService.moveOrderQtyAndStatus(orderDetail, "TEST");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/move-order-qty-and-status/3/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/move-order-qty-and-status/3/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void moveOrderQtyAndStatus3() {
        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey("ORD0001")
                .orderLineNumber("00001")
                .openQty(BigDecimal.valueOf(6))
                .pickedQty(BigDecimal.valueOf(3))
                .shippedQty(BigDecimal.ZERO)
                .sku("ROV0000000000000000358")
                .storerKey("465852")
                .status(OrderStatus.PACKED)
                .build();
        bbxdShortService.moveOrderQtyAndStatus(orderDetail, "TEST");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/move-order-qty-and-status/4/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/move-order-qty-and-status/4/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void moveOrderQtyAndStatus4() {
        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey("ORD0001")
                .orderLineNumber("00001")
                .openQty(BigDecimal.valueOf(6))
                .pickedQty(BigDecimal.valueOf(0))
                .shippedQty(BigDecimal.ZERO)
                .sku("ROV0000000000000000358")
                .storerKey("465852")
                .status(OrderStatus.PACKED)
                .build();
        bbxdShortService.moveOrderQtyAndStatus(orderDetail, "TEST");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/move-order-qty-and-status/5/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/move-order-qty-and-status/5/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void moveOrderQtyAndStatus5() {
        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey("ORD0001")
                .orderLineNumber("00001")
                .openQty(BigDecimal.valueOf(6))
                .pickedQty(BigDecimal.valueOf(0))
                .shippedQty(BigDecimal.ZERO)
                .sku("ROV0000000000000000358")
                .storerKey("465852")
                .status(OrderStatus.PACKED)
                .build();
        bbxdShortService.moveOrderQtyAndStatus(orderDetail, "TEST");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/move-order-qty-and-status/6/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/move-order-qty-and-status/6/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void moveOrderQtyAndStatus6() {
        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey("ORD0001")
                .orderLineNumber("00001")
                .openQty(BigDecimal.valueOf(6))
                .pickedQty(BigDecimal.valueOf(0))
                .shippedQty(BigDecimal.valueOf(2))
                .sku("ROV0000000000000000358")
                .storerKey("465852")
                .status(OrderStatus.PART_SHIPPED)
                .build();
        bbxdShortService.moveOrderQtyAndStatus(orderDetail, "TEST");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/move-order-qty-and-status/7/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/move-order-qty-and-status/7/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void moveOrderQtyAndStatus7() {
        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey("ORD0001")
                .orderLineNumber("00001")
                .openQty(BigDecimal.valueOf(6))
                .pickedQty(BigDecimal.valueOf(2))
                .shippedQty(BigDecimal.valueOf(2))
                .sku("ROV0000000000000000358")
                .storerKey("465852")
                .status(OrderStatus.PART_SHIPPED)
                .build();
        bbxdShortService.moveOrderQtyAndStatus(orderDetail, "TEST");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/move-order-qty-and-status/8/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/move-order-qty-and-status/8/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void moveOrderQtyAndStatus8() {
        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey("ORD0001")
                .orderLineNumber("00001")
                .openQty(BigDecimal.valueOf(6))
                .pickedQty(BigDecimal.valueOf(0))
                .shippedQty(BigDecimal.ZERO)
                .sku("ROV0000000000000000358")
                .storerKey("465852")
                .status(OrderStatus.PACKED)
                .build();
        bbxdShortService.moveOrderQtyAndStatus(orderDetail, "TEST");
    }

    /**
     * 2 штуки без уитов, шипнули 0. в заказах ожидалось 3, ничего не делаем
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/1/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes1() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    /**
     * 2 штуки без уитов, шипнули 2. в заказах ожидалось 2, переводим статусы
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/2/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes2() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    /**
     * 2 штуки без уитов, один уит. в заказах ожидалось 3, шипнули 2, шортим одну штуку
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/3/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/3/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes3() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    /**
     * 2 штуки без уитов, один уит, в лостах 2, шипнули 0. в заказах ожидалось 3, шортим три штуки
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/4/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/4/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes4() {
        bbxdShortService.shortReceipt("0000012345", Map.of(SkuId.of("465852", "ROV0000000000000000358"), 2), null);
    }

    /**
     * два заказа. в одном ожилось 3, все шипнули, в другом ожилось 2 и шипнули 1. без уитов приняли 4, один по уитам
     * шортим одну штуку во втором заказе
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/5/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/5/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes5() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    /**
     * два заказа. в одном ожилось 3, шипнули 6, в другом ожилось 3 и шипнули 0. без уитов приняли 6
     * шортим все во втором заказе
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/6/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/6/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes6() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    /**
     * 2 штуки без уитов, шипнули 2. в заказах ожидалось 2, отменяем активные задание на сортировку
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/7/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/7/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes7() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    /**
     * 2 штуки без уитов, шипнули 0. заказов нет, ничего не делаем
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/8/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/8/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes8() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    /**
     * 1 штука без уитов, шипнули 1. правим все заказы
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/9/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/9/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes9() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    /**
     * отменяем лишнюю ску в заказе
     */
    @Test
    @DatabaseSetup("/db/service/bbxd-service/short-boxes/10/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/short-boxes/10/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shortBoxes10() {
        bbxdShortService.shortReceipt("0000012345", Collections.emptyMap(), null);
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/finish-bbxd-task/1/before.xml")
    @ExpectedDatabase(value = "/db/service/bbxd-service/finish-bbxd-task/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishBbxdTask1() {
        bbxdService.finishBbxdTask(1, "0000000201", "DROP");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/choose-drop/1/db.xml")
    void chooseDrop1() {
        String palletId = "PLT123";
        assertions.assertThatThrownBy(() -> bbxdService.chooseDrop(null, palletId, null, true))
                .hasMessageContaining(
                        "400 BAD_REQUEST \"Паллета PLT123 находится в зоне 1 типа UNDEFINED, отличного от BBXD_SORTER");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/choose-drop/2/db.xml")
    void chooseDrop2() {
        String palletId = "PLT123";
        assertions.assertThatThrownBy(
                        () -> bbxdService.chooseDrop(List.of("BOX1", "BOX2"), palletId,
                                SkuId.of("465852", "ROV0000000000000000358"),
                                true))
                .hasMessageContaining(
                        "400 BAD_REQUEST \"В зоне 1 с паллетой PLT123 нет " +
                                "подходящих дропок для сортировки на направления CARRIER-02");
    }

    @Test
    @DatabaseSetup("/db/service/bbxd-service/choose-drop/3/db.xml")
    void chooseDrop3() {
        String palletId = "PLT123";
        assertions.assertThatThrownBy(
                        () -> bbxdService.chooseDrop(List.of("BOX1", "BOX2"), palletId,
                                SkuId.of("465852", "ROV0000000000000000358"),
                                true))
                .hasMessageContaining(
                        "400 BAD_REQUEST \"Нет потребности в сортировке " +
                                "товара ROV0000000000000000358 465852 с паллеты PLT123 для поставки 0000012345");
    }
}
