package ru.yandex.market.wms.autostart.modules.autostartlogic.service.util.dispenser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.comparator.PickSkuComparator;
import ru.yandex.market.wms.autostart.autostartlogic.orderbatching.InventoryRemains;
import ru.yandex.market.wms.autostart.autostartlogic.orderbatching.VanillaOrderBatchingLogic;
import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.OrderItemOutOfStockException;
import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PickingOrdersBatchingService;
import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.PopularPickingOrderPlanner;
import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.RandomOrdersGenerator;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.autostart.model.dto.AOSSettingsDto;
import ru.yandex.market.wms.autostart.model.dto.AOSZoneSettingsDto;
import ru.yandex.market.wms.autostart.util.dispenser.Dispenser;
import ru.yandex.market.wms.autostart.util.dispenser.PopularZonesDispenser;
import ru.yandex.market.wms.autostart.util.dispenser.SingleAccumulatedLimitChecker;
import ru.yandex.market.wms.common.spring.dao.entity.Batch;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.PickingOrderBatch;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SubBatch;
import ru.yandex.market.wms.common.spring.enums.WaveType;
import ru.yandex.market.wms.common.utils.IteratorUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000001003;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderDetailTestData.b00000100300001ROV13;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.invROV0000000000000000001C4100001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.sampleInventory;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owd;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004SkuDups;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000005002;


public class PopularZonesDispenserTest {
    static AtomicInteger batchKey = new AtomicInteger();
    static AtomicInteger pickingOrderKey = new AtomicInteger();

    @Data
    @AllArgsConstructor
    private class SkuAndZone {
        SkuId skuId;
        String zone;
    }

    @BeforeEach
    void beforeEach() {
        batchKey.set(0);
        pickingOrderKey.set(0);
    }

    @Test
    void shouldThrowExceptionWhenItemsOutOfStock() {
        final int minOrdersPerPutWall = 1;
        final int maxOrdersPerPutWall = 1;
        final int maxItemsPerPutwall = 100;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        candidateStations.put("STATION_1", 100);

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        SingleAccumulatedLimitChecker<OrderWithDetails> limitChecker =
                new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall);
        InventoryRemains inventoryRemains = new InventoryRemains(listOf(invROV0000000000000000001C4100001(1)));
        Dispenser<OrderWithDetails> ordersDispenser = new PopularZonesDispenser(
                listOf(owd(orderB000001003(), listOf(b00000100300001ROV13()))),
                inventoryRemains,
                () -> limitChecker,
                5,
                defaultWaveSettings()
        );

