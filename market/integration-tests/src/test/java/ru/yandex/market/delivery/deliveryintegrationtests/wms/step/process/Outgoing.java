package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static java.util.Collections.singletonList;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.util.Pair;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.LocationKey;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OutboundStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ApiOrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SorterExit;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui.UISteps;
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemStatus;

@Slf4j
public class Outgoing {
    private final UISteps uiSteps;

    public Outgoing(UISteps uiSteps) {
        this.uiSteps = uiSteps;
    }

    //В тестах можно использовать только запуск на конкретную станцию.
    //Если WMS дать выбирать станцию самой, то 2 параллельных запуска не будут работать
    //Волны будут запускаться на сортстанции чужого теста
    @Step("Создаем и принудительно запускаем волну типа Большое изъятие через Ручной запуск волн в новом ui")
    public WaveId createAndForceStartBigWithdrawalWaveManually(Order order, String consolidationLine) {
        waitOrderStatusIs(order.getFulfillmentId(), OrderStatus.CREATED);

        uiSteps.Login().PerformLogin();
        WaveId waveId = uiSteps.Wave().createWaveWithOrder(order);

        log.info("WaveId: {}", waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().reserveWave(waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().forceStartWaveOntoConsolidationLine(waveId, consolidationLine);
        waitOrderStatusIs(order.getFulfillmentId(), OrderStatus.COMPLECTATION_STARTED);

        return waveId;
    }

    public WaveId createWaveManuallyForReplenishment(Order order) {
        return createWaveManuallyForReplenishment(List.of(order));
    }

    @Step("Создаем и неудачно резервируем волну под пополнение")
    public WaveId createWaveManuallyForReplenishment(Collection<Order> orders) {
        List<String> orderIds = orders.stream().map(Order::getFulfillmentId).toList();
        waitOrderStatusIs(orderIds, OrderStatus.CREATED);

        uiSteps.Login().PerformLogin();
        WaveId waveId = uiSteps.Wave().createWaveWithOrders(orderIds);

        log.info("WaveId: {}", waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().tryReserveWaveAndReplenish(waveId);
        return waveId;
    }

    @Step("Создаем и принудительно запускаем волну на сортировочную станцию через Ручной запуск волн в новом ui")
    public WaveId createAndForceStartWaveManually(Order order, String sortingStation) {
        waitOrderStatusIs(order.getFulfillmentId(), OrderStatus.CREATED);

        uiSteps.Login().PerformLogin();
        WaveId waveId = uiSteps.Wave().createWaveWithOrder(order);

        log.info("WaveId: {}", waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().reserveWave(waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().forceStartWaveOntoSortingStation(waveId, sortingStation);
        waitOrderStatusIs(order.getFulfillmentId(), OrderStatus.PACKAGED);

        return waveId;
    }

    @Step("Создаем и принудительно запускаем волну на сортировочную станцию через Ручной запуск волн в новом ui")
    public WaveId createAndForceStartWaveManuallyByOrderIds(Collection<String> orderIds, String sortingStation) {
        waitOrderStatusIs(orderIds, OrderStatus.CREATED);

        uiSteps.Login().PerformLogin();
        WaveId waveId = uiSteps.Wave().createWaveWithOrders(orderIds);

        log.info("WaveId: {}", waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().reserveWave(waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().forceStartWaveOntoSortingStation(waveId, sortingStation);
        waitOrderStatusIs(orderIds, OrderStatus.PACKAGED);

        return waveId;
    }

    public WaveId createAndForceStartWaveManually(Collection<Order> orders, String sortingStation) {
        var orderIds = orders.stream().map(Order::getFulfillmentId).collect(Collectors.toSet());
        return createAndForceStartWaveManuallyByOrderIds(orderIds, sortingStation);
    }

    @Step("Создаем и принудительно запускаем сингловую волну на линию консолидации через Ручной запуск волн в новом ui")
    public WaveId createAndForceStartSingleWaveManually(Order order, String consolidationLine) {
        waitOrderStatusIs(order.getFulfillmentId(), OrderStatus.CREATED);

        uiSteps.Login().PerformLogin();
        WaveId waveId = uiSteps.Wave().createWaveWithOrder(order);

        log.info("WaveId: {}", waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().reserveWave(waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().forceStartWaveOntoConsolidationLine(waveId, consolidationLine);
        waitOrderStatusIs(order.getFulfillmentId(), OrderStatus.PACKAGED);

        return waveId;
    }

    @Step("Создаем и принудительно запускаем сингловую волну на линию консолидации с двумя сингловыми заказами через Ручной запуск волн в новом ui")
    public WaveId createAndForceStartSingleWaveManuallyWithTwoOrders(Collection<Order> orders, String consolidationLine) {
        List<String> orderIds = orders.stream().map(Order::getFulfillmentId).collect(Collectors.toList());
        waitOrderStatusIs(orderIds, OrderStatus.CREATED);

        uiSteps.Login().PerformLogin();
        WaveId waveId = uiSteps.Wave().createWaveWithOrders(orderIds);

        log.info("WaveId: {}", waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().reserveWave(waveId);

        uiSteps.Login().PerformLogin();
        uiSteps.Wave().forceStartWaveOntoConsolidationLine(waveId, consolidationLine);
        waitOrderStatusIs(orderIds, OrderStatus.PACKAGED);

        return waveId;
    }

    @Step("Создаем волну с заданным заказом и сортировочной станцией через api ручку автостарта")
    public WaveId createAndStartAutoWave(Order order, String sortingStation) {
        waitOrderStatusIs(order.getFulfillmentId(), OrderStatus.CREATED);

        ApiSteps.Autostart().addSortingStation(sortingStation);
        ApiSteps.Autostart().startWave(order);
        ApiSteps.Autostart().removeSortingStation(sortingStation);

        uiSteps.Login().PerformLogin();
        uiSteps.Order().verifyOrderStatus(order.getFulfillmentId(), OrderStatus.PACKAGED);
        uiSteps.Login().PerformLogin();
        WaveId waveId = uiSteps.Wave().getOrderWave(order.getFulfillmentId());
        log.info("WaveId: {}", waveId);

        return waveId;
    }

    @Step("Узнаем сортировочную станцию волны")
    public String getSortingStationByWaveId(WaveId waveId) {
        return ApiSteps.Autostart().getSortingStationByWaveId(waveId);
    }


    public List<String> consolidateWaveAndOrder(List<String> itemSerials, String consolidationCell, String containerLabel,
                                                String sortingStation, String sortingCell) {
        consolidateWaveContainerToLine(containerLabel, consolidationCell);
        return sortFromContainerIntoPutwall(itemSerials, containerLabel, sortingStation, sortingCell);
    }

    public List<String> consolidateWaveAndOrders(Map<String, String> itemSerialToSortingCellsMapping,
                                                 String consolidationCell, String containerLabel,
                                                 String sortingStation) {
        consolidateWaveContainerToLine(containerLabel, consolidationCell);
        return sortFromContainerIntoPutwall(itemSerialToSortingCellsMapping, containerLabel, sortingStation);
    }

    @Step("Сортируем в путвол заказы из контейнера {containerLabel}")
    public List<String> sortFromContainerIntoPutwall(List<String> itemSerials, String containerLabel,
                                                     String sortingStation, String sortingCell) {
        return sortFromContainerIntoPutwallBase(itemSerials, containerLabel, sortingStation, sortingCell,
                Collections.emptyList());
    }

    @Step("Сортируем в путвол заказы из контейнера {containerLabel}, часть шортируем")
    public List<String> sortFromContainerIntoPutwall(List<String> itemSerials, String containerLabel,
                                                     String sortingStation, String sortingCell,
                                                     List<String> itemSerialsForShortage) {
        return sortFromContainerIntoPutwallBase(itemSerials, containerLabel, sortingStation, sortingCell,
                itemSerialsForShortage);
    }

    @Step("Сортируем в путвол заказы из контейнера {containerLabel}")
    public List<String> sortFromContainerIntoPutwall(Map<String, String> itemSerialToSortingCellsMapping,
                                                     String containerLabel, String sortingStation) {
        return sortFromContainerIntoPutwallBase(itemSerialToSortingCellsMapping, containerLabel, sortingStation,
                Collections.emptyList());
    }

    @Step("Сортируем в путвол заказы из контейнера {containerLabel}, часть шортируем")
    public List<String> sortFromContainerIntoPutwall(Map<String, String> itemSerialToSortingCellsMapping,
                                                     String containerLabel, String sortingStation,
                                                     List<String> itemSerialsForShortage) {
        return sortFromContainerIntoPutwallBase(itemSerialToSortingCellsMapping, containerLabel, sortingStation,
                itemSerialsForShortage);
    }

    private List<String> sortFromContainerIntoPutwallBase(List<String> itemSerials, String containerLabel,
                                                          String sortingStation, String sortingCell,
                                                          List<String> itemSerialsForShortage) {
        log.info("Items: {} in container {} before order consolidation", itemSerials, containerLabel);
        uiSteps.Login().PerformLogin();
        if (!itemSerialsForShortage.isEmpty()) {
            uiSteps.Order().orderConsolidationWithShortage(sortingStation, sortingCell, containerLabel,
                    itemSerials, itemSerialsForShortage);
            itemSerials.removeAll(itemSerialsForShortage);
        } else {
            uiSteps.Order().orderConsolidation(sortingStation, sortingCell, containerLabel,
                    itemSerials);
        }
        return itemSerials;
    }

    private List<String> sortFromContainerIntoPutwallBase(
            Map<String, String> itemSerialToSortingCellsMapping,
            String containerLabel,
            String sortingStation,
            List<String> itemSerialsForShortage) {
        log.info(
                "Items: {} in container {} before order consolidation",
                itemSerialToSortingCellsMapping.keySet(),
                containerLabel
        );
        uiSteps.Login().PerformLogin();
        if (!itemSerialsForShortage.isEmpty()) {
            uiSteps.Order().orderConsolidationWithShortage(sortingStation, containerLabel,
                    itemSerialToSortingCellsMapping, itemSerialsForShortage);
            itemSerialsForShortage.forEach(itemSerialToSortingCellsMapping::remove);
        } else {
            uiSteps.Order().orderConsolidation(sortingStation, containerLabel, itemSerialToSortingCellsMapping);
        }
        return new ArrayList<>(itemSerialToSortingCellsMapping.keySet());
    }

    @Step("Предконсолидируем контейнер {containerLabel} на линию")
    public void consolidateWaveContainerToLine(String containerLabel, String consolidationCell) {
        log.info("Wave consolidation start for container {} in consolidation cell {}",
                containerLabel, consolidationCell);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().preConsOneCart(containerLabel, consolidationCell);
    }

    @Step("Предконсолидируем контейнер {containerLabel} на предлагаемую линию")
    public void consolidateWaveContainerToProposedLine(String containerLabel) {
        log.info("Wave consolidation start for container {} to proposed consolidation cell", containerLabel);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().preConsOneCart(containerLabel);
    }

    @Step("Отбор одного назначения для заказа c отказом взять следующее задание")
    public List<String> pickSingleOrderAssignmentRefusingNextTask(String areaKey, String containerLabel) {
        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Order().getAssignment(areaKey, containerLabel);
        }, Retrier.RETRIES_TINY, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
        return uiSteps.Order().pickAndFinishAssignment();
    }

    @Step("Отбор одного назначения для заказа и получение следующего задания")
    public List<String> pickSingleOrderAssignmentAcceptingNextTask(String areaKey, String containerLabel) {
        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Order().getAssignment(areaKey, containerLabel);
        }, Retrier.RETRIES_TINY, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
        return uiSteps.Order().pickAndFinishAssignmentAndTakeNextTask();
    }

    @Step("Отбор одного назначения для заказа после получение следующего задания")
    public List<String> pickSingleOrderAfterAcceptingNextTask(String putawayZoneKey, String containerLabel) {
        uiSteps.Order().checkPutawayZoneOfAssignment(putawayZoneKey);
        uiSteps.Order().enterPickingCart(containerLabel);
        return uiSteps.Order().pickAndFinishAssignment();
    }

    @Step("Отбор одного назначения c шортированием и ожиданием дорезерва")
    public List<String> pickSingleOrderAssignmentWithShortage(String areaKey, String containerLabel, int expectedQty) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getAssignment(areaKey, containerLabel);
        uiSteps.Order().doShortWhilePickingAssignmentItems();

        waitForShortedItemReplacementInAssignment(containerLabel, expectedQty);

        return uiSteps.Order().pickAssignmentItems();
    }

    @Step("Отбор одного назначения изъятий c шортированием и ожиданием дорезерва")
    public Pair<String, String> pickWithdrawalAssignmentDoShortageWaitReplacement(String areaKey, String containerLabel,
                                                                          int expectedQty) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getWithdrawalAssignment(areaKey, containerLabel);
        Pair<String, String> shortedCellAndLot = uiSteps.Order().doShortWhilePickingAssignmentItems();

        waitForShortedItemReplacementInAssignment(containerLabel, expectedQty);
        return shortedCellAndLot;
    }

    @Step("Доотбираем ранее взятое назначение изъятия")
    public List<String> resumeAndCompleteWithdrawalAssignment() {
        //подразумевается, что будет вызов сразу после pickWithdrawalAssignmentDoShortageWaitReplacement()
        return uiSteps.Order().pickAssignmentItems();
    }

    @Step("Отбор одного назначения изъятий c шортированием, ожиданием дорезерва и доотбором")
    public List<String> pickWithdrawalAssignmentWithShortage(String areaKey, String containerLabel, int expectedQty) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getWithdrawalAssignment(areaKey, containerLabel);
        uiSteps.Order().doShortWhilePickingAssignmentItems();
        waitForShortedItemReplacementInAssignment(containerLabel, expectedQty);
        return resumeAndCompleteWithdrawalAssignment();
    }

    @Step("Отбор одного назначения для заказа")
    public List<String> pickSingleOrderAssignmentWithRetry(String areaKey, String containerLabel) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getAssignmentWithRetry(areaKey, containerLabel);
        return uiSteps.Order().pickAndFinishAssignment();
    }

    @Step("Отбор одного назначения для изъятия")
    public List<String> pickWithdrawalAssignmentWithRetry(String areaKey, String containerLabel) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getWithdrawalAssignmentWithRetry(areaKey, containerLabel);
        return uiSteps.Order().pickAndFinishAssignment();
    }

    @Step("Отбор одного назначения для изъятия")
    public List<String> pickWithdrawalAssignment(String areaKey, String containerLabel) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getWithdrawalAssignment(areaKey, containerLabel);
        return uiSteps.Order().pickAndFinishAssignment();
    }

    @Step("Отбор одного назначения для изъятия по НЗН")
    public void pickSingleOutboundAssignment(String areaKey, String id, String containerLabel) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getWithdrawalAssignment(areaKey, containerLabel);
        uiSteps.Order().pickAndFinishAssignment(id);
    }

    @Step("Отбор множественных назначений")
    public void pickMultipleAssignments(Map<String, List<String>> containerItemsMap,
                                        Set<String> containers,
                                        String areaKey) {
        log.info("multiPicking test container labels: {}", containers);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getMultiAssignments(areaKey, containers);
        uiSteps.Order().pickMultiAssignment(containerItemsMap);
        uiSteps.Order().dropContainers(containerItemsMap);
    }

    @Step("Отбор множественных назначений для сингловых заказов")
    public void pickMultipleAssignmentsSingleOrders(Map<String, List<String>> containerItemsMap,
                                        Set<String> containers,
                                        String areaKey) {
        log.info("multiPicking test container labels: {}", containers);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().getMultiAssignments(areaKey, containers);
        uiSteps.Order().pickMultiAssignment(containerItemsMap);
        uiSteps.Order().dropContainersSingleOrders();
    }

    @Step("Упаковка заказа")
    public List<ParcelId> packOrder(List<String> itemSerials, String packingTable) {
        return packOrder(itemSerials, packingTable, null);
    }

    @Step("Упаковка заказа")
    public List<ParcelId> packOrderFromContainer(List<String> itemSerials, String packingTable, String containerLabel) {
        log.info("Packing start for items {} on packing table {}, containerLabel={}",
                itemSerials, packingTable, containerLabel);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().packGoodsFromContainer(packingTable, itemSerials, containerLabel);

        uiSteps.Login().PerformLogin();
        return uiSteps.Balances().findNznBySerial(itemSerials);
    }

    @Step("Последовательная упаковка нескольких сингловых заказов")
    public List<ParcelId> packSeveralOrders(String packingTable, Map<String, List<String>> containerToUitsMap) {
        List<ParcelId> parcelIds = new ArrayList<>();
        log.info("Packing start on packing table {}", packingTable);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().packGoodsFromSeveralContainers(packingTable, containerToUitsMap);

        containerToUitsMap.keySet().forEach(containerLabel -> {
            uiSteps.Login().PerformLogin();
            parcelIds.addAll(uiSteps.Balances().findNznBySerial(containerToUitsMap.get(containerLabel)));
        });

        return parcelIds;
    }

    @Step("Упаковка изъятий")
    public List<ParcelId> packWithdrawalOrder(List<String> itemSerials, String packingTable) {
        log.info("Packing start for items {} on packing table {}", itemSerials, packingTable);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().packGoodsOnePerPack(packingTable, itemSerials);

        uiSteps.Login().PerformLogin();
        return uiSteps.Balances().findNznBySerial(itemSerials);
    }

    @Step("Упаковка нонсортового заказа")
    public List<ParcelId> packNonsortOrder(String containerLabel, List<String> itemSerials, String packingTable) {
        return packOrder(itemSerials, packingTable, containerLabel);
    }

    @Step("Упаковка заказов")
    public List<ParcelId> packOrders(Map<String, String> itemSerialsToSortingCellsMapping, String packingTable) {
        var itemSerials = new ArrayList<>(itemSerialsToSortingCellsMapping.keySet());
        log.info("Packing start for items {} on packing table {}", itemSerials, packingTable);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().packGoods(packingTable, itemSerialsToSortingCellsMapping);

        uiSteps.Login().PerformLogin();
        return uiSteps.Balances().findNznBySerial(itemSerials);
    }

    @Step("Упаковка заказа из ячейки {putwallCell} с шортированием оставшихся товаров")
    public void packOneOrderWithShort(List<String> itemSerials, String packingTable, String putwallCell,
                                      boolean isLastTask) {
            log.info("Packing start for items {} on packing table {} with short other serials",
                    itemSerials, packingTable);
            uiSteps.Login().PerformLogin();
            uiSteps.Order().packGoodsFromCellShortage(packingTable, itemSerials, putwallCell, isLastTask);
    }

    @Step("Упаковка заказа из ячейки {putwallCell}")
    public void packOneOrder(List<String> itemSerials, String packingTable, String putwallCell) {
        log.info("Packing start for items {} on packing table {} with short other serials",
                itemSerials, packingTable);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().packGoodsFromCell(packingTable, itemSerials, putwallCell);
    }

    @Step("Упаковка заказа по НЗН")
    public List<ParcelId> packId(String id, String packingTable) {
        log.info("Packing start for id {} on packing table {}", id, packingTable);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().packId(packingTable, id);

        uiSteps.Login().PerformLogin();
        return uiSteps.Balances().findNznByLoc(packingTable);
    }

    private List<ParcelId> packOrder(
            List<String> itemSerials, String packingTable, String containerLabel) {
        log.info("Packing start for items {} on packing table {}", itemSerials, packingTable);
        uiSteps.Login().PerformLogin();
        uiSteps.Order().packGoods(packingTable, itemSerials, containerLabel);

        uiSteps.Login().PerformLogin();
        return uiSteps.Balances().findNznBySerial(itemSerials);
    }

    @Step("Перекладываем в отмененку")
    public void moveCancelledItems(List<String> stuckItems, String containerForCancelledLabel, String packingTable) {
        log.info("Moving cancelled items from packing table {} to {}", packingTable, containerForCancelledLabel);
        // задания могут не успеть появиться в JVM, интервал подгрузки 10 сек.
        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Order().moveCancelledItems(packingTable, stuckItems, containerForCancelledLabel);
        }, Retrier.RETRIES_TINY, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }

