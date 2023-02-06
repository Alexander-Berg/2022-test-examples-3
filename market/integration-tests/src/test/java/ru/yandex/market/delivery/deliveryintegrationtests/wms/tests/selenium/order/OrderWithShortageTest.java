package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;

import java.util.Collections;
import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ApiOrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Order with shortage flow")
@Epic("Selenium Tests")
@Slf4j
public class OrderWithShortageTest extends AbstractUiTest {
    private final String ITEM1_ARTICLE = UniqueId.getString();
    private final String ITEM2_ARTICLE = UniqueId.getString();
    private final Item ITEM1 = Item.builder()
            .sku(ITEM1_ARTICLE)
            .vendorId(1559)
            .article(ITEM1_ARTICLE)
            .quantity(1)
            .build();
    private final Item ITEM2 = Item.builder()
            .sku(ITEM2_ARTICLE)
            .vendorId(1559)
            .article(ITEM2_ARTICLE)
            .quantity(1)
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
    private String storageCell7;
    private String storageCell8;
    private String consolidationCell;
    private String packingTable;
    private SortingStation sortingStationObj;
    private String sortingStation;
    private String sortingCell;
    private String sortingCell2;
    private String shippingCell;
    private String shippingDoor;
    private String droppingCell;

    private WaveId waveId;
    private WaveId waveId2;

    @BeforeEach
    @Step("Подготовка: Создаем участок, выдаем разрешения," +
            "проверяем, что нет запущеных отборов и принимаем товар для заказа")
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
        storageCell7 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell8 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        consolidationCell = DatacreatorSteps.Location().createConsolidationCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        // сортировочная станция
        sortingStationObj = DatacreatorSteps.Location().createSortingStation(pickingZone);
        sortingStation = sortingStationObj.getStation();
        sortingCell = sortingStationObj.getSortLocations().get(0);
        sortingCell2 = sortingStationObj.getSortLocations().get(1);

        //ячейка дропинга
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingCell = DatacreatorSteps.Location().createShippingStandardCell(shippingZone);

