package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.scheduler;

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

import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: Remove Item Cancel Order")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/infor.properties"})
public class RemoveItemCancelOrderTest extends AbstractUiTest {

    private static final Logger log = LoggerFactory.getLogger(RemoveItemCancelOrderTest.class);
    private final String ITEM1_ARTICLE = UniqueId.getString();
    private final String ITEM2_ARTICLE = UniqueId.getString();
    private final String ITEM3_ARTICLE = UniqueId.getString();
    private final Item ITEM1 = Item.builder()
            .sku(ITEM1_ARTICLE)
            .vendorId(1559)
            .article(ITEM1_ARTICLE)
            .quantity(1)
            .removableIfAbsent(true)
            .build();
    private final Item ITEM2 = Item.builder()
            .sku(ITEM2_ARTICLE)
            .vendorId(1559)
            .article(ITEM2_ARTICLE)
            .quantity(1)
            .build();
    private final Item ITEM3 = Item.builder()
            .sku(ITEM3_ARTICLE)
            .vendorId(1559)
            .article(ITEM3_ARTICLE)
            .quantity(1)
            .build();
    private final List<Item> ITEMS1 = List.of(ITEM1, ITEM2);
    private final List<Item> ITEMS2 = List.of(ITEM1, ITEM3);
    private String areaKey1;
    private String pickingZone1;
    private String packingZone1;
    private String storageCell1;


    private Order orderWithItemToRemove;
    private Order orderToCancel;


    @BeforeEach
    @Step("Подготовка: Создаем участки, выдаем разрешения," +
            "проверяем, что нет запущеных отборов и принимаем товар для заказа")
    public void setUp() throws Exception {
        // участок 1
        areaKey1 = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки S
        pickingZone1 = DatacreatorSteps.Location().createPutawayZone(areaKey1);
        packingZone1 = DatacreatorSteps.Location().createPutawayZone(areaKey1);

        // ячейки отбора, консолидации и стол упаковки S
        storageCell1 = DatacreatorSteps.Location().createPickingCell(pickingZone1);

        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM2, storageCell1);
    }

    @AfterEach
    @Step("Чистим за собой данные")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(storageCell1);

        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(packingZone1);
        DatacreatorSteps.Location().deletePutawayZone(pickingZone1);

        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey1);
    }

    @RetryableTest
    @ResourceLock("Тест изменения статуса заказов, по которым не найдено товара")
    @DisplayName("Тест изменения статуса заказов, по которым не найдено товара")
    void removeItemCancelOrderTest() {
        orderWithItemToRemove = ApiSteps.Order().createOrder(UniqueId.get(), ITEMS1, DateUtil.currentDateTimePlus(-878400));
        orderToCancel = ApiSteps.Order().createOrder(UniqueId.get(), ITEMS2, DateUtil.currentDateTimePlus(-948000));
        ApiSteps.Order().startRemoveItemCancelOrderJob();

        String orderWithItemToRemoveKey = orderWithItemToRemove.getFulfillmentId();
        String orderToCancelKey = orderToCancel.getFulfillmentId();
        processSteps.Outgoing().checkOrderWithRemovedItemStatusHistory(orderWithItemToRemoveKey);
        processSteps.Outgoing().waitOrderStatusIs(orderToCancelKey, OrderStatus.CANCELLED_INTERNALLY);
        processSteps.Outgoing().checkCanceledOrderStatusHistory(orderToCancelKey);

        DatacreatorSteps.Items().checkTotalOpenQty(orderWithItemToRemoveKey, ITEMS1.size() - 1);
        DatacreatorSteps.Items().checkTotalOpenQty(orderToCancelKey, 0);
    }
}