    @Step("Размещаем коробки на паллеты")
    public DropId dropOrders(List<ParcelId> packs, String droppingCell) {
        uiSteps.Login().PerformLogin();
        return uiSteps.Order().deliverySorting(packs, droppingCell);
    }

    @Step("Размещаем коробку на паллету")
    public DropId dropOrder(ParcelId pack, String droppingCell) {
        uiSteps.Login().PerformLogin();
        return uiSteps.Order().deliverySorting(pack, droppingCell);
    }

    public void shipOrder(Order order,
                          DropId dropId,
                          String shippingDoor,
                          String shippingCell) {
        shipOrders(singletonList(order),  singletonList(dropId), shippingDoor, singletonList(shippingCell));
    }

    @Step("Новая отгрузка заказов")
    public void shipOrders(List<Order> orders, List<DropId> dropIds,
                           String shippingDoor,
                           List<String> shippingCells) {
        log.info("Shipping start for orders {}", orders);

        String carrier = "PickPoint very very very very long name max45";
        String vehicle = "ST" + shippingDoor;
        String scheduledDepartureTime = DateUtil.currentTimePlusHours(1);

        uiSteps.Login().PerformLogin();
        uiSteps.Order().connectDoorToCarrier(shippingDoor, carrier);

        uiSteps.Login().PerformLogin();
        uiSteps.Order().shippingPlacement(dropIds, shippingCells);

        uiSteps.Login().PerformLogin();
        Long shipmentId = uiSteps.Order()
                .createShipmentStandard(vehicle, carrier, shippingDoor, scheduledDepartureTime);

        uiSteps.Login().PerformLogin();
        uiSteps.Order().newShipping(shippingDoor, dropIds);

        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Order().closeShipment(shipmentId);
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }

