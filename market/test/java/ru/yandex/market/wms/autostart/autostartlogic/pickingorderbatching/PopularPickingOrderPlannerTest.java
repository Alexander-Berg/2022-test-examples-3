package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.orderbatching.InventoryRemains;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.utils.Grouper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.invROV0000000000000000001C4100001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.invROV0000000000000000001C4110003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004SkuDups;


public class PopularPickingOrderPlannerTest {
    private static Grouper<Map.Entry<PickSku, OrderDetail>, PickingOrderGroupKey, PickSku> grouper;

    @BeforeEach
    void clean() {
        grouper = new Grouper<>(
                entry -> new PickingOrderGroupKey(
                        entry.getKey().getLocation().getZone(),
                        entry.getValue().getAssigmentType()
                ), Map.Entry::getKey);
    }

    @Test
    public void shouldPlanOneInventory() {
        InventoryRemains inventoryRemains = new InventoryRemains(List.of(invROV0000000000000000001C4100001(1)));
        PopularPickingOrderPlanner planner = new PopularPickingOrderPlanner(inventoryRemains);
        SkuId skuId = SkuId.of(owdB000001004().getOrderDetails().get(0).getStorerKey(),
                owdB000001004().getOrderDetails().get(0).getSku());
        inventoryRemains.getAndReserve(owdB000001004());
        inventoryRemains.acceptReserved();

        //when
        int actualResult = planner.accept(owdB000001004().getOrderDetails().get(0), grouper);
        //then
        List<PickSku> pickSkuList = grouper.get().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertEquals(skuId, pickSkuList.get(0).getSkuId());
        assertEquals(1, pickSkuList.get(0).getQty());
        assertEquals(0, actualResult);

        //then
        assertEquals(1, planner.accept(owdB000001004().getOrderDetails().get(0), grouper));
    }

    @Test
    public void shouldPlanThreeInventoriesDuplicateSkuInOrder() {
        InventoryRemains inventoryRemains = new InventoryRemains(List.of(invROV0000000000000000001C4100001(3)));
        PopularPickingOrderPlanner planner = new PopularPickingOrderPlanner(inventoryRemains);
        SkuId skuId = SkuId.of(owdB000001004SkuDups().getOrderDetails().get(0).getStorerKey(),
                owdB000001004().getOrderDetails().get(0).getSku());
        inventoryRemains.getAndReserve(owdB000001004SkuDups());
        inventoryRemains.acceptReserved();

        //when
        int actualResult = planner.accept(owdB000001004SkuDups().getOrderDetails().get(0), grouper);
        //then
        List<PickSku> pickSkuList = grouper.get().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertEquals(skuId, pickSkuList.get(0).getSkuId());
        assertEquals(1, pickSkuList.get(0).getQty());
        assertEquals(0, actualResult);

        //then
        assertEquals(0, planner.accept(owdB000001004SkuDups().getOrderDetails().get(1), grouper));
        //then
        assertEquals(0, planner.accept(owdB000001004SkuDups().getOrderDetails().get(2), grouper));
        //then
        assertEquals(1, planner.accept(owdB000001004SkuDups().getOrderDetails().get(0), grouper));
    }

    @Test
    public void shouldPlanAllInventoriesFromDiffLocationsDuplicateSkuInOrder() {
        InventoryRemains inventoryRemains = new InventoryRemains(List.of(
                invROV0000000000000000001C4100001(1), invROV0000000000000000001C4110003(2)));
        PopularPickingOrderPlanner planner = new PopularPickingOrderPlanner(inventoryRemains);
        SkuId skuId = SkuId.of(owdB000001004SkuDups().getOrderDetails().get(0).getStorerKey(),
                owdB000001004().getOrderDetails().get(0).getSku());
        inventoryRemains.getAndReserve(owdB000001004SkuDups());
        inventoryRemains.acceptReserved();

        //when
        int actualResult = planner.accept(owdB000001004SkuDups().getOrderDetails().get(0), grouper);
        //then
        List<PickSku> pickSkuList = grouper.get().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertEquals(skuId, pickSkuList.get(0).getSkuId());
        assertEquals(1, pickSkuList.get(0).getQty());
        assertEquals(0, actualResult);

        //then
        assertEquals(0, planner.accept(owdB000001004SkuDups().getOrderDetails().get(1), grouper));
        //then
        assertEquals(0, planner.accept(owdB000001004SkuDups().getOrderDetails().get(2), grouper));
        //then
        assertEquals(1, planner.accept(owdB000001004SkuDups().getOrderDetails().get(0), grouper));
    }

    @Test
    public void shouldPlanRandomOrders() {
        RandomOrdersGenerator generator = new RandomOrdersGenerator();

        List<OrderWithDetails> orders = generator.genOwd(1000, 10, 20, 20, LocalDateTime.now());
        List<OrderInventoryDetail> inventory = generator.genInventoryExactly(orders, 10, 5, 0);
        InventoryRemains inventoryRemains = new InventoryRemains(inventory);
        orders.forEach(inventoryRemains::getAndReserve);
        inventoryRemains.acceptReserved();

        PopularPickingOrderPlanner planner = new PopularPickingOrderPlanner(inventoryRemains);
        for (OrderWithDetails owd: orders) {
            owd.getOrderDetails().forEach(od -> {
                assertEquals(0, planner.accept(od, grouper));
            });
        }
        assertEquals(orders.get(0).getOrderDetails().get(0).getOpenQty().intValueExact(),
                planner.accept(orders.get(0).getOrderDetails().get(0), grouper));
        int sumItemsInGroup = grouper.get().entrySet().stream().flatMap(group -> group.getValue().stream())
                .mapToInt(PickSku::getQty).sum();
        int sumItemsInOrders = orders.stream().mapToInt(OrderWithDetails::itemCount).sum();
        assertEquals(sumItemsInGroup, sumItemsInOrders);
    }

    @Test
    public void shouldCorrectlyProcessZeroItems() {
        InventoryRemains inventoryRemains = new InventoryRemains(List.of(invROV0000000000000000001C4100001(1)));
        PopularPickingOrderPlanner planner = new PopularPickingOrderPlanner(inventoryRemains);
        OrderDetail actualOrderDetail = owdB000001004().getOrderDetails().get(0);
        SkuId skuId = SkuId.of(actualOrderDetail.getStorerKey(), actualOrderDetail.getSku());
        inventoryRemains.getAndReserve(owdB000001004());
        inventoryRemains.acceptReserved();

        OrderDetail orderDetail = OrderDetail.builder()
                .orderKey(actualOrderDetail.getOrderKey())
                .sku(actualOrderDetail.getSku())
                .openQty(BigDecimal.ZERO)
                .build();
        assertEquals(0, planner.accept(orderDetail, grouper));
    }
}
