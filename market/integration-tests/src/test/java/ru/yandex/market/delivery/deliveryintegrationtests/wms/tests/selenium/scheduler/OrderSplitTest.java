package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.scheduler;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;


@DisplayName("Selenium: Split Order")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/infor.properties"})
public class OrderSplitTest extends AbstractUiTest {
    private static final Logger log = LoggerFactory.getLogger(OrderSplitTest.class);
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
    private String building1;
    private String building2;
    private String areaKey1;
    private String areaKey2;
    private String pickingZone1;
    private String pickingZone2;
    private String packingZone1;
    private String packingZone2;
    private String storageCell1;
    private String storageCell2;
    private String consolidationCell1;
    private String packingTable1;
    private String consolidationCell2;
    private String packingTable2;
    private SortingStation sortingStationObj1;
    private String sortingStation1;
    private String sortingCell1;
    private SortingStation sortingStationObj2;
    private String sortingStation2;
    private String sortingCell2;

    private Order originalOrder;
    private WaveId waveId;
    private List<Order> splitOrders = new ArrayList<>();


    @BeforeEach
    @Step("Подготовка: Создаем участки, выдаем разрешения," +
            "проверяем, что нет запущеных отборов и принимаем товар для заказа")
    public void setUp() throws Exception {
        //Здания
        building1 = DatacreatorSteps.Location().createBuilding();
        building2 = DatacreatorSteps.Location().createBuilding();

        // участок 1
        areaKey1 = DatacreatorSteps.Location().createArea();
        // участок 2
        areaKey2 = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки S
        pickingZone1 = DatacreatorSteps.Location().createPutawayZone(areaKey1, building1);
        packingZone1 = DatacreatorSteps.Location().createPutawayZone(areaKey1, building1);

        // зоны отбора и упаковки M
        pickingZone2 = DatacreatorSteps.Location().createPutawayZone(areaKey2, building2);
        packingZone2 = DatacreatorSteps.Location().createPutawayZone(areaKey2, building2);

        // ячейки отбора, консолидации и стол упаковки S
        storageCell1 = DatacreatorSteps.Location().createPickingCell(pickingZone1);
        consolidationCell1 = DatacreatorSteps.Location().createConsolidationCell(pickingZone1);
        packingTable1 = DatacreatorSteps.Location().createPackingTable(packingZone1);

        // ячейки отбора, консолидации и стол упаковки M
        storageCell2 = DatacreatorSteps.Location().createPickingCell(pickingZone2);
        consolidationCell2 = DatacreatorSteps.Location().createConsolidationCell(pickingZone2);
        packingTable2 = DatacreatorSteps.Location().createPackingTable(packingZone2);

        // сортировочная станция S
        sortingStationObj1 = DatacreatorSteps.Location().createSortingStation(pickingZone1);
        sortingStation1 = sortingStationObj1.getStation();
        sortingCell1 = sortingStationObj1.getSortLocations().get(0);

        // сортировочная станция M
        sortingStationObj2 = DatacreatorSteps.Location().createSortingStation(pickingZone2);
        sortingStation2 = sortingStationObj2.getStation();
        sortingCell2 = sortingStationObj2.getSortLocations().get(0);

        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM1, storageCell1);
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM2, storageCell2);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление сортировочной станции
        DatacreatorSteps.Location().deleteSotingStation(sortingStation1);
        DatacreatorSteps.Location().deleteSotingStation(sortingStation2);

        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable1);
        DatacreatorSteps.Location().deleteCell(packingTable2);
        DatacreatorSteps.Location().deleteCell(consolidationCell1);
        DatacreatorSteps.Location().deleteCell(consolidationCell2);
        DatacreatorSteps.Location().deleteCell(storageCell2);
        DatacreatorSteps.Location().deleteCell(storageCell1);

        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(packingZone1);
        DatacreatorSteps.Location().deletePutawayZone(pickingZone1);
        DatacreatorSteps.Location().deletePutawayZone(packingZone2);
        DatacreatorSteps.Location().deletePutawayZone(pickingZone2);

        // удаление здания у заказа
        DatacreatorSteps.Location().deleteBuildingInOrder(originalOrder.getFulfillmentId());

        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey1);
        DatacreatorSteps.Location().deleteArea(areaKey2);

        //Удаление зданий
        DatacreatorSteps.Location().deleteBuilding(building1);
        DatacreatorSteps.Location().deleteBuilding(building2);

        if (waveId != null) {
            uiSteps.Login().PerformLogin();
            uiSteps.Wave().unreserveWave(waveId);
        }
    }

    @RetryableTest
    @ResourceLock("Тест разделения заказа")
    @DisplayName("Тест разделения заказа")
    public void splitOrderTest(){
        originalOrder = ApiSteps.Order().createTodayOrder(ITEMS);
        splitOrders = processSteps.Outgoing().splitCreatedOrder(originalOrder.getYandexId());
        processSteps.Outgoing().checkBuildingForOrder(splitOrders.get(0).getFulfillmentId());
        processSteps.Outgoing().checkBuildingForOrder(splitOrders.get(1).getFulfillmentId());
        processSteps.Outgoing().checkOrdersStatus(splitOrders.get(0).getFulfillmentId());
        processSteps.Outgoing().checkOrdersStatus(splitOrders.get(1).getFulfillmentId());
    }
}
