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
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ApiOrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Order with Multi Picking")
@Epic("Selenium Tests")
@Slf4j
public class MultiPickingOrderTest extends AbstractUiTest {
    private final String ITEM_ARTICLE1 = UniqueId.getString();
    private final Item ITEM1 = Item.builder()
            .sku(ITEM_ARTICLE1)
            .vendorId(1559)
            .article(ITEM_ARTICLE1)
            .quantity(2)
            .build();
    private final String ITEM_ARTICLE2 = UniqueId.getString();
    private final Item ITEM2 = Item.builder()
            .sku(ITEM_ARTICLE2)
            .vendorId(1559)
            .article(ITEM_ARTICLE2)
            .quantity(2)
            .build();
    private final List<Item> items = List.of(ITEM1, ITEM2);

    // чтобы изменить количество заказов нужно добавить/убрать запросы на создание в списке filePathList
    private final int numberOfOrders = 2;
    private String areaKey;
    private String pickingZone;
    private String packingZone;
    private String shippingZone;
    private String storageCell1;
    private String storageCell2;
    private String consolidationCell;
    private String packingTable;
    private SortingStation sortingStationObj;
    private String sortingStation;
    private String sortingCell;
    private String shippingCell;
    private String shippingDoor;
    private String droppingCell;

    private List<Order> orders = new ArrayList<>();
    private List<WaveId> waves = new ArrayList<>();
    private Map<String, List<String>> containerItemsMap = new LinkedHashMap<>();
    private Map<String, Order> containerToOrderMap = new LinkedHashMap<>();

    @BeforeEach
    @Step("Подготовка: Создаем участок, выдаем разрешения," +
            "проверяем, что нет запущеных отборов и принимаем товар для заказа")
    public void setUp() {
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        packingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        shippingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // ячейки отбора, консолидации и стол упаковки
        storageCell1 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        storageCell2 = DatacreatorSteps.Location().createPickingCell(pickingZone);
        consolidationCell = DatacreatorSteps.Location().createConsolidationCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

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
        DatacreatorSteps.Location().deleteCell(consolidationCell);
        DatacreatorSteps.Location().deleteCell(storageCell2);
        DatacreatorSteps.Location().deleteCell(storageCell1);
        DatacreatorSteps.Location().deleteCell(droppingCell);
        DatacreatorSteps.Location().deleteCell(shippingCell);
        DatacreatorSteps.Location().deleteCell(shippingDoor);

        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(packingZone);
        DatacreatorSteps.Location().deletePutawayZone(pickingZone);
        DatacreatorSteps.Location().deletePutawayZone(shippingZone);

        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey);

        for (WaveId waveId : waves) {
            if (waveId != null) {
                uiSteps.Login().PerformLogin();
                uiSteps.Wave().unreserveWave(waveId);
            }
        }
    }

    private void createOrdersAndStartWaves() {
        for (int i = 0; i < numberOfOrders; i++) {
            Order order = ApiSteps.Order().createTodayOrder(items.get(i));
            orders.add(order);
            WaveId waveId = processSteps.Outgoing().createAndForceStartWaveManually(order, sortingStation);
            waves.add(waveId);
        }
    }

    private void mapContainerLabelsToItemsAndOrders() {
        for (Order order : orders) {
            String container = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
            containerItemsMap.put(container, new ArrayList<>());
            containerToOrderMap.put(container, order);
        }

        log.info("Map of containers to orders {}", containerToOrderMap.toString());
    }

    @RetryableTest
    @Tag("PickingReleaseSuite")
    @Tag("PickingMultitestingSuite")
    @DisplayName("Тест заказа: Два назначения, четыре товара")
    @ResourceLock("Тест заказа: Два назначения, четыре товара")
    public void orderTest() {
        Order lastOrder = null;
        createOrdersAndStartWaves();
        mapContainerLabelsToItemsAndOrders();

        Set<String> containers = containerToOrderMap.keySet();
        processSteps.Outgoing()
                .pickMultipleAssignments(containerItemsMap, containers, areaKey);

        for (String containerLabel : containerToOrderMap.keySet()) {
            Order order = containerToOrderMap.get(containerLabel);
            List<String> itemSerials = containerItemsMap.get(containerLabel);
            processSteps.Outgoing().consolidateWaveAndOrder(itemSerials, consolidationCell, containerLabel,
                    sortingStation, sortingCell);

            List<ParcelId> packs = processSteps.Outgoing().packOrder(itemSerials, packingTable);
            DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);
            processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
            processSteps.Outgoing().shipOrder(order, dropId, shippingDoor, shippingCell);
            processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
        }
    }
}
