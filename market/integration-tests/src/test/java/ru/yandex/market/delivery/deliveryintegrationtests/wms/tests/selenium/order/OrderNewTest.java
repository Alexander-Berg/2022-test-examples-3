package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
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

@DisplayName("Selenium: New Order")
@Epic("Selenium Tests")
@Slf4j
public class OrderNewTest extends AbstractUiTest {
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
    private String storageCell;
    private String storageCell1;
    private String storageCell2;
    private String consolidationCell;
    private String packingTable;
    private SortingStation sortingStationObj;
    private String sortingStation;
    private String sortingCell;
    private String shippingCell;
    private String otherSortingCell;
    private String otherShippingCell;
    private String shippingDoor;
    private String droppingCell;

    private WaveId waveId;

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
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell1 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell2 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        consolidationCell = DatacreatorSteps.Location().createConsolidationCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        // сортировочная станция
        sortingStationObj = DatacreatorSteps.Location().createSortingStation(pickingZone);
        sortingStation = sortingStationObj.getStation();
        sortingCell = sortingStationObj.getSortLocations().get(0);
        otherSortingCell = sortingStationObj.getSortLocations().get(1);

        //ячейка дропинга
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingCell = DatacreatorSteps.Location().createShippingStandardCell(shippingZone);
        otherShippingCell = DatacreatorSteps.Location().createShippingStandardCell(shippingZone);

        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM1, storageCell1, 3);
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM2, storageCell2, 3);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление сортировочной станции
        DatacreatorSteps.Location().deleteSotingStation(sortingStation);

        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(consolidationCell);
        DatacreatorSteps.Location().deleteCell(storageCell2);
        DatacreatorSteps.Location().deleteCell(storageCell1);
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
    }

    @RetryableTest
    @Tag("AutostartReleaseSuite")
    @Tag("ConsolidationReleaseSuite")
    @Tag("DroppingReleaseSuite")
    @Tag("PackingReleaseSuite")
    @Tag("ShippingReleaseSuite")
    @Tag("PickingReleaseSuite")
    @Tag("AutostartMultitestingSuite")
    @Tag("ConsolidationMultitestingSuite")
    @Tag("DroppingMultitestingSuite")
    @Tag("PackingMultitestingSuite")
    @Tag("PickingMultitestingSuite")
    @Tag("ShippingMultitestingSuite")
    @DisplayName("Тест заказа: Два айтема")
    @ResourceLock("Тест заказа: Два айтема")
    public void orderTestWithNewPickingAndNewPackingAutoWave() {
        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        Order order = ApiSteps.Order().createTodayOrder(ITEMS);
        waveId = processSteps.Outgoing().createAndForceStartWaveManually(order, sortingStation);
        List<String> itemSerials = processSteps.Outgoing().pickSingleOrderAssignmentRefusingNextTask(areaKey, containerLabel);
        processSteps.Outgoing().consolidateWaveAndOrder(itemSerials, consolidationCell, containerLabel,
                sortingStation, sortingCell);
        List<ParcelId> packs = processSteps.Outgoing().packOrder(itemSerials, packingTable);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
        processSteps.Outgoing().shipOrder(order, dropId, shippingDoor, shippingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
    }

    @RetryableTest
    @Tag("ConsolidationReleaseSuite")
    @Tag("PackingReleaseSuite")
    @Tag("ShippingReleaseSuite")
    @Tag("ConsolidationMultitestingSuite")
    @Tag("PackingMultitestingSuite")
    @Tag("ShippingMultitestingSuite")
    @DisplayName("Тест с двумя заказами: Два айтема и совместной упаковкой и отгрузкой")
    @ResourceLock("Тест с двумя заказами: Два айтема и совместной упаковкой и отгрузкой")
    public void orderTestWithTwoOrdersAndPacking() {
        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Order order1 = ApiSteps.Order().createTodayOrder(ITEMS);
        Order order2 = ApiSteps.Order().createTodayOrder(ITEMS);

        List<Order> orders = List.of(order1, order2);

        waveId = processSteps.Outgoing().createAndForceStartWaveManually(orders, sortingStation);
        List<String> itemSerials = processSteps.Outgoing().pickSingleOrderAssignmentRefusingNextTask(areaKey, containerLabel).stream().sorted().toList();
        var itemSerialsToSortingCellsMapping = new LinkedHashMap<String, String>();
        itemSerialsToSortingCellsMapping.put(itemSerials.get(0), sortingCell);
        itemSerialsToSortingCellsMapping.put(itemSerials.get(1), otherSortingCell);
        itemSerialsToSortingCellsMapping.put(itemSerials.get(2), sortingCell);
        itemSerialsToSortingCellsMapping.put(itemSerials.get(3), otherSortingCell);
        processSteps.Outgoing().consolidateWaveAndOrders(itemSerialsToSortingCellsMapping, consolidationCell,
                containerLabel, sortingStation);
        List<ParcelId> packs = processSteps.Outgoing().packOrders(itemSerialsToSortingCellsMapping, packingTable);

        List<DropId> dropIds = List.of(
                processSteps.Outgoing().dropOrder(packs.get(0), droppingCell),
                processSteps.Outgoing().dropOrder(packs.get(1), droppingCell)
        );
        processSteps.Outgoing().verifyOrdersStatus(orders, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
        processSteps.Outgoing().shipOrders(orders, dropIds, shippingDoor,
                List.of(shippingCell, otherShippingCell));
        processSteps.Outgoing().verifyOrdersStatus(orders, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
    }
}

