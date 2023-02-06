package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;

import java.util.Collections;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: New Order")
@Epic("Selenium Tests")
@Slf4j
public class OrderAutostartTest extends AbstractUiTest {
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

    private String consolidationCell;
    private String packingTable;
    private SortingStation sortingStationObj;
    private String sortingStation;
    private String sortingCell;
    private String shippingCell;
    private String shippingDoor;

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
        consolidationCell = DatacreatorSteps.Location().createConsolidationCell(pickingZone);
        packingTable = DatacreatorSteps.Location().createPackingTable(packingZone);

        // сортировочная станция
        sortingStationObj = DatacreatorSteps.Location().createSortingStation(pickingZone);
        sortingStation = sortingStationObj.getStation();
        sortingCell = sortingStationObj.getSortLocations().get(0);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingCell = DatacreatorSteps.Location().createShippingStandardCell(shippingZone);

        //принимаем товары
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell, 1);


    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление сортировочной станции
        DatacreatorSteps.Location().deleteSotingStation(sortingStation);

        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(packingTable);
        DatacreatorSteps.Location().deleteCell(consolidationCell);
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
    @Tag("AutostartMultitestingSuite")
    @DisplayName("Тест заказа: Запуск волны через автостарт (проверяем что волна создалась и запустилась, отбор, консолидацию и тп не смотрим)")
    @ResourceLock("Тест заказа: Запуск волны через автостарт (проверяем что волна создалась и запустилась, отбор, консолидацию и тп не смотрим)")
    public void orderTestWithNewPickingAndNewPackingAutoWave() {
        Order order = ApiSteps.Order().createTodayOrder(ITEM);
        waveId = processSteps.Outgoing().createAndStartAutoWave(order, sortingStation);
        String actualSortingStation = processSteps.Outgoing().getSortingStationByWaveId(waveId);
        log.info("""
                    Wave id: %s
                    expected sorting station: %s
                    actual sorting station: %s
                 """
                .formatted(waveId, sortingStation, actualSortingStation)
        );
    }
}