        // Принимаем чуть больше товара для перерезерва после шортов
        int itemCopiesForInbound = 4;
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM1, storageCell1, itemCopiesForInbound);
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM2, storageCell2, itemCopiesForInbound);

        // перекладываем по одной штуке в другие ячейки для быстрого перерезерва после шорта
        String itemRov1 = ApiSteps.Stocks().getRovByStorerKeyAndManufaturerSku(1559, ITEM1_ARTICLE);
        String itemRov2 = ApiSteps.Stocks().getRovByStorerKeyAndManufaturerSku(1559, ITEM2_ARTICLE);

        uiSteps.Login().PerformLogin();
        List<String> snList1 = uiSteps.Balances().getUitListBySkuAndLoc(itemRov1, storageCell1);
        log.info("Got uits for {}: {}", itemRov1, snList1);
        uiSteps.Login().PerformLogin();
        List<String> snList2 = uiSteps.Balances().getUitListBySkuAndLoc(itemRov2, storageCell2);
        log.info("Got uits for {}: {}", itemRov2, snList2);

        String emptyTote = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().goToMovementMenu()
                .moveUIT(emptyTote, storageCell3, snList1.subList(0, 1))
                .moveUIT(emptyTote, storageCell4, snList2.subList(0, 1))
                .moveUIT(emptyTote, storageCell5, snList1.subList(1, 2))
                .moveUIT(emptyTote, storageCell6, snList2.subList(1, 2))
                .moveUIT(emptyTote, storageCell7, snList1.subList(2, 3))
                .moveUIT(emptyTote, storageCell8, snList2.subList(2, 3));
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {

        // удаление сортировочной станции
        DatacreatorSteps.Location().deleteSotingStation(sortingStation);

        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(consolidationCell);
        DatacreatorSteps.Location().deleteCell(storageCell8);
        DatacreatorSteps.Location().deleteCell(storageCell7);
        DatacreatorSteps.Location().deleteCell(storageCell6);
        DatacreatorSteps.Location().deleteCell(storageCell5);
        DatacreatorSteps.Location().deleteCell(storageCell4);
        DatacreatorSteps.Location().deleteCell(storageCell3);
        DatacreatorSteps.Location().deleteCell(storageCell2);
        DatacreatorSteps.Location().deleteCell(storageCell1);
        DatacreatorSteps.Location().deleteCell(droppingCell);
        DatacreatorSteps.Location().deleteCell(shippingCell);
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
        if (waveId2 != null) {
            uiSteps.Login().PerformLogin();
            uiSteps.Wave().unreserveWave(waveId2);
        }
    }

    @RetryableTest(duration = 22)
    @Tag("ConsolidationReleaseSuite")
    @Tag("DroppingReleaseSuite")
    @Tag("PackingReleaseSuite")
    @Tag("PickingReleaseSuite")
    @DisplayName("Тест заказа: два айтема. Спасение шорта на отборе, на консолидации и на упаковке")
    @ResourceLock("Тест заказа: два айтема. Спасение шорта на отборе, на консолидации и на упаковке")
    public void orderTestWithShortage() {
        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);
        Order order = ApiSteps.Order().createTodayOrder(ITEMS);
        waveId = processSteps.Outgoing().createAndForceStartWaveManually(order, sortingStation);

        int expectedItemsQuantity = ITEMS.stream().mapToInt(Item::getQuantity).sum();
        //Отбираем с шортированием (отработает автоматический дорезерв)
        List<String> itemSerials = processSteps.Outgoing().pickSingleOrderAssignmentWithShortage(areaKey,
                containerLabel, expectedItemsQuantity);

        //Консолидируем контейнер перед путволом
        processSteps.Outgoing().consolidateWaveContainerToLine(containerLabel, consolidationCell);

        //Разбираем контейнер в путвол и делаем шортирование
        List<String> serialsForShortage = List.of(itemSerials.get(0));
        itemSerials = processSteps.Outgoing()
                .sortFromContainerIntoPutwall(itemSerials, containerLabel,
                        sortingStation, sortingCell, serialsForShortage);
        processSteps.Outgoing().checkItemsLocAndNznAfterAsyncAction(serialsForShortage, "LOST", "");

        //Доотбираем то, что шортанули на консолидации. И тут же доконсолидируем.
        List<String> itemSerialsSecondPick = processSteps.Outgoing()
                .pickSingleOrderAssignmentWithRetry(areaKey, containerLabel);
        Assertions.assertEquals(expectedItemsQuantity-itemSerials.size(), itemSerialsSecondPick.size(),
                String.format("Не получилось добрать на линию предконсолидации остатки заказа. " +
                                "Надо %d шт, а добрали %d шт.",
                        expectedItemsQuantity-itemSerials.size(), itemSerialsSecondPick.size()));
        processSteps.Outgoing()
                .consolidateWaveAndOrder(itemSerialsSecondPick, consolidationCell, containerLabel,
                sortingStation, sortingCell);
        itemSerials.addAll(itemSerialsSecondPick);

        // Шортим один товар на упаковке
        List<String> stuckItems = List.of(itemSerials.get(0));
        List<String> lostItems = itemSerials.subList(1, itemSerials.size());
        processSteps.Outgoing().packOneOrderWithShort(stuckItems, packingTable, sortingCell, true);
        processSteps.Outgoing().checkItemsLocAndNznAfterAsyncAction(lostItems, "LOST", "");

        // Появляется задание на отмененку
        String containerForCancelledLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);
        processSteps.Outgoing().moveCancelledItems(stuckItems, containerForCancelledLabel, packingTable);
        processSteps.Outgoing().checkItemsLocAndNznAfterAsyncAction(stuckItems, "INTRANSIT", containerForCancelledLabel);

        // Запускаем повторно заказ и дальше делаем как обычно
        waveId2 = processSteps.Outgoing().createAndForceStartWaveManually(order, sortingStation);
        itemSerials = processSteps.Outgoing().pickSingleOrderAssignmentRefusingNextTask(areaKey, containerLabel);
        processSteps.Outgoing().consolidateWaveAndOrder(itemSerials, consolidationCell, containerLabel,
                sortingStation, sortingCell2);
        List<ParcelId> packs = processSteps.Outgoing().packOrder(itemSerials, packingTable);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
        processSteps.Outgoing().shipOrder(order, dropId, shippingDoor, shippingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
    }
}