    @Step("Новая отгрузка изъятия")
    public void shipWithdrawals(Outbound outbound, DropId dropId, String shippingDoor, String shippingCell) {
        String vehicle = "WD" + shippingDoor;

        uiSteps.Login().PerformLogin();
        uiSteps.Order().shippingPlacement(dropId, shippingCell);

        uiSteps.Login().PerformLogin();
        Long shipmentId = uiSteps.Order().createShipmentWithdrawal(vehicle, outbound.getFulfillmentId(), shippingDoor);

        uiSteps.Login().PerformLogin();
        uiSteps.Order().newShipping(shippingDoor, dropId);

        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Order().closeShipment(shipmentId);
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);

        ApiSteps.Outbound().verifyOutboundStatus(outbound, OutboundStatus.TRANSFERRED);
    }

    @Step("Новая отгрузка изъятий")
    public void shipWithdrawals(List<Outbound> outbounds, List<DropId> dropIds, String shippingDoor, List<String> shippingCells) {
        String vehicle = "WD" + shippingDoor;

        uiSteps.Login().PerformLogin();
        uiSteps.Order().shippingPlacement(dropIds, shippingCells);

        List<String> withdrawalIds = outbounds.stream()
                .map(Outbound::getFulfillmentId)
                .collect(Collectors.toList());
        uiSteps.Login().PerformLogin();
        Long shipmentId = uiSteps.Order().createShipmentWithdrawal(vehicle, withdrawalIds, shippingDoor);

        uiSteps.Login().PerformLogin();
        uiSteps.Order().newShipping(shippingDoor, dropIds);

        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Order().closeShipment(shipmentId);
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);

        outbounds.forEach(outbound -> ApiSteps.Outbound().verifyOutboundStatus(outbound, OutboundStatus.TRANSFERRED));
    }

    @Step("Отгрузка изъятия через интерфейс \"Отгрузка заказа\"")
    public void shipWithdrawalByOrderKey(Outbound outbound) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().shipOrderByOrderKey(outbound.getFulfillmentId());

        ApiSteps.Outbound().verifyOutboundStatus(outbound, OutboundStatus.TRANSFERRED);
    }

    @Step("Отгрузка стандартного заказа через интерфейс \"Отгрузка заказа\"")
    public void shipOrderByOrderKey(Order order) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().shipOrderByOrderKey(order.getFulfillmentId());

        ApiSteps.Order().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
    }

    @Step("Проверяем выход сортировщика")
    public void checkSorterOrderLocation(List<ParcelId> packs, SorterExit targetSorterLocation) {
        String parcelId = packs.get(0).getId();
        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Order().checkSorterOrderLocation(parcelId, targetSorterLocation);
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
    }

    @Step("Разделяем заказ по зданиям")
    public List<Order> splitCreatedOrder(Long yandexId) {
        List<Order> splitOrders = new ArrayList<>();

        ApiSteps.Order().startBuildingMarkingJob();

        uiSteps.Login().PerformLogin();
        splitOrders.add(
                new Order(yandexId, uiSteps.Order().getOrderFulfilmentIdByRowIndex(yandexId, 0)));

        uiSteps.Login().PerformLogin();
        splitOrders.add(
                new Order(yandexId, uiSteps.Order().getOrderFulfilmentIdByRowIndex(yandexId, 1)));

        return splitOrders;
    }

    @Step("Проверяем заказ на проставленность поля Building")
    public void checkBuildingForOrder(String orderKey) {
        uiSteps.Login().PerformLogin();
        uiSteps.Wave().checkBuildingForOrder(orderKey);
    }

    @Step("Проверяем статус заказа")
    public void checkOrdersStatus(String orderKey) {
        ApiSteps.Order().startCalculateOrdersStatusJob();

        waitOrderStatusIs(orderKey, OrderStatus.CREATED);
    }

    @Step("Проверяем историю статуса заказа с удаленной строкой")
    public void checkOrderWithRemovedItemStatusHistory(String orderKey) {
        ApiSteps.Order().checkOrderWasInStatus(orderKey, OrderStatus.ITEMS_AUTOMATICALLY_REMOVED);
    }

    @Step("Проверяем историю статуса отмененного заказа")
    public void checkCanceledOrderStatusHistory(String orderKey) {
        ApiSteps.Order().checkOrderStatusHistory(orderKey, OrderStatus.CANCELLED_INTERNALLY,
                OrderStatus.ITEMS_OUT_OF_STOCK);
    }

    @Step("Ждём, пока totalQty заказа {orderId} станет {totalQty}")
    public void waitOrderTotalQtyIs(String orderId, int totalQty) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().waitOrderTotalQtyIs(orderId, totalQty);
    }

    @Step("Ждём, пока статус заказа {orderId} станет {status}")
    public void waitOrderStatusIs(String orderId, OrderStatus status) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().waitOrderStatusIs(orderId, status);
    }

    @Step("Ждём, пока статус заказа {orderIds} станет {status}")
    public void waitOrderStatusIs(Collection<String> orderIds, OrderStatus status) {
        uiSteps.Login().PerformLogin();
        uiSteps.Order().waitOrderStatusIs(orderIds, status);
    }

    @Step("Проверяем, что УИТы находятся в ячейке {loc} и НЗН {nzn} после асинхронной обработки.")
    public void checkItemsLocAndNznAfterAsyncAction(List<String> uits, String loc, String nzn) {
        LocationKey expectedLocationKey = new LocationKey(loc, nzn);
        uits.forEach(uit -> waitItemLocationKeyIs(expectedLocationKey, uit));
    }

    public void createWithdrawalReplenishmentTasks(String orderId) {
        createWithdrawalReplenishmentTasks(List.of(orderId));
    }

    @Step("Ждем появления проблемного заказа изъятий и создаем ему задачки на пополнение")
    public void createWithdrawalReplenishmentTasks(Collection<String> orderIds) {
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().waitOrderStatusIs(orderIds, ProblemStatus.NEW);
        ApiSteps.replenishmentSteps().startWithdrawalReplenishmentJob();
        uiSteps.Login().PerformLogin();
        uiSteps.Replenishment().waitOrderStatusIs(orderIds, ProblemStatus.TASK_CREATED);
    }

    private void waitItemLocationKeyIs(LocationKey expectedLocationKey, String uit) {
        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            LocationKey locationKey = uiSteps.Balances().findLocationKeyByUit(uit);
            Assertions.assertNotNull(locationKey,
                    String.format("УИТ %s не существует. Возможно, УИТ покинул склад", uit));
            Assertions.assertEquals(expectedLocationKey, locationKey,
                    String.format("УИТ %s находится в %s, а должен был асинхронно переместиться в %s",
                            uit, locationKey, expectedLocationKey));
        }, Retrier.RETRIES_MEDIUM, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
    }

    @Step("Ждем, пока в назначение добавится товар на замену шортированного товара")
    private void waitForShortedItemReplacementInAssignment(String containerLabel, int expectedQty) {
        Retrier.retry(() -> {
            uiSteps.Login().PerformLogin();
            uiSteps.Order().resumeAssignment(containerLabel);

            Assertions.assertEquals(expectedQty,
                    uiSteps.Order().getCountOfItemsToPickInAssignment(),
                    "Не дождались, чтобы в назначении количество товаров стало " + expectedQty);
        }, Retrier.RETRIES_MEDIUM, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
    }

    public void verifyOrderStatus(Order order, ApiOrderStatus status) {
        verifyOrdersStatus(Collections.singletonList(order), status);
    }

    @Step("Проверяем статус что заказов {orders} равен {status}")
    public void verifyOrdersStatus(List<Order> orders, ApiOrderStatus status) {
        for (Order order : orders) {
            Retrier.retry(() -> ApiSteps.Order().verifyOrderStatus(order, status),
                    Retrier.RETRIES_SMALL, Retrier.TIMEOUT_SMALL, TimeUnit.SECONDS);
        }
    }
}
