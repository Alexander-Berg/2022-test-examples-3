package ru.yandex.market.wms.autostart.autostartlogic.orderbatching;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.OrderItemOutOfStockException;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.PickSkuLocation;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000001003;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000001004;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderDetailTestData.b00000100300001ROV13;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderDetailTestData.b00000100300002ROV23;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderDetailTestData.b00000100400001ROV11;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.invROV0000000000000000001C4100001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.invROV0000000000000000001C4101001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.invROV0000000000000000001C4110003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004SkuDups;

public class InventoryRemainsTest {
    static List<BiFunction<InventoryRemains, OrderWithDetails, List<PickSku>>> pickOrReserve() {
        List<BiFunction<InventoryRemains, OrderWithDetails, List<PickSku>>> methods = new ArrayList<>();
        methods.add(InventoryRemains::pick);
        methods.add((inventoryRemains, order) -> {
                    List<PickSku> pickSkus = inventoryRemains.getAndReserve(order);
                    inventoryRemains.acceptReserved();
                    return pickSkus;
                }
        );
        return methods;
    }

    static OrderWithDetails owd(Order o, List<OrderDetail> details) {
        return OrderWithDetails.builder().order(o).orderDetails(details).build();
    }

    @Test
    void shouldNotMergeLocationsWithSameLocDiffLotIds() {
        OrderInventoryDetail inventory1 = OrderInventoryDetail.builder()
                .skuId(new SkuId("1", "1"))
                .location(PickSkuLocation.builder().zone("zone").logicalLocation("1").loc("loc").lot("lot1").build())
                .skuProperties(SkuProperties.builder().build())
                .qty(1)
                .build();
        OrderInventoryDetail inventory2 = OrderInventoryDetail.builder()
                .skuId(new SkuId("1", "1"))
                .location(PickSkuLocation.builder().zone("zone").logicalLocation("1").loc("loc").lot("lot2").build())
                .skuProperties(SkuProperties.builder().build())
                .qty(1)
                .build();
        InventoryRemains inventoryRemains = new InventoryRemains(listOf(inventory1, inventory2));

        OrderWithDetails order = OrderWithDetails.builder()
                .order(Order.builder().orderKey("1").build())
                .orderDetails(listOf(
                        OrderDetail.builder().orderKey("1").storerKey("1").sku("1").openQty(new BigDecimal(2))
                                .build()))
                .build();

        assertTrue(inventoryRemains.isFitted(order));
        assertTrue(inventoryRemains.isEnoughQty(order));
        List<PickSku> pickSkus = inventoryRemains.get(order);
        assertEquals(2, pickSkus.size());
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldNotMergeInventoriesWithSameLocationDifferentInProperties(
            BiFunction<InventoryRemains, OrderWithDetails, List<PickSku>> function) {
        String orderKey = "1";
        SkuId skuId = new SkuId("1", "1");
        OrderInventoryDetail inventory1 = OrderInventoryDetail.builder()
                .skuId(skuId)
                .location(PickSkuLocation.builder().zone("zone").logicalLocation("1").loc("loc").lot("lot1").build())
                .skuProperties(SkuProperties.builder().packKey("p1").build())
                .qty(1)
                .build();
        OrderInventoryDetail inventory2 = OrderInventoryDetail.builder()
                .skuId(skuId)
                .location(PickSkuLocation.builder().zone("zone").logicalLocation("1").loc("loc").lot("lot1").build())
                .skuProperties(SkuProperties.builder().packKey("p2").build())
                .qty(1)
                .build();
        InventoryRemains inventoryRemains = new InventoryRemains(listOf(inventory1, inventory2));

        OrderWithDetails order = OrderWithDetails.builder()
                .order(Order.builder().orderKey(orderKey).build())
                .orderDetails(listOf(
                        OrderDetail.builder().orderKey("1").storerKey("1").sku("1").openQty(new BigDecimal(2))
                                .build()))
                .build();

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);

        //then
        PickSku actualPick1 = actualPicks.get(0);
        PickSku actualPick2 = actualPicks.get(1);
        assertEquals(2, actualPicks.size());
        assertEquals(1, actualPick1.getQty());
        assertEquals(1, actualPick2.getQty());
        assertNotEquals(actualPick1.getSkuProperties().getPackKey(), actualPick2.getSkuProperties().getPackKey());
        assertTrue(inventoryRemains.getRemains().isEmpty());
        assertEquals(2, inventoryRemains.getSavedPicks(orderKey, skuId).size());
    }

    @Test
    void shouldReturnEnoughItems() {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(3));
        OrderWithDetails order = owd(orderB000001004(), listOf(b00000100400001ROV11()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order));
        assertTrue(inventoryRemains.isEnoughQty(order));

