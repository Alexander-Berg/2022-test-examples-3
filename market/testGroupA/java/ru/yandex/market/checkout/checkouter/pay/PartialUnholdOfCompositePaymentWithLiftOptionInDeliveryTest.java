package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemChangeRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.UpdateBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponse;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCheckBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkUpdateBasketCalls;
import static ru.yandex.market.checkout.util.balance.checkers.UpdateBasketParams.updateBasket;

/**
 * MARKETCHECKOUT-25033
 */
public class PartialUnholdOfCompositePaymentWithLiftOptionInDeliveryTest extends AbstractPaymentTestBase {

    public static final BigDecimal CASHBACK_SPEND_ITEM = BigDecimal.valueOf(50);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderUpdateService orderUpdateService;

    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    void testPartialUnholdInCaseOfCompositePaymentWithLiftOptionInDelivery() throws Exception {
        // Создать заказ с кэшбэком, с опцией Lift в доставке, и двумя айтемами.
        Order order = createCashbackOrderWithLiftDeliveryOption();

        // Оплата данного заказа.
        Payment payment = paymentHelper.payForOrder(order);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(payment.getStatus(), PaymentStatus.HOLD);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        trustMockConfigurer.mockCheckBasket(createMockBasketWithDiliveryLiftPriceIncluded(order));

        // Удаление одного айтема из заказа. Должен запуститься частичный анхолд платежа.
        Iterator<OrderItem> orderItemIterator = order.getItems().iterator();
        OrderItem orderItemToRemove = orderItemIterator.next();
        OrderItem orderItemToLeave = orderItemIterator.next();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(orderItemToLeave.getId(),
                orderItemToLeave.getCount(), orderItemToLeave.getQuantity(), orderItemToLeave.getFeedId(),
                orderItemToLeave.getOfferId());
        orderUpdateService.updateOrderItems(order.getId(), List.of(orderItemChangeRequest), ClientInfo.SYSTEM);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.PAYMENT_PARTIAL_UNHOLD, payment.getId());

        // Проверяем запросы в траст.
        OneElementBackIterator<ServeEvent> callIterator = trustMockConfigurer.eventsIterator();
        checkCheckBasketCall(callIterator, payment.getBasketKey().getPurchaseToken());

        // В трасте должен быть только один запрос отмены айтема корзины. Апдейтов быть не должно.
        UpdateBasketParams updateBasketParams = updateBasket(payment.getBasketKey())
                .withUid(payment.getUid())
                .withUserIp("127.0.0.1")
                .withLineToRemove(orderItemToRemove.getBalanceOrderId());
        checkUpdateBasketCalls(callIterator, updateBasketParams);

        assertFalse(callIterator.hasNext());
    }

    private CheckBasketParams createMockBasketWithDiliveryLiftPriceIncluded(Order order) {
        Collection<CheckBasketParams.BasketLineState> basketLines = new ArrayList<>();
        Collection<CheckBasketParams.BasketRefund> basketRefunds = new ArrayList<>();
        order.getItems().forEach(i -> basketLines.add(
                        new CheckBasketParams.BasketLineState(
                                i.getBalanceOrderId(), i.getCount(), i.getBuyerPrice())
                )
        );
        basketLines.add(
                new CheckBasketParams.BasketLineState(
                        order.getDelivery().getBalanceOrderId(), 1, order.getDelivery().getPriceWithLift())
        );

        CheckBasketParams config = new CheckBasketParams();
        config.setLines(basketLines);
        config.setRefunds(basketRefunds);
        return config;
    }

    @NotNull
    private Order createCashbackOrderWithLiftDeliveryOption() throws Exception {
        Parameters parameters = WhiteParametersProvider.shopDeliveryOrder(OrderProvider.orderBuilder()
                .item(OrderItemProvider.getOrderItem())
                .item(OrderItemProvider.getOrderItem())
                .build()
        );
        enableLiftOptions(parameters);

        parameters.getOrder().getDelivery().setLiftType(LiftType.CARGO_ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);

        parameters.setCheckCartErrors(false);
        parameters.setupPromo("PROMO");
        parameters.getLoyaltyParameters()
                .setExpectedCashbackOptionsResponse(singleItemCashbackResponse(CASHBACK_SPEND_ITEM));
        parameters.setMockLoyalty(true);
        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.setSelectedCashbackOption(CashbackOption.SPEND);
        MultiOrder multiOrder = orderCreateHelper.checkout(cart, parameters);

        return orderService.getOrder(multiOrder.getOrders().get(0).getId());
    }

    private void enableLiftOptions(Parameters parameters) {
        checkouterProperties.setEnableLiftOptions(true);
        parameters.setExperiments(MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE);
    }
}
