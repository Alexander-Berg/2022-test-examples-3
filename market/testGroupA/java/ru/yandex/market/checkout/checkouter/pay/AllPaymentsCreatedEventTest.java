package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.List;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.createCustomNewPrepayMeta;

/**
 * @author : poluektov
 * date: 2021-09-24.
 */
public class AllPaymentsCreatedEventTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderHistoryEventsTestHelper historyEventsTestHelper;
    @Autowired
    private QueuedCallService queuedCallService;

    private Order order;

    @Test
    public void testAllPaymentsCreatedEventForSubsidy() {
        createSubsidyOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());

        var events = historyEventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.ALL_PAYMENTS_CREATED);
        assertThat(events, IsCollectionWithSize.hasSize(1));
    }

    @Test
    public void testAllPaymentsCreatedEventForPrepay() {
        createPrepayOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());

        var events = historyEventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.ALL_PAYMENTS_CREATED);
        assertThat(events, IsCollectionWithSize.hasSize(1));
    }

    @Test
    public void testAllPaymentsCreatedEventForPostpay() {
        trustMockConfigurer.mockWholeTrust();
        createPostpaidOrderWithSubsidy();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT, order.getId());

        var events = historyEventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.ALL_PAYMENTS_CREATED);
        assertThat(events, IsCollectionWithSize.hasSize(0));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());

        events = historyEventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.ALL_PAYMENTS_CREATED);
        assertThat(events, IsCollectionWithSize.hasSize(1));
    }

    private void createPrepayOrder() {
        Parameters parameters = prepaidBlueOrderParameters();
        order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
    }

    private void createSubsidyOrder() {
        Parameters parameters = prepaidBlueOrderParameters();
        OrderItem item1 = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("111.00"), 1);
        item1.setMsku(332L);
        item1.setShopSku("sku-1");
        item1.setSku("332");
        item1.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        item1.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);

        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters()
                .addLoyaltyDiscount(item1, LoyaltyDiscount.discountFor(10, PromoType.MARKET_PROMOCODE));

        parameters.addShopMetaData(item1.getSupplierId(), createCustomNewPrepayMeta(item1.getSupplierId().intValue()));
        parameters.getOrder().setItems(List.of(item1));
        order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
    }

    private void createPostpaidOrderWithSubsidy() {
        Parameters parameters = postpaidBlueOrderParameters();
        OrderItem item1 = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("111.00"), 1);
        item1.setMsku(332L);
        item1.setShopSku("sku-1");
        item1.setSku("332");
        item1.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        item1.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);

        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters()
                .addLoyaltyDiscount(item1, LoyaltyDiscount.discountFor(10, PromoType.MARKET_PROMOCODE));

        parameters.addShopMetaData(item1.getSupplierId(), createCustomNewPrepayMeta(item1.getSupplierId().intValue()));

        parameters.getOrder().setItems(List.of(item1));
        order = orderCreateHelper.createOrder(parameters);
    }
}
