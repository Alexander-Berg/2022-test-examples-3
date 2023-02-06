package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;

import java.util.Collections;
import java.util.HashMap;
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

@DisplayName("Selenium: Consecutive picking")
@Epic("Selenium Tests")
@Slf4j
public class ConsecutivePickingTest extends AbstractUiTest {
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
    private String areaKey;
    private String pickingZone;
    private String packingZone;
    private String shippingZone;
    private String storageCell1;
    private String storageCell2;
    private String singlesConsolidationCell;
    private String packingTable;
    private SortingStation sortingStationObj;
    private String sortingStation;
    private String sortingCell;
    private String shippingCell;
    private String shippingDoor;
    private String droppingCell;

    private WaveId firstWaveId;
    private WaveId secondWaveId;
    private HashMap<String, List<String>> containerToUitsMap = new HashMap<>();

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
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        // линия консолидации синглов
        singlesConsolidationCell = DatacreatorSteps.Location().createSinglesConsolidationCell(pickingZone);

        // сортировочная станция
        sortingStationObj = DatacreatorSteps.Location().createSortingStation(pickingZone);
        sortingStation = sortingStationObj.getStation();
        sortingCell = sortingStationObj.getSortLocations().get(0);

        //ячейка дропинга
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingCell = DatacreatorSteps.Location().createShippingStandardCell(shippingZone);

        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM1, storageCell1);
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM2, storageCell2);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {

        // удаление сортировочной станции
        DatacreatorSteps.Location().deleteSotingStation(sortingStation);

        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(singlesConsolidationCell);
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

        if (firstWaveId != null) {
            uiSteps.Login().PerformLogin();
            uiSteps.Wave().unreserveWave(firstWaveId);
        }

        if (secondWaveId != null) {
            uiSteps.Login().PerformLogin();
            uiSteps.Wave().unreserveWave(firstWaveId);
        }
    }

    @RetryableTest
    @Tag("PickingReleaseSuite")
    @Tag("PickingMultitestingSuite")
    @DisplayName("Тест создания и принудительного запуска сингловой волны на станцию: Один айтем")
    @ResourceLock("Тест создания и принудительного запуска сингловой волны на станцию: Один айтем")
    public void orderTestWithManualWave() {
        String firstContainerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        String secondContainerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        Order firstOrder = ApiSteps.Order().createTodayOrder(ITEM1);
        Order secondOrder = ApiSteps.Order().createTodayOrder(ITEM2);
        firstWaveId = processSteps.Outgoing().createAndForceStartSingleWaveManually(firstOrder, singlesConsolidationCell);
        secondWaveId = processSteps.Outgoing().createAndForceStartSingleWaveManually(secondOrder, singlesConsolidationCell);

        List<Order> orders = List.of(firstOrder, secondOrder);
        List<String> uitsOfFirstOrder = processSteps.Outgoing().pickSingleOrderAssignmentAcceptingNextTask(areaKey, firstContainerLabel);
        List<String> uitsOfSecondOrder = processSteps.Outgoing().pickSingleOrderAfterAcceptingNextTask(pickingZone, secondContainerLabel);
        containerToUitsMap.put(firstContainerLabel, uitsOfFirstOrder);
        containerToUitsMap.put(secondContainerLabel, uitsOfSecondOrder);

        processSteps.Outgoing().consolidateWaveContainerToLine(firstContainerLabel, singlesConsolidationCell);
        processSteps.Outgoing().consolidateWaveContainerToLine(secondContainerLabel, singlesConsolidationCell);
        List<ParcelId> packs = processSteps.Outgoing().packSeveralOrders(packingTable, containerToUitsMap);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);
        processSteps.Outgoing().verifyOrdersStatus(orders, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
        processSteps.Outgoing().shipOrder(firstOrder, dropId, shippingDoor, shippingCell);
        processSteps.Outgoing().verifyOrdersStatus(orders, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
    }
}
