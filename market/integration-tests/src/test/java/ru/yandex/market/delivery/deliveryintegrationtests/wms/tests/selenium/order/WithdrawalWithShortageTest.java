package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.data.util.Pair;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.StockType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Withdrawal with shortage flow")
@Epic("Selenium Tests")
@Slf4j
public class WithdrawalWithShortageTest extends AbstractUiTest {
    private final String ITEM1_ARTICLE = UniqueId.getString();
    private final String ITEM2_ARTICLE = UniqueId.getString();
    private final int EACH_ITEMS_QTY = 2;
    private final Item ITEM1 = Item.builder()
            .sku(ITEM1_ARTICLE)
            .vendorId(1559)
            .article(ITEM1_ARTICLE)
            .quantity(EACH_ITEMS_QTY)
            .build();
    private final Item ITEM2 = Item.builder()
            .sku(ITEM2_ARTICLE)
            .vendorId(1559)
            .article(ITEM2_ARTICLE)
            .quantity(EACH_ITEMS_QTY)
            .build();
    private final List<Item> ITEMS = List.of(ITEM1, ITEM2);
    private String areaKey;
    private String pickingZone;
    private String packingZone;
    private String shippingZone;
    private String storageCell1;
    private String storageCell2;
    private String storageCell3;
    private String storageCell4;
    private String storageCell5;
    private String storageCell6;
    private String consolidationCell;
    private String packingTable;
    private SortingStation sortingStationObj;
    private String sortingStation;
    private String sortingCell;
    private String sortingCell2;
    private String shippingWithdrawalCell;
    private String shippingWithdrawalCell2;
    private String shippingDoor;
    private String droppingCell;

    private WaveId waveId;
    private List<String> snList1;
    private List<String> snList2;
    private String lot1;
    private String lot2;
    private String containerId;
    private List<String> takenItemSerials;

