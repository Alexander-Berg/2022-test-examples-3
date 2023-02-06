package ru.yandex.market.checkout.checkouter.items;

import java.util.ArrayList;
import java.util.List;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.FreezeHelper;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

public class ChangeOrderItemsUnfreezeStocksTest extends AbstractChangeOrderItemsTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ChangeOrderItemsUnfreezeStocksTest.class);

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @Autowired
    private FreezeHelper freezeHelper;

    @Autowired
    private ZooTask itemsUnfreezeTask;

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    public void shouldAddItemsUnfreezeMessage() throws Exception {
        Order order = createOrder(OrderConfig.defaultConfig()
                .with(orderItemsWithCorrectWareMD5())
                .with(PaymentType.POSTPAID)
                .fulfillment(true)
                .with(OrderAcceptMethod.WEB_INTERFACE)
                .withColor(Color.BLUE)
                .with(DeliveryPartnerType.YANDEX_MARKET));

        stockStorageConfigurer.mockOkForRefreeze();

        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.isFulfilment(), is(true));

        stockStorageConfigurer.resetRequests();
        List<OrderItem> items = new ArrayList<>(order.getItems());
        changeItemsAndCheckResult(ImmutableMap.of(
                items.get(0).getOfferItemKey(), 2,
                items.get(1).getOfferItemKey(), 2)
        );

        queuedCallsHelper.runItemsRefreezeQCProcessor(order.getId(), items);

        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(1));
        ServeEvent event = Iterables.getOnlyElement(events);

        Assertions.assertEquals("/order", event.getRequest().getUrl());
        String requestBody = event.getRequest().getBodyAsString();

        logger.debug("request body: {}", requestBody);

        JsonTest.checkJson(requestBody, "$.orderId", order.getId());
        JsonTest.checkJson(requestBody, "$.items", JsonPathExpectationsHelper::assertValueIsArray);
        JsonTest.checkJson(requestBody, "$.items[0].amount", 2);
        JsonTest.checkJson(requestBody, "$.items[1].amount", 2);

        freezeHelper.assertFreezeCount(order.getId(), 2, 2);
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Пропуск refreeze для unfrozen товаров")
    public void shouldNotRefreezeUnfrozenItems() throws Exception {
        Order order = createOrder(OrderConfig.defaultConfig()
                .with(orderItemsWithCorrectWareMD5())
                .with(PaymentType.POSTPAID)
                .fulfillment(true)
                .with(OrderAcceptMethod.WEB_INTERFACE)
                .withColor(Color.BLUE)
                .with(DeliveryPartnerType.YANDEX_MARKET));

        stockStorageConfigurer.mockOkForRefreeze();
        stockStorageConfigurer.mockOkForUnfreeze();

        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.isFulfilment(), is(true));
        assertThat(orderFromDb.getItems(), hasSize(2));

        stockStorageConfigurer.resetRequests();

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING, OrderSubstatus.SHIPPED);
        itemsUnfreezeTask.runOnce();

        List<ServeEvent> events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(1));
        ServeEvent event = Iterables.getOnlyElement(events);

        Assertions.assertEquals("/order/" + order.getId() + "?cancel=false", event.getRequest().getUrl());
        Assertions.assertEquals(RequestMethod.DELETE, event.getRequest().getMethod());

        freezeHelper.assertFreezeCount(order.getId(), 0, 0);

        stockStorageConfigurer.resetRequests();

        List<OrderItem> items = new ArrayList<>(order.getItems());
        changeItemsAndCheckResult(ImmutableMap.of(
                items.get(0).getOfferItemKey(), 2,
                items.get(1).getOfferItemKey(), 2)
        );
        itemsUnfreezeTask.runOnce();

        events = stockStorageConfigurer.getServeEvents();
        assertThat(events, hasSize(0));

        freezeHelper.assertFreezeCount(order.getId(), 0, 0);
    }
}