        assertThrows(OrderItemOutOfStockException.class, () -> batchingLogic.nextSubBatch(ordersDispenser));
    }

    @Test
    void shouldNotReturnBatchesBecauseOfNumberOfOrdersLesserThanMin() {
        final int minOrdersPerPutWall = 2;
        final int maxOrdersPerPutWall = 4;
        final int maxItemsPerPutwall = 100;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        candidateStations.put("STATION_1", 100);

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        SingleAccumulatedLimitChecker<OrderWithDetails> limitChecker =
                new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall);
        InventoryRemains inventoryRemains = new InventoryRemains(listOf(invROV0000000000000000001C4100001(3)));
        Dispenser<OrderWithDetails> ordersDispenser =
                new PopularZonesDispenser(listOf(owdB000001004()), inventoryRemains,
                        () -> limitChecker, 5, defaultWaveSettings());

        assertNull(batchingLogic.nextSubBatch(ordersDispenser));
    }

    @Test
    void shouldNotReturnAnyBatchWhenMinOrdersThresholdApproached() {
        final int minOrdersPerPutWall = 2;
        final int maxOrdersPerPutWall = 4;
        final int maxItemsPerPutwall = 100;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        candidateStations.put("STATION_1", 100);

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        SingleAccumulatedLimitChecker<OrderWithDetails> limitChecker =
                new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall);
        OrderInventoryDetail inventory = invROV0000000000000000001C4100001(3);
        InventoryRemains inventoryRemains = new InventoryRemains(listOf(inventory));
        OrderWithDetails order = owdB000001004();
        Dispenser<OrderWithDetails> ordersDispenser =
                new PopularZonesDispenser(listOf(order), inventoryRemains,
                        () -> limitChecker, 5, defaultWaveSettings());

        assertEquals(1, ordersDispenser.remainingCount());

        //when
        SubBatch<OrderWithDetails> subBatch = batchingLogic.nextSubBatch(ordersDispenser);

        //then
        assertNull(subBatch);
    }

    @Test
    // https://st.yandex-team.ru/MARKETWMS-8983
    void shouldMatchItemsCountInOrdersAndInPicksInCaseOfDuplicatedSku() {
        final int minOrdersPerPutWall = 1;
        final int maxOrdersPerPutWall = 4;
        final int maxItemsPerPutwall = 100;

        PickingOrdersBatchingService pickingService = new PickingOrdersBatchingService(
                () -> String.valueOf(batchKey.getAndIncrement()),
                () -> String.valueOf(pickingOrderKey.getAndIncrement()),
                zone -> AOSZoneSettingsDto.builder()
                        .itemsIntoPickingOrder(30)
                        .build(),
                AOSSettingsDto.builder().uniformPickingOrdersEnabled(false).itemsIntoPickingOrder(0).build()
        );

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        candidateStations.put("STATION_1", 100);

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        SingleAccumulatedLimitChecker<OrderWithDetails> limitChecker =
                new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall);
        OrderInventoryDetail inventory = invROV0000000000000000001C4100001(3);
        InventoryRemains inventoryRemains = new InventoryRemains(listOf(inventory));
        OrderWithDetails order = owdB000001004SkuDups();
        Dispenser<OrderWithDetails> ordersDispenser =
                new PopularZonesDispenser(listOf(order), inventoryRemains,
                        () -> limitChecker, 5, defaultWaveSettings());

        assertEquals(1, ordersDispenser.remainingCount());

        //when
        SubBatch<OrderWithDetails> subBatch = batchingLogic.nextSubBatch(ordersDispenser);

        assertEquals(order.itemCount(), subBatch.getOrders().stream().mapToInt(OrderWithDetails::itemCount).sum());

        PopularPickingOrderPlanner planner = new PopularPickingOrderPlanner(inventoryRemains);
        Batch<OrderWithDetails> batch = Batch.<OrderWithDetails>builder().id("1").subBatches(listOf(subBatch)).build();
        List<Batch<OrderWithDetails>> batches = Arrays.asList(batch);
        List<PickingOrderBatch> pickingOrderBatches = pickingService.makePickingOrderBatches(
                batches,
                Map.of(), true, 20, planner);

        assertEquals(order.itemCount(), StreamEx.of(pickingOrderBatches).mapToInt(PickingOrderBatch::itemCount).sum());
    }

    @Test
    void shouldReturnTwoBatches() {
        final int minOrdersPerPutWall = 1;
        final int maxOrdersPerPutWall = 3;
        final int maxItemsPerPutwall = 100;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        candidateStations.put("STATION_1", 100);
        candidateStations.put("STATION_2", 100);

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        SingleAccumulatedLimitChecker<OrderWithDetails> limitChecker =
                new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall);
        InventoryRemains inventoryRemains = new InventoryRemains(sampleInventory());
        List<OrderWithDetails> orders =
                listOf(owdB000001004(), owdB000003003(), owdB000005002(), owdB000003001(), owdB000003002(),
                        owdB000003004());
        Dispenser<OrderWithDetails> ordersDispenser =
                new PopularZonesDispenser(orders, inventoryRemains,
                        () -> limitChecker, 5, defaultWaveSettings());

        assertEquals(6, ordersDispenser.remainingCount());

        //when
        SubBatch<OrderWithDetails> subBatch1 = batchingLogic.nextSubBatch(ordersDispenser);
        SubBatch<OrderWithDetails> subBatch2 = batchingLogic.nextSubBatch(ordersDispenser);

        //then
        assertEquals(3, subBatch1.getOrders().size());
        assertEquals(3, subBatch2.getOrders().size());
        assertEquals(0, ordersDispenser.remainingCount());

        //when then
        assertNull(batchingLogic.nextSubBatch(ordersDispenser));
    }

    @Test
    void testBigOrders() {
        final int stationsNum = 20;
        final int minOrdersPerPutWall = 1;
        final int maxOrdersPerPutWall = 50;
        final int maxItemsPerPutwall = 1500;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        for (int i = 1; i <= stationsNum; i++) {
            candidateStations.put("STATION_" + i, 1000);
        }

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        RandomOrdersGenerator generator = new RandomOrdersGenerator();

        List<OrderWithDetails> orders = generator.genOwd(1000, 5, 10, 100, LocalDateTime.now());
        List<OrderInventoryDetail> inventory = generator.genInventoryExactly(orders, 10, 5, 0);
        Map<SkuId, Integer> inventoryExpected = new HashMap<>();
        inventory.forEach(i -> {
            inventoryExpected.putIfAbsent(i.getSkuId(), 0);
            inventoryExpected.computeIfPresent(i.getSkuId(), (k, v) -> v + i.getQty());
            //inventoryExpected.merge(i.getSkuId(), i.getQty(),  (k, v) -> v + i.getQty());
        });
        InventoryRemains inventoryRemains = new InventoryRemains(inventory);
        Dispenser<OrderWithDetails> ordersDispenser =
                new PopularZonesDispenser(
                        orders,
                        inventoryRemains,
                        () -> new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall),
                        6, defaultWaveSettings());

        List<SubBatch<OrderWithDetails>> subBatches =
                IteratorUtil.takeWhileNonNull(() -> batchingLogic.nextSubBatch(ordersDispenser));
        assertEquals(20, subBatches.size());
        subBatches.forEach(subBatch -> assertEquals(50, subBatch.getOrders().size()));
        subBatches.forEach(subBatch ->
                assertTrue(subBatch.getOrders().stream()
                        .mapToInt(OrderWithDetails::itemCount).sum() <= maxItemsPerPutwall));
        assertEquals(0, inventoryRemains.getRemains().size());


        Map<SkuId, Integer> inventoryActual = new HashMap<>();
        inventoryRemains.getSavedPicks().forEach(p -> {
            inventoryActual.putIfAbsent(p.getSkuId(), 0);
            inventoryActual.computeIfPresent(p.getSkuId(), (k, v) -> v + p.getQty());
            //inventoryActual.merge(p.getSkuId(), p.getQty(), (k, v) -> v + p.getQty());
        });

        assertEquals(inventoryExpected, inventoryActual);
    }

    @Test
    void testSortingOrderBetweenBatches() {
        final int stationsNum = 50;
        final int minOrdersPerPutWall = 1;
        final int maxOrdersPerPutWall = 20;
        final int maxItemsPerPutwall = 1000;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        for (int i = 1; i <= stationsNum; i++) {
            candidateStations.put("STATION_" + i, 1000);
        }

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        RandomOrdersGenerator generator = new RandomOrdersGenerator();

        List<OrderWithDetails> orders = generator.genOwd(1000, 5, 10, 10, LocalDateTime.now());

        List<OrderInventoryDetail> inventories = generator.genInventoryExactly(orders, 3, 5, 0);
        InventoryRemains inventoryRemains = new InventoryRemains(inventories);
        Dispenser<OrderWithDetails> ordersDispenser =
                new PopularZonesDispenser(
                        orders,
                        inventoryRemains,
                        () -> new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall),
                        10, defaultWaveSettings());

        List<SubBatch<OrderWithDetails>> subBatches =
                IteratorUtil.takeWhileNonNull(() -> batchingLogic.nextSubBatch(ordersDispenser));

        Map<SkuId, Instant> lastExpDatePerBatch = new HashMap<>();
        Map<SkuAndZone, PickSku> lastPickInZonePerBatch = new HashMap<>();

        Comparator<PickSku> comparator = new PickSkuComparator();

        for (SubBatch<OrderWithDetails> subBatch: subBatches) {
            List<PickSku> choosedPicks = new ArrayList<>();
            for (OrderWithDetails owd: subBatch.getOrders()) {
                for (OrderDetail od : owd.getOrderDetails()) {
                    List<PickSku> savedPicks = inventoryRemains
                            .getSavedPicks(owd.getOrder().getOrderKey(), SkuId.of(od.getStorerKey(), od.getSku()));
                        choosedPicks.addAll(savedPicks);
                }
            }

            if (!lastExpDatePerBatch.isEmpty()) {
                for (PickSku pickSku : choosedPicks) {
                    SkuAndZone skuAndZone = new SkuAndZone(pickSku.getSkuId(), pickSku.getLocation().getZone());
                    Instant lastExpDate = lastExpDatePerBatch.get(skuAndZone.skuId);
                    if (lastExpDate != null && pickSku.getSkuProperties().getLottable05() != null) {
                        assertTrue(lastExpDate.compareTo(pickSku.getSkuProperties().getLottable05()) <= 0);
                    }
                    PickSku lastPick = lastPickInZonePerBatch.get(skuAndZone);
                    if (lastPick != null) {
                        assertTrue(comparator.compare(lastPick, pickSku) <= 0);
                    }
                }
            }

            for (PickSku pickSku : choosedPicks) {
                SkuAndZone skuAndZone = new SkuAndZone(pickSku.getSkuId(), pickSku.getLocation().getZone());
                if (pickSku.getSkuProperties().getLottable05() != null) {
                    lastExpDatePerBatch
                            .putIfAbsent(skuAndZone.skuId, pickSku.getSkuProperties().getLottable05());
                    if (lastExpDatePerBatch.get(skuAndZone.skuId).compareTo(pickSku.getSkuProperties()
                            .getLottable05()) < 0) {
                        lastExpDatePerBatch.put(skuAndZone.skuId, pickSku.getSkuProperties().getLottable05());
                    }
                }
                lastPickInZonePerBatch.putIfAbsent(skuAndZone, pickSku);
                if (comparator.compare(lastPickInZonePerBatch.get(skuAndZone), pickSku) < 0) {
                    lastPickInZonePerBatch.put(skuAndZone, pickSku);
                }
            }
        }
    }

    @Test
    public void testPriorityOrders() {
        final int stationsNum = 2;
        final int minOrdersPerPutWall = 20;
        final int maxOrdersPerPutWall = 50;
        final int maxItemsPerPutwall = 1500;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        for (int i = 1; i <= stationsNum; i++) {
            candidateStations.put("STATION_" + i, 1000);
        }

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        RandomOrdersGenerator generator = new RandomOrdersGenerator();

        List<OrderWithDetails> orders = generator.genOwd(110, 5, 10, 100, LocalDateTime.now());
        List<OrderInventoryDetail> inventory = generator.genInventoryExactly(orders, 10, 5, 0);
        InventoryRemains inventoryRemains = new InventoryRemains(inventory);
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.ALL)
                .warehouseCutoffShift(1)
                .serverTime(LocalDateTime.now())
                .build();
        Dispenser<OrderWithDetails> ordersDispenser =
                new PopularZonesDispenser(
                        orders,
                        inventoryRemains,
                        () -> new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall),
                        6, waveSettings);

        List<SubBatch<OrderWithDetails>> subBatches =
                IteratorUtil.takeWhileNonNull(() -> batchingLogic.nextSubBatch(ordersDispenser));

        List<Order> batch1 = subBatches.get(0).getOrders().stream()
                .map(OrderWithDetails::getOrder).collect(Collectors.toList());
        List<Order> batch2 = subBatches.get(1).getOrders().stream()
                .map(OrderWithDetails::getOrder).collect(Collectors.toList());

        OffsetDateTime maxDate1 = batch1.stream().sorted(Comparator.comparing(Order::getShipmentDateTime).reversed())
                .map(Order::getShipmentDateTime)
                .findFirst().get();
        OffsetDateTime minDate2 = batch2.stream().sorted(Comparator.comparing(Order::getShipmentDateTime))
                .map(Order::getShipmentDateTime)
                .findFirst().get();
        assertEquals(2, subBatches.size());
        assertEquals(maxOrdersPerPutWall, batch1.size());
        assertEquals(maxOrdersPerPutWall, batch2.size());
        assertTrue(maxDate1.isBefore(minDate2) || maxDate1.isEqual(minDate2));
    }

    @Test
    void testPopZonesWithoutOptimisations() {
        final int stationsNum = 1;
        final int minOrdersPerPutWall = 100;
        final int maxOrdersPerPutWall = 100;
        final int maxItemsPerPutwall = 5000;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        for (int i = 1; i <= stationsNum; i++) {
            candidateStations.put("STATION_" + i, 1000);
        }

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);
        RandomOrdersGenerator generator = new RandomOrdersGenerator();

        List<OrderWithDetails> orders = generator.genOwd(100, 5, 10, 100, LocalDateTime.now());
        List<OrderInventoryDetail> inventory = generator.genInventoryExactly(orders, 10, 5, 0);
        InventoryRemains inventoryRemains = new InventoryRemains(inventory);
        Dispenser<OrderWithDetails> ordersDispenser =
                new PopularZonesDispenser(
                        orders,
                        inventoryRemains,
                        () -> new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall),
                        0, defaultWaveSettings());

        List<SubBatch<OrderWithDetails>> subBatches =
                IteratorUtil.takeWhileNonNull(() -> batchingLogic.nextSubBatch(ordersDispenser));
        assertEquals(1, subBatches.size());
        assertEquals(100, subBatches.get(0).getOrders().size());

        LocalDateTime lastShipmentDateTime = LocalDateTime.MIN;
        for (OrderWithDetails order: subBatches.get(0).getOrders()) {
            LocalDateTime nextDate = order.getOrder().getShipmentDateTime().toLocalDateTime();
            assertTrue(lastShipmentDateTime.isBefore(nextDate) || lastShipmentDateTime.equals(nextDate));
            lastShipmentDateTime = nextDate;
        }
    }

    private WaveSettings defaultWaveSettings() {
        return WaveSettings.builder()
                .waveType(WaveType.ALL)
                .warehouseCutoffShift(0)
                .serverTime(LocalDateTime.MIN)
                .build();
    }
}