    @BeforeEach
    @Step("Подготовка, принимаем товар для заказа")
    public void setUp() throws Exception {
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        packingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        shippingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // ячейки отбора, консолидации и стол упаковки
        storageCell1 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell2 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell3 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell4 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell5 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell6 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        consolidationCell = DatacreatorSteps.Location().createConsolidationCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        // сортировочная станция
        sortingStationObj = DatacreatorSteps.Location().createSortingStation(pickingZone);
        sortingStation = sortingStationObj.getStation();
        sortingCell = sortingStationObj.getSortLocations().get(0);
        sortingCell2 = sortingStationObj.getSortLocations().get(1);
        DatacreatorSteps.Location().updateSortStationMode(sortingStation, AutoStartSortingStationMode.WITHDRAWALS);

        //ячейка дропинга
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingWithdrawalCell = DatacreatorSteps.Location().createShippingWithdrawalCell(shippingZone);
        shippingWithdrawalCell2 = DatacreatorSteps.Location().createShippingWithdrawalCell(shippingZone);

        createBalancesInCells();

        containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);
    }

    @Step("Создаём товары в ячейках. Количество товаров такое, чтобы хватило на перерезерв и случилась корректировка")
    private void createBalancesInCells() {
        int itemCopiesForInbound = 3;
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM1, storageCell1, itemCopiesForInbound);
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM2, storageCell2, itemCopiesForInbound);

        // перекладываем по одной штуке в другие ячейки для быстрого перерезерва после шорта
        String itemRov1 = ApiSteps.Stocks().getRovByStorerKeyAndManufaturerSku(1559, ITEM1_ARTICLE);
        String itemRov2 = ApiSteps.Stocks().getRovByStorerKeyAndManufaturerSku(1559, ITEM2_ARTICLE);

        uiSteps.Login().PerformLogin();
        snList1 = uiSteps.Balances().getUitListBySkuAndLoc(itemRov1, storageCell1);
        log.info("Got uits for {}: {}", itemRov1, snList1);
        lot1 = DatacreatorSteps.Items().getItemLotBySerialNumber(snList1.get(0));

        uiSteps.Login().PerformLogin();
        snList2 = uiSteps.Balances().getUitListBySkuAndLoc(itemRov2, storageCell2);
        log.info("Got uits for {}: {}", itemRov2, snList2);
        lot2 = DatacreatorSteps.Items().getItemLotBySerialNumber(snList2.get(0));

        String emptyTote = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);
        uiSteps.Login().PerformLogin();
        uiSteps.Placement().goToMovementMenu()
                .moveUIT(emptyTote, storageCell3, snList1.subList(0, 1))
                .moveUIT(emptyTote, storageCell4, snList2.subList(0, 1))
                .moveUIT(emptyTote, storageCell5, snList1.subList(1, 2))
                .moveUIT(emptyTote, storageCell6, snList2.subList(1, 2));
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление сортировочной станции
        DatacreatorSteps.Location().deleteSotingStation(sortingStation);

        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(consolidationCell);
        DatacreatorSteps.Location().deleteCell(storageCell6);
        DatacreatorSteps.Location().deleteCell(storageCell5);
        DatacreatorSteps.Location().deleteCell(storageCell4);
        DatacreatorSteps.Location().deleteCell(storageCell3);
        DatacreatorSteps.Location().deleteCell(storageCell2);
        DatacreatorSteps.Location().deleteCell(storageCell1);
        DatacreatorSteps.Location().deleteCell(droppingCell);
        DatacreatorSteps.Location().deleteCell(shippingWithdrawalCell);
        DatacreatorSteps.Location().deleteCell(shippingWithdrawalCell2);
        DatacreatorSteps.Location().deleteCell(shippingDoor);

        // удаление зон
        DatacreatorSteps.Location().deletePickToInventoryTasks(Collections.singleton(pickingZone));
        DatacreatorSteps.Location().deletePutawayZone(pickingZone);
        DatacreatorSteps.Location().deletePutawayZone(packingZone);
        DatacreatorSteps.Location().deletePutawayZone(shippingZone);

        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey);

        if (waveId != null) {
            uiSteps.Login().PerformLogin();
            uiSteps.Wave().unreserveWave(waveId);
        }
    }

    @RetryableTest(duration = 22)
    @Tag("ConsolidationReleaseSuite")
    @Tag("PackingReleaseSuite")
    @Tag("PickingReleaseSuite")
    @DisplayName("Тест изъятия, FIT сток. Спасение шорта на отборе, на консолидации. На упаковке шорт не спасаем.")
    @ResourceLock("Тест изъятия, FIT сток. Спасение шорта на отборе, на консолидации. На упаковке шорт не спасаем.")
    public void withdrawalFitWithShortageTest() {
        Outbound outbound1 = ApiSteps.Outbound().createOutbound(ITEM1);
        Outbound outbound2 = ApiSteps.Outbound().createOutbound(ITEM2);
        List<Outbound> outbounds = List.of(outbound1, outbound2);
        List<String> ordersIds = outbounds.stream().map(Outbound::getFulfillmentId).collect(Collectors.toList());

        waveId = processSteps.Outgoing().createAndForceStartWaveManuallyByOrderIds(ordersIds, sortingStation);

        boolean isFirstOrderShortedAtPicking = pickAndConsolidateWithShortageAndRereserve();

        boolean isFirstOrderToBeAdjusted = isFirstOrderShortedAtPicking;
        int expectedQtyAdjust = 1;
        int shortedAtConsQty = sortItemsWithShortage(ordersIds, isFirstOrderToBeAdjusted, 1);

        boolean isFirstOrderNeedPickingAgain = !isFirstOrderShortedAtPicking;
        repickConsolidateSort(shortedAtConsQty - expectedQtyAdjust, isFirstOrderNeedPickingAgain);

        var isFirstOrderShortAtPack = !isFirstOrderShortedAtPicking;
        List<String> lostAtPackingItems = shortOneOrderAtPacking(isFirstOrderShortAtPack);

        Outbound outboundToShip = isFirstOrderShortAtPack ? outbound2 : outbound1;
        packAndDropAndShipSurvivedOrder(outboundToShip);

        String orderIdShortenedAtPacking = isFirstOrderShortAtPack ? ordersIds.get(0) : ordersIds.get(1);
        delayedAsyncCheck(orderIdShortenedAtPacking, lostAtPackingItems);
    }

    @RetryableTest(duration = 22)
    @Tag("ConsolidationReleaseSuite")
    @Tag("PackingReleaseSuite")
    @Tag("PickingReleaseSuite")
    @DisplayName("Тест изъятия, EXPIRED сток. Спасение шорта на отборе, на консолидации. На упаковке шорт не спасаем.")
    @ResourceLock("Тест изъятия, EXPIRED сток. Спасение шорта на отборе, на консолидации. На упаковке шорт не спасаем.")
    public void withdrawalExpiredWithShortageTest() {
        DatacreatorSteps.Items().placeHoldOnLot(lot1, InventoryHoldStatus.EXPIRED);
        DatacreatorSteps.Items().placeHoldOnLot(lot2, InventoryHoldStatus.EXPIRED);
        Outbound outbound1 = ApiSteps.Outbound().createOutbound(ITEM1, StockType.EXPIRED);
        Outbound outbound2 = ApiSteps.Outbound().createOutbound(ITEM2, StockType.EXPIRED);
        List<Outbound> outbounds = List.of(outbound1, outbound2);
        List<String> ordersIds = outbounds.stream().map(Outbound::getFulfillmentId).collect(Collectors.toList());

        waveId = processSteps.Outgoing().createAndForceStartWaveManuallyByOrderIds(ordersIds, sortingStation);

        boolean isFirstOrderShortedAtPicking = pickAndConsolidateWithShortageAndRereserve();

        boolean isFirstOrderToBeAdjusted = isFirstOrderShortedAtPicking;
        int expectedQtyAdjust = 1;
        int shortedAtConsQty = sortItemsWithShortage(ordersIds, isFirstOrderToBeAdjusted, 1);

        boolean isFirstOrderNeedPickingAgain = !isFirstOrderShortedAtPicking;
        repickConsolidateSort(shortedAtConsQty - expectedQtyAdjust, isFirstOrderNeedPickingAgain);

        var isFirstOrderShortAtPack = !isFirstOrderShortedAtPicking;
        List<String> lostAtPackingItems = shortOneOrderAtPacking(isFirstOrderShortAtPack);

        Outbound outboundToShip = isFirstOrderShortAtPack ? outbound2 : outbound1;
        packAndDropAndShipSurvivedOrder(outboundToShip);

        String orderIdShortenedAtPacking = isFirstOrderShortAtPack ? ordersIds.get(0) : ordersIds.get(1);
        delayedAsyncCheck(orderIdShortenedAtPacking, lostAtPackingItems);
    }

    @Step("Проверяем асинхрон, который можно проверить попозже.")
    private void delayedAsyncCheck(String orderShortenedAtPacking, List<String> lostAtPackingItems) {
        processSteps.Outgoing().checkItemsLocAndNznAfterAsyncAction(lostAtPackingItems, "LOST", "");
        // пока шортанутый на упаковке заказ стопорится в статусе OrderStatus.NOT_STARTED, не будем пытаться его спасти
        processSteps.Outgoing().waitOrderStatusIs(orderShortenedAtPacking, OrderStatus.NOT_STARTED);
    }

    @Step("Пакуем, дропаем, шипаем живой заказ")
    private void packAndDropAndShipSurvivedOrder(Outbound outboundToShip) {
        final var snToCell = new HashMap<String, String>();
        getIntersection(takenItemSerials, snList1).forEach(sn -> snToCell.put(sn, sortingCell));
        getIntersection(takenItemSerials, snList2).forEach(sn -> snToCell.put(sn, sortingCell2));

        List<ParcelId> packs =         processSteps.Outgoing().packOrders(snToCell, packingTable);
        List<DropId> dropIds = List.of(processSteps.Outgoing().dropOrder(packs.get(0), droppingCell));
                                       processSteps.Outgoing().shipWithdrawals(List.of(outboundToShip), dropIds,
                                               shippingDoor, List.of(shippingWithdrawalCell, shippingWithdrawalCell2));
    }

    @NotNull
    @Step("Шортируем один заказ на упаковке")
    private List<String> shortOneOrderAtPacking(boolean isFirstOrderShortAtPack) {
        var serialsForVictimOrder = getIntersection(takenItemSerials, isFirstOrderShortAtPack ? snList1 : snList2);
        var cellForVictimOrder = isFirstOrderShortAtPack ? sortingCell : sortingCell2;

        List<String> stuckItems = serialsForVictimOrder.subList(1, serialsForVictimOrder.size());
        List<String> lostAtPackingItems = List.of(serialsForVictimOrder.get(0));
        processSteps.Outgoing().packOneOrderWithShort(stuckItems, packingTable,
                cellForVictimOrder, false);

        getAndExecuteStuckItemsTask(stuckItems);
        takenItemSerials.removeAll(serialsForVictimOrder);//убираем УИТы заказа, который не пережил упаковку
        return lostAtPackingItems;
    }

    @Step("Выполняем задание на отмененку (STUCK-задание)")
    private void getAndExecuteStuckItemsTask(List<String> stuckItems) {
        String containerForCancelledLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);
        processSteps.Outgoing().moveCancelledItems(stuckItems, containerForCancelledLabel, packingTable);
        processSteps.Outgoing().checkItemsLocAndNznAfterAsyncAction(stuckItems, "INTRANSIT", containerForCancelledLabel);
    }

    @NotNull
    @Step("Отбираем, консолидируем, сортируем снова, после перерезерва и автокоррекции при шорте на консолидации")
    private void repickConsolidateSort(int expectedQtyToPick, boolean isFirstOrderRePicked) {
        List<String> itemSerialsSecondPick =
                processSteps.Outgoing().pickWithdrawalAssignmentWithRetry(areaKey, containerId);
        Assertions.assertEquals(expectedQtyToPick, itemSerialsSecondPick.size(),
                String.format("Не получилось добрать на линию предконсолидации остатки заказа. " +
                                "Надо %d шт, а добрали %d шт.", expectedQtyToPick, itemSerialsSecondPick.size()));
        takenItemSerials.addAll(itemSerialsSecondPick);
        String targetCell = isFirstOrderRePicked ? sortingCell : sortingCell2;
        Allure.step(String.format("Доотобрали %s заказ, доложим товар в ячейку %s",
                isFirstOrderRePicked ? "первый" : "второй", targetCell));
        processSteps.Outgoing().consolidateWaveAndOrder(itemSerialsSecondPick, consolidationCell, containerId,
                        sortingStation, targetCell);
    }

    @NotNull
    @Step("Сортируем товары в путвол и шортим по одному товару из каждого заказа.")
    private int sortItemsWithShortage(List<String> ordersIds, boolean isFirstOrderToBeAdjusted,
                                      int expectedQtyAdjust) {
        final List<String> serialsForShortageAtCons = new ArrayList<>();
        serialsForShortageAtCons.addAll(getAnySingleIntersection(takenItemSerials, snList1));
        serialsForShortageAtCons.addAll(getAnySingleIntersection(takenItemSerials, snList2));

        Allure.step(String.format("Первый заказ кладём в ячейку %s. Второй заказ кладём в ячкейку %s",
                sortingCell, sortingCell2));
        final var itemSerialsToSortingCellsMapping = new HashMap<String, String>();
        getIntersection(takenItemSerials, snList1).forEach(sn -> itemSerialsToSortingCellsMapping.put(sn, sortingCell));
        getIntersection(takenItemSerials, snList2).forEach(sn -> itemSerialsToSortingCellsMapping.put(sn, sortingCell2));
        takenItemSerials = processSteps.Outgoing()
                .sortFromContainerIntoPutwall(itemSerialsToSortingCellsMapping, containerId,
                        sortingStation, serialsForShortageAtCons);

        // Убеждаемся, что отработала очередь move-to-lost для случая на консолидации. В асинхроне будет и дорезерв.
        processSteps.Outgoing().checkItemsLocAndNznAfterAsyncAction(serialsForShortageAtCons, "LOST", "");
        int shortedAtConsQty =  serialsForShortageAtCons.size();

        Allure.step(String.format("У %s заказа ожидается автокоррекция с %d товаров до %d товара",
                isFirstOrderToBeAdjusted ? "первого": "второго",
                EACH_ITEMS_QTY, EACH_ITEMS_QTY - expectedQtyAdjust));
        String orderAutocorrectedAtCons = isFirstOrderToBeAdjusted ? ordersIds.get(0) : ordersIds.get(1);
        processSteps.Outgoing().waitOrderTotalQtyIs(orderAutocorrectedAtCons, EACH_ITEMS_QTY - expectedQtyAdjust);
        return shortedAtConsQty;
    }

    @Step("Отбираем назначение, шортируем, ждём дорезерв, доотбираем, кладём у путвола.")
    private boolean pickAndConsolidateWithShortageAndRereserve() {
        int expectedItemsQuantity = ITEMS.stream().mapToInt(Item::getQuantity).sum();
        Pair<String, String> shortedData = processSteps.Outgoing().pickWithdrawalAssignmentDoShortageWaitReplacement(
                areaKey, containerId, expectedItemsQuantity);
        takenItemSerials = processSteps.Outgoing().resumeAndCompleteWithdrawalAssignment();
        boolean isFirstOrderShortedAtPicking = lot1.endsWith(shortedData.getSecond());
        Allure.step(String.format("Шорт случился с %s заказом", isFirstOrderShortedAtPicking ? "первым": "вторым"));
        processSteps.Outgoing().consolidateWaveContainerToLine(containerId, consolidationCell);
        return isFirstOrderShortedAtPicking;
    }

    private List<String> getAnySingleIntersection(List<String> list1, List<String> list2) {
        return list1.stream().filter(sn -> list2.contains(sn)).limit(1).toList();
    }

    private List<String> getIntersection(List<String> list1, List<String> list2) {
        return list1.stream().filter(sn -> list2.contains(sn)).toList();
    }
}
