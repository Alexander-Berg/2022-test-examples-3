package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.scheduler;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: Calculate Orders Status")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/infor.properties"})
public class CalculateOrdersStatusTest extends AbstractUiTest {

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
    private String pickingArea;
    private String palletArea;
    private String pickingZone;
    private String palletZone;
    private String pickingCell;
    private String palletStorageCell;

    @BeforeEach
    @Step("Подготовка: Создаем участки и принимаем товар для заказа")
    public void setUp() throws Exception {
        // участок с ячейками отбора
        pickingArea = DatacreatorSteps.Location().createArea();
        // участок с ячейками паллетного хранения
        palletArea = DatacreatorSteps.Location().createArea();

        // зоны отбора и паллетного хранения
        pickingZone = DatacreatorSteps.Location().createPutawayZone(pickingArea);
        palletZone = DatacreatorSteps.Location().createPutawayZone(palletArea);

        // ячейка отбора
        pickingCell = DatacreatorSteps.Location().createPickingCell(pickingZone);

        // ячейка паллетного хранения
        palletStorageCell = DatacreatorSteps.Location().createPalletStorageCell(palletZone);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(pickingCell);
        DatacreatorSteps.Location().deleteCell(palletStorageCell);

        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(pickingZone);
        DatacreatorSteps.Location().deletePutawayZone(palletZone);

        // удаление участка
        DatacreatorSteps.Location().deleteArea(pickingArea);
        DatacreatorSteps.Location().deleteArea(palletArea);
    }

    @RetryableTest
    @DisplayName("Тест перехода заказа в статус 02")
    @ResourceLock("Тест перехода заказа в статус 02")
    public void orderShouldGetCreatedStatusTest() {
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM1, pickingCell);
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM2, pickingCell);

        Order orderToEnoughBalances = ApiSteps.Order().createTodayOrder(List.of(ITEM1, ITEM2));

        ApiSteps.Order().startCalculateOrdersStatusJob();
        processSteps.Outgoing().waitOrderStatusIs(orderToEnoughBalances.getFulfillmentId(), OrderStatus.CREATED);
    }

    @RetryableTest
    @DisplayName("Тест перехода заказа в статус -3")
    @ResourceLock("Тест перехода заказа в статус -3")
    public void orderShouldGetOutOfPickingStatusTest() {
        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM1, pickingCell);
        processSteps.Incoming().acceptItemsAndPlaceThemToPalletCell(ITEM2, palletStorageCell);

        Order orderToReplenishment = ApiSteps.Order().createTodayOrder(List.of(ITEM1, ITEM2));

        ApiSteps.Order().startCalculateOrdersStatusJob();
        processSteps.Outgoing().waitOrderStatusIs(orderToReplenishment.getFulfillmentId(), OrderStatus.OUT_OF_PICKING_LOT);
    }

}
