package ru.yandex.market.checkout.checkouter.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException.notAllowed;
import static ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper.checkErroneousResponse;
import static ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper.reduceOneItem;

/**
 * MARKETCHECKOUT-5378: Давать менять состав FF-заказа только на статусе PENDING.
 *
 * @author Nikolai Iusiumbeli
 * date: 06/02/2018
 */
//ignored until 'remove items' is refactored and supported in production
@Disabled
public class ChangePendingOrderItemsTest extends AbstractChangeOrderItemsTestBase {

    @Autowired
    private OrderDeliveryHelper deliveryHelper;
    @Autowired
    private WireMockServer stockStorageMock;
    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;


    @DisplayName("проверяем, что фф заказ можно менять только в статусе PENDING, в остальных - нет")
    @Test
    public void testChangeItemsInFFOrderInPendingStatus() throws Exception {
        Order order = createOrder(OrderConfig.defaultConfig()
                .with(orderItemsWithCorrectWareMD5())
                .with(PaymentType.POSTPAID)
                .with(OrderAcceptMethod.WEB_INTERFACE)
                .fulfillment(true)
                .withColor(Color.BLUE)
                .freeDelivery()
        );

        stockStorageConfigurer.mockOkForRefreeze();

        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.getStatus(), is(OrderStatus.PENDING));
        assertThat(orderFromDb.isFulfilment(), is(true));

        List<OrderItem> items = new ArrayList<>(order.getItems());
        changeItemsAndCheckResult(ImmutableMap.of(
                items.get(0).getOfferItemKey(), 2,
                items.get(1).getOfferItemKey(), 2)
        );


        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        assertThat(orderService.getOrder(order.getId()).getStatus(), is(OrderStatus.PROCESSING));

        items.get(0).setCount(1);
        items.get(1).setCount(1);

        OrderStatusNotAllowedException expectedExc = notAllowed(order.getId(), null, null);
        checkErroneousResponse(changeOrderItems(items), expectedExc);
    }

    @DisplayName("проверяем, что не-фф заказ нельзя менять в статусе PENDING")
    @Test
    public void testChangeItemsOrderInProcessingStatus() throws Exception {
        Order order = createOrder(OrderConfig.defaultConfig()
                .with(PaymentType.POSTPAID)
                .with(OrderAcceptMethod.WEB_INTERFACE)
                .fulfillment(false)
                .withColor(Color.GREEN)
        );

        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.getStatus(), is(OrderStatus.PENDING));
        assertThat(orderFromDb.isFulfilment(), is(false));

        List<OrderItem> items = new ArrayList<>(order.getItems());
        Collection<OrderItem> newItems = reduceOneItem(items);

        OrderStatusNotAllowedException expectedExc = notAllowed(order.getId(), null, null);
        checkErroneousResponse(changeOrderItems(newItems), expectedExc);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        assertThat(orderService.getOrder(order.getId()).getStatus(), is(OrderStatus.PROCESSING));

        refreshOrder();

        changeItemsAndCheckResult(ImmutableMap.of(
                items.get(0).getOfferItemKey(), 1,
                items.get(1).getOfferItemKey(), 1)
        );
    }

    @DisplayName("проверяем, что доставку у фф заказ можно менять в статусе PENDING")
    @Test
    public void testChangeDeliveryInFFOrderInPendingStatus() throws Exception {
        Order order = createOrder(
                OrderConfig.defaultConfig()
                        .with(orderItemsWithCorrectWareMD5())
                        .with(PaymentType.POSTPAID)
                        .with(OrderAcceptMethod.WEB_INTERFACE)
                        .fulfillment(true)
                        .withColor(Color.BLUE)
                        .freeDelivery()
        );

        stockStorageMock.stubFor(post(urlPathEqualTo("/stocks/unfreeze"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));

        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.getStatus(), is(OrderStatus.PENDING));
        assertThat(orderFromDb.isFulfilment(), is(true));


        String street = "qwiurpiojpkdsjaopfj";

        Delivery delivery = orderFromDb.getDelivery();
        ((AddressImpl) (delivery.getShopAddress())).setStreet(street);
        deliveryHelper.updateOrderDelivery(order.getId(), delivery);

        assertThat(orderService.getOrder(order.getId()).getDelivery().getShopAddress().getStreet(), is(street));
    }

    @DisplayName("проверяем, что у не-фф заказов нельзя менять доставку в PENDING")
    @Test
    public void testNotChangeDeliveryInPendingStatus() throws Exception {
        Order order = createOrder(OrderConfig.defaultConfig()
                .with(PaymentType.POSTPAID)
                .with(OrderAcceptMethod.WEB_INTERFACE)
                .fulfillment(false)
                .withColor(Color.GREEN)
        );

        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.getStatus(), is(OrderStatus.PENDING));
        assertThat(orderFromDb.isFulfilment(), is(false));

        String street = "qwiurpiojpkdsjaopfj";

        Delivery delivery = orderFromDb.getDelivery();
        ((AddressImpl) (delivery.getShopAddress())).setStreet(street);
        deliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, delivery)
                .andExpect(status().is4xxClientError());

    }
}