        //when
        List<PickSku> actualPicks = inventoryRemains.get(order);

        //then
        PickSku actualPick1 = actualPicks.get(0);
        assertEquals(1, actualPicks.size());
        assertEquals(1, actualPick1.getQty());
        assertEquals(0, inventoryRemains.getSavedPicks(orderKey, skuId).size());
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldReturnEnoughItemsByConcreteZones(
            BiFunction<InventoryRemains, OrderWithDetails, List<PickSku>> function) {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(3));
        OrderWithDetails order = owd(orderB000001004(), listOf(b00000100400001ROV11()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();
        Set<String> currentZones = Collections.singleton(inventory.get(0).getLocation().getZone());

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertTrue(inventoryRemains.isEnoughQty(order, currentZones));

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);

        //then
        PickSku actualPick1 = actualPicks.get(0);
        assertEquals(1, actualPicks.size());
        assertEquals(1, actualPick1.getQty());
        assertEquals(2, inventoryRemains.getRemains().get(0).getQty());
        assertEquals(1, inventoryRemains.getSavedPicks(orderKey, skuId).size());
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldDecreaseItemQtyInInventory(BiFunction<InventoryRemains, OrderWithDetails, List<PickSku>> function) {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(3));
        OrderWithDetails order = owd(orderB000001004(), listOf(b00000100400001ROV11()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order));
        assertTrue(inventoryRemains.isEnoughQty(order));

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);

        //then
        PickSku actualPick1 = actualPicks.get(0);
        assertEquals(1, actualPicks.size());
        assertEquals(1, actualPick1.getQty());
        assertEquals(actualPicks.size(), inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertEquals(actualPick1, inventoryRemains.getSavedPicks(orderKey, skuId).get(0));
        assertEquals(2, inventoryRemains.getRemains().get(0).getQty());
        assertTrue(inventoryRemains.isFitted(order));
        assertTrue(inventoryRemains.isEnoughQty(order));
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldDecreaseItemQtyInInventoryByConcreteZones(BiFunction<InventoryRemains, OrderWithDetails,
            List<PickSku>> function) {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(3));
        OrderWithDetails order = owd(orderB000001004(), listOf(b00000100400001ROV11()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();
        Set<String> currentZones = Collections.singleton(inventory.get(0).getLocation().getZone());

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertTrue(inventoryRemains.isEnoughQty(order, currentZones));

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);

        //then
        PickSku actualPick1 = actualPicks.get(0);
        assertEquals(1, actualPicks.size());
        assertEquals(1, actualPick1.getQty());
        assertEquals(actualPicks.size(), inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertEquals(actualPick1, inventoryRemains.getSavedPicks(orderKey, skuId).get(0));
        assertEquals(2, inventoryRemains.getRemains().get(0).getQty());
        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertTrue(inventoryRemains.isEnoughQty(order, currentZones));
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldPickLastItemFromInventory(BiFunction<InventoryRemains, OrderWithDetails, List<PickSku>> function) {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(1));
        InventoryRemains inventoryRemains = new InventoryRemains(inventory);
        OrderWithDetails order = owd(orderB000001004(), listOf(b00000100400001ROV11()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();

        assertTrue(inventoryRemains.isFitted(order));
        assertTrue(inventoryRemains.isEnoughQty(order));

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);

        //then
        PickSku actualPick1 = actualPicks.get(0);
        assertEquals(1, actualPicks.size());
        assertEquals(1, actualPick1.getQty());
        assertEquals(actualPicks.size(), inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertEquals(actualPick1, inventoryRemains.getSavedPicks(orderKey, skuId).get(0));
        assertEquals(0, inventoryRemains.getRemains().size());
        assertFalse(inventoryRemains.isFitted(order));
        assertFalse(inventoryRemains.isEnoughQty(order));
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldPickLastItemFromInventoryByConcreteZones(BiFunction<InventoryRemains, OrderWithDetails,
            List<PickSku>> function) {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(1));
        OrderWithDetails order = owd(orderB000001004(), listOf(b00000100400001ROV11()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();
        Set<String> currentZones = Collections.singleton(inventory.get(0).getLocation().getZone());

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertTrue(inventoryRemains.isEnoughQty(order, currentZones));

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);

        //then
        PickSku actualPick1 = actualPicks.get(0);
        assertEquals(1, actualPicks.size());
        assertEquals(1, actualPick1.getQty());
        assertEquals(actualPicks.size(), inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertEquals(actualPick1, inventoryRemains.getSavedPicks(orderKey, skuId).get(0));
        assertEquals(0, inventoryRemains.getRemains().size());
        assertFalse(inventoryRemains.isFitted(order, currentZones));
        assertFalse(inventoryRemains.isEnoughQty(order, currentZones));
    }

    @Test
    void shouldBeFittedToZonesButNotEnoughQty() {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(1));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order));
        assertFalse(inventoryRemains.isEnoughQty(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.get(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.pick(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.getAndReserve(order));
    }

    @Test
    void shouldBeFittedToZonesButNotEnoughQtyByZones() {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(1));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));
        Set<String> currentZones = Collections.singleton(inventory.get(0).getLocation().getZone());

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertFalse(inventoryRemains.isEnoughQty(order, currentZones));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.get(order, currentZones));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.pick(order, currentZones));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.getAndReserve(order, currentZones));
    }

