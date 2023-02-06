package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ApiOrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SorterExit;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Order with shipping conveyor")
@Epic("Selenium Tests")
@Slf4j
public class OrderWithShippingConveyorTest extends AbstractUiTest {

    private final String ITEM_ARTICLE = UniqueId.getString();
    private final Item ITEM = Item.builder()
            .sku(ITEM_ARTICLE)
            .vendorId(1559)
            .article(ITEM_ARTICLE)
            .quantity(2)
            .build();

    private String areaKey;
    private String pickingZone;
    private String packingZone;
    private String shippingZone;
    private String storageCell;
    private String consolidationCell;
    private String packingTable;
    private SortingStation sortingStationObj;
    private String sortingStation;
    private String sortingCell;
    private String conveyorTransit;
    private SorterExit targetSorterLocation;
    private SorterExit alternateAndErrorSorterLocation;
    private WaveId waveId;
    private String shippingCell;
    private String shippingDoor;
    private String droppingCell;

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

        // выходы сортировочного конвейера и транзитной локации
        targetSorterLocation = DatacreatorSteps.Location().createSorterExit(packingZone, false, false);
        alternateAndErrorSorterLocation = DatacreatorSteps.Location().createSorterExit(packingZone, true, true);
        conveyorTransit = DatacreatorSteps.Location().createConveyorTransit(packingZone);

        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление сортирововчного конвейера и ордеров на нем
        DatacreatorSteps.Location().deleteSorterExit(targetSorterLocation.getSorterExitKey());
        DatacreatorSteps.Location().deleteSorterExit(alternateAndErrorSorterLocation.getSorterExitKey());

        // удаление сортировочной станции
        DatacreatorSteps.Location().deleteSotingStation(sortingStation);

        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(consolidationCell);
        DatacreatorSteps.Location().deleteCell(storageCell);
        DatacreatorSteps.Location().deleteCell(conveyorTransit);
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
    @DisplayName("Тест заказа с новым отбором и новой упаковкой с конвейером: Один айтем, две единицы товара")
    @ResourceLock("Тест заказа с новым отбором и новой упаковкой с конвейером: Один айтем, две единицы товара")
    public void orderTestWithNewPickingAndNewPacking() {
        String containerLabel = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        Order order = ApiSteps.Order().createTodayOrder(ITEM);
        waveId = processSteps.Outgoing().createAndForceStartWaveManually(order, sortingStation);
        List<String> itemSerials = processSteps.Outgoing().pickSingleOrderAssignmentRefusingNextTask(areaKey, containerLabel);
        processSteps.Outgoing().consolidateWaveAndOrder(itemSerials, consolidationCell, containerLabel,
                sortingStation, sortingCell);
        List<ParcelId> packs = processSteps.Outgoing().packOrder(itemSerials, packingTable);
        processSteps.Outgoing().checkSorterOrderLocation(packs, targetSorterLocation);
        DropId dropId = processSteps.Outgoing().dropOrders(packs, droppingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_PLACES_CHANGED);
        processSteps.Outgoing().shipOrder(order, dropId, shippingDoor, shippingCell);
        processSteps.Outgoing().verifyOrderStatus(order, ApiOrderStatus.SORTING_CENTER_TRANSMITTED);
    }
}
