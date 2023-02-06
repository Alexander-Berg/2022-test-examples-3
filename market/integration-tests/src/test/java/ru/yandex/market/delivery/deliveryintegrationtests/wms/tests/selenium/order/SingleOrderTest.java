package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Single Order")
@Epic("Selenium Tests")
@Slf4j
public class SingleOrderTest extends AbstractUiTest {

    private final String ITEM_ARTICLE = UniqueId.getString();
    private final Item ITEM = Item.builder()
            .sku(ITEM_ARTICLE)
            .vendorId(1559)
            .article(ITEM_ARTICLE)
            .quantity(1)
            .build();
    private String areaKey;
    private String pickingZone;
    private String packingZone;
    private String shippingZone;
    private String storageCell;
    private String storageCell2;
    private String singlesConsolidationCell;
    private String packingTable;
    private String shippingCell;
    private String shippingDoor;
    private String droppingCell;

    private WaveId waveId;

    private Map<String, List<String>> containerItemsMap = new LinkedHashMap<>();
    private Map<String, Order> containerToOrderMap = new LinkedHashMap<>();

    @BeforeEach
    @Step("Подготовка: Создаем участок, выдаем разрешения," +
            "проверяем, что нет запущеных отборов и принимаем товар для заказа")
    public void setUp() throws Exception {
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зоны отбора, упаковки и отгрузки
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        packingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        shippingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // ячейка отбора и стол упаковки
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell2 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        // линия консолидации синглов
        singlesConsolidationCell = DatacreatorSteps.Location().createSinglesConsolidationCell(pickingZone);

        //ячейка дропинга
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingCell = DatacreatorSteps.Location().createShippingStandardCell(shippingZone);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(singlesConsolidationCell);
        DatacreatorSteps.Location().deleteCell(storageCell);
        DatacreatorSteps.Location().deleteCell(droppingCell);
        DatacreatorSteps.Location().deleteCell(shippingCell);
        DatacreatorSteps.Location().deleteCell(shippingDoor);

        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(packingZone);
        DatacreatorSteps.Location().deletePutawayZone(pickingZone);
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
    @Tag("PackingReleaseSuite")
    @Tag("AutostartMultitestingSuite")
    @Tag("ConsolidationMultitestingSuite")
    @Tag("PackingMultitestingSuite")
    @DisplayName("Тест создания и принудительного запуска сингловой волны на станцию: Один айтем")
    @ResourceLock("Тест создания и принудительного запуска сингловой волны на станцию: Один айтем")
    public void orderTestWithManualWave() {
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell);
        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        Order order = ApiSteps.Order().createTodayOrder(ITEM);
        waveId = processSteps.Outgoing().createAndForceStartSingleWaveManually(order, singlesConsolidationCell);
        List<String> itemSerials = processSteps.Outgoing().pickSingleOrderAssignmentRefusingNextTask(areaKey, containerLabel);
        processSteps.Outgoing().consolidateWaveContainerToLine(containerLabel, singlesConsolidationCell);
        List<ParcelId> packs = processSteps.Outgoing().packNonsortOrder(containerLabel, itemSerials, packingTable);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
        processSteps.Outgoing().shipOrder(order, dropId, shippingDoor, shippingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
    }

    @RetryableTest
    @Tag("AutostartReleaseSuite")
    @Tag("ConsolidationReleaseSuite")
    @Tag("PackingReleaseSuite")
    @DisplayName("Тест создания и принудительного запуска сингловой волны на станцию два заказа: Один айтем")
    @ResourceLock("Тест создания и принудительного запуска сингловой волны на станцию два заказа: Один айтем")
    public void twoOrdersTestWithManualWave() {
        Order lastOrder = null;
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell, 1);
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell2, 1);

        Order order1 = ApiSteps.Order().createTodayOrder(ITEM);
        Order order2 = ApiSteps.Order().createTodayOrder(ITEM);

        List<Order> orders = List.of(order1, order2);

        waveId = processSteps.Outgoing().createAndForceStartSingleWaveManuallyWithTwoOrders(orders, singlesConsolidationCell);

        for (Order order : orders) {
            String container = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
            containerItemsMap.put(container, new ArrayList<>());
            containerToOrderMap.put(container, order);
        }

            Set<String> containers = containerToOrderMap.keySet();
            processSteps.Outgoing()
                    .pickMultipleAssignmentsSingleOrders(containerItemsMap, containers, areaKey);

        for (String containerLabel : containerToOrderMap.keySet()) {
            Order order = containerToOrderMap.get(containerLabel);
            List<String> itemSerials = containerItemsMap.get(containerLabel);
            processSteps.Outgoing().consolidateWaveContainerToLine(containerLabel, singlesConsolidationCell);
            List<ParcelId> packs = processSteps.Outgoing().packNonsortOrder(containerLabel, itemSerials, packingTable);
            DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);
            processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
            processSteps.Outgoing().shipOrder(order, dropId, shippingDoor, shippingCell);
            processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
        }
    }
}