    @Test
    void shouldReturnEnoughItemsFromDoubleZone() {
        List<OrderInventoryDetail> inventory = listOf(
                invROV0000000000000000001C4100001(1), invROV0000000000000000001C4101001(2));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order));
        assertTrue(inventoryRemains.isEnoughQty(order));

        //when
        List<PickSku> actualPicks = inventoryRemains.get(order);

        //then
        assertEquals(2, actualPicks.size());
        assertEquals(2, inventoryRemains.getRemains().size());
        assertEquals(0, inventoryRemains.getSavedPicks(orderKey, skuId).size());

        //when
        actualPicks = inventoryRemains.pick(order);
        PickSku actualPick1 = actualPicks.stream().filter(pick -> pick.getQty() == 2).findFirst().get();
        PickSku actualPick2 = actualPicks.stream().filter(pick -> pick.getQty() == 1).findFirst().get();
        //then
        assertEquals(2, actualPicks.size());
        assertEquals(0, inventoryRemains.getRemains().size());
        assertEquals(2, inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertTrue(inventoryRemains.getSavedPicks(orderKey, skuId).containsAll(List.of(actualPick1, actualPick2)));
    }

    @Test
    void shouldReturnEnoughItemsFromDoubleZoneByZones() {
        List<OrderInventoryDetail> inventory = listOf(
                invROV0000000000000000001C4100001(1), invROV0000000000000000001C4101001(2));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));
        Set<String> currentZones = inventory.stream()
                .map(detail -> detail.getLocation().getZone()).collect(Collectors.toSet());
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertTrue(inventoryRemains.isEnoughQty(order, currentZones));

        //when
        List<PickSku> actualPicks = inventoryRemains.get(order, currentZones);
        //then
        assertEquals(2, actualPicks.size());
        assertEquals(2, inventoryRemains.getRemains().size());
        assertEquals(0, inventoryRemains.getSavedPicks(orderKey, skuId).size());

        //when
        actualPicks = inventoryRemains.pick(order);
        PickSku actualPick1 = actualPicks.stream().filter(pick -> pick.getQty() == 2).findFirst().get();
        PickSku actualPick2 = actualPicks.stream().filter(pick -> pick.getQty() == 1).findFirst().get();
        //then
        assertEquals(2, actualPicks.size());
        assertEquals(0, inventoryRemains.getRemains().size());
        assertEquals(2, inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertTrue(inventoryRemains.getSavedPicks(orderKey, skuId).containsAll(List.of(actualPick1, actualPick2)));
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldDecreaseItemQtyInDoubleZones(BiFunction<InventoryRemains, OrderWithDetails, List<PickSku>> function) {
        List<OrderInventoryDetail> inventory = listOf(
                invROV0000000000000000001C4100001(2), invROV0000000000000000001C4101001(2));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order));
        assertTrue(inventoryRemains.isEnoughQty(order));

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);
        PickSku actualPick1 = actualPicks.stream().filter(pick -> pick.getQty() == 2).findFirst().get();
        PickSku actualPick2 = actualPicks.stream().filter(pick -> pick.getQty() == 1).findFirst().get();
        //then
        assertEquals(2, actualPicks.size());
        assertNotNull(actualPick1);
        assertNotNull(actualPick2);
        assertEquals(actualPick1.getLocation().getZone(), actualPick2.getLocation().getZone());
        assertEquals(actualPicks.size(), inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertTrue(inventoryRemains.getSavedPicks(orderKey, skuId).stream().anyMatch(pick -> pick.equals(actualPick1)));
        assertTrue(inventoryRemains.getSavedPicks(orderKey, skuId).stream().anyMatch(pick -> pick.equals(actualPick2)));
        assertTrue(inventoryRemains.isFitted(order));
        assertFalse(inventoryRemains.isEnoughQty(order));
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldDecreaseItemQtyInDoubleZonesByConcreteZones(BiFunction<InventoryRemains, OrderWithDetails,
            List<PickSku>> function) {
        List<OrderInventoryDetail> inventory = listOf(
                invROV0000000000000000001C4100001(2), invROV0000000000000000001C4101001(2));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));
        Set<String> currentZones = inventory.stream()
                .map(detail -> detail.getLocation().getZone()).collect(Collectors.toSet());
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertTrue(inventoryRemains.isEnoughQty(order, currentZones));

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);
        //then
        PickSku actualPick1 = actualPicks.stream().filter(pick -> pick.getQty() == 2).findFirst().get();
        PickSku actualPick2 = actualPicks.stream().filter(pick -> pick.getQty() == 1).findFirst().get();
        assertEquals(2, actualPicks.size());
        assertNotNull(actualPick1);
        assertNotNull(actualPick2);
        assertEquals(actualPick1.getLocation().getZone(), actualPick2.getLocation().getZone());
        assertEquals(actualPicks.size(), inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertTrue(inventoryRemains.getSavedPicks(orderKey, skuId).stream().anyMatch(pick -> pick.equals(actualPick1)));
        assertTrue(inventoryRemains.getSavedPicks(orderKey, skuId).stream().anyMatch(pick -> pick.equals(actualPick2)));
        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertFalse(inventoryRemains.isEnoughQty(order, currentZones));
    }

    @Test
    void shouldBeFittedToZonesButNotEnoughQtyInDoubleZone() {
        List<OrderInventoryDetail> inventory = listOf(
                invROV0000000000000000001C4100001(1), invROV0000000000000000001C4101001(1));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order));
        assertFalse(inventoryRemains.isEnoughQty(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.get(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.pick(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.getAndReserve(order));
    }

    @Test
    void shouldBeFittedToZonesButNotEnoughQtyInDoubleZoneByConcreteZones() {
        List<OrderInventoryDetail> inventory = listOf(
                invROV0000000000000000001C4100001(1), invROV0000000000000000001C4101001(1));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));

        Set<String> currentZones = inventory.stream()
                .map(detail -> detail.getLocation().getZone()).collect(Collectors.toSet());

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order, currentZones));
        assertFalse(inventoryRemains.isEnoughQty(order, currentZones));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.get(order, currentZones));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.pick(order, currentZones));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.getAndReserve(order, currentZones));
    }

    @ParameterizedTest
    @MethodSource("pickOrReserve")
    void shouldDecreaseItemQtyInTwoZones(BiFunction<InventoryRemains, OrderWithDetails, List<PickSku>> function) {
        List<OrderInventoryDetail> inventory = listOf(
                invROV0000000000000000001C4100001(2), invROV0000000000000000001C4110003(2));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300001ROV13()));
        String orderKey = order.getOrder().getOrderKey();
        SkuId skuId = order.getOrderDetails().get(0).skuId();

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order));
        assertTrue(inventoryRemains.isEnoughQty(order));

        //when
        List<PickSku> actualPicks = function.apply(inventoryRemains, order);
        //then
        PickSku actualPick1 = actualPicks.stream().filter(pick -> pick.getQty() == 2).findFirst().get();
        PickSku actualPick2 = actualPicks.stream().filter(pick -> pick.getQty() == 1).findFirst().get();
        assertEquals(2, actualPicks.size());
        assertNotNull(actualPick1);
        assertNotNull(actualPick2);
        assertNotEquals(actualPick1.getLocation().getZone(), actualPick2.getLocation().getZone());
        assertEquals(actualPicks.size(), inventoryRemains.getSavedPicks(orderKey, skuId).size());
        assertTrue(inventoryRemains.getSavedPicks(orderKey, skuId).stream().anyMatch(pick -> pick.equals(actualPick1)));
        assertTrue(inventoryRemains.getSavedPicks(orderKey, skuId).stream().anyMatch(pick -> pick.equals(actualPick2)));
        assertTrue(inventoryRemains.isFitted(order));
        assertFalse(inventoryRemains.isEnoughQty(order));
    }

    @Test
    void shouldNotFittedToZones() {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(2));
        OrderWithDetails order = owd(orderB000001003(), listOf(b00000100300002ROV23()));

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertFalse(inventoryRemains.isFitted(order));
        assertFalse(inventoryRemains.isEnoughQty(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.get(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.pick(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.getAndReserve(order));
    }

    @Test
    void shouldNotEnoughQtyWhenDuplicateSku() {
        List<OrderInventoryDetail> inventory = listOf(invROV0000000000000000001C4100001(2));
        OrderWithDetails order = owdB000001004SkuDups();

        InventoryRemains inventoryRemains = new InventoryRemains(inventory);

        assertTrue(inventoryRemains.isFitted(order));
        assertFalse(inventoryRemains.isEnoughQty(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.get(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.pick(order));
        assertThrows(OrderItemOutOfStockException.class, () -> inventoryRemains.getAndReserve(order));
    }
}
