package ru.yandex.market.checkout.checkouter.pay;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.common.report.model.json.common.Region;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.ITEMS_NOT_FOUND;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class TinkoffCreditPartiallyCancelTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;

    @BeforeEach
    public void setUp() {
        checkouterProperties.setItemsRemoveAllow(true);
    }

    @Test
    public void cancelOneOrderOutOfTwo() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOrder(defaultBlueOrderParameters());
        parameters.setShowCredits(true);
        parameters.configureMultiCart(multiCart -> multiCart.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT));
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order order = multiOrder.getOrders().get(0);

        var payment = orderPayHelper.payForOrdersWithoutNotification(multiOrder.getOrders());
        orderPayHelper.notifyWaitingBankDecision(payment);

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), equalTo(UNPAID));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        assertFalse(trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .anyMatch(url -> url.contains(payment.getBasketKey().getPurchaseToken() + "/unhold"))
        );

        var cancellationRequest = new CompatibleCancellationRequest(
                OrderSubstatus.USER_CHANGED_MIND.name(),
                "notes"
        );
        var canceledOrder = client.createCancellationRequest(
                order.getId(),
                cancellationRequest,
                ClientRole.USER,
                BuyerProvider.UID,
                singletonList(order.getRgb()));

        assertThat(canceledOrder.getStatus(), equalTo(CANCELLED));
        assertThat(canceledOrder.getSubstatus(), equalTo(OrderSubstatus.USER_CHANGED_MIND));
        assertThat(canceledOrder.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        validateQcCallAndSendCancelPaymentNotification(canceledOrder, payment);

        var creditUnholdCount = trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .filter(url -> url.contains(
                        "/credit/" + payment.getBasketKey().getPurchaseToken() + "/unhold"))
                .count();

        assertThat("Must be only 1 unhold call in trust", creditUnholdCount, equalTo(1L));

        SoftAssertions.assertSoftly(softly -> multiOrder.getOrders()
                .stream()
                .map(Order::getId)
                .map(orderService::getOrder)
                .map(dbOrder -> {
                    softly.assertThat(dbOrder.getStatus()).isEqualTo(CANCELLED);
                    softly.assertThat(dbOrder.getSubstatus())
                            .isIn(OrderSubstatus.USER_CHANGED_MIND, OrderSubstatus.USER_NOT_PAID);
                    return dbOrder.getPayment();
                })
                .filter(Objects::nonNull)
                .forEach(dbPayment -> softly.assertThat(dbPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED)));
    }

    @Test
    public void missingOneItemTest() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOtherItem(10);
        parameters.getItems().forEach(item -> item.setManufacturerCountries(List.of(new Region())));

        parameters.addOrder(defaultBlueOrderParameters());

        parameters.setShowCredits(true);
        parameters.configureMultiCart(multiCart -> multiCart.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT));
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order order = multiOrder.getOrders().stream()
                .filter(it -> it.getItems().size() > 1)
                .findFirst()
                .orElseGet(() -> fail("Order with itmes.size > 1 not found"));

        var payment = orderPayHelper.payForOrdersWithoutNotification(multiOrder.getOrders());
        orderPayHelper.notifyWaitingBankDecision(payment);

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), equalTo(UNPAID));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        assertFalse(trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .anyMatch(url -> url.contains(payment.getBasketKey().getPurchaseToken() + "/unhold"))
        );

        var editRequest = new OrderEditRequest();
        var itemInfoList = order.getItems().stream()
                .filter(item -> item.getCount() > 1)
                .map(item -> new ItemInfo(item.getSupplierId(), item.getShopSku(), item.getCount() - 1))
                .collect(Collectors.toList());

        editRequest.setMissingItemsNotification(
                new MissingItemsNotification(true, itemInfoList, ITEMS_NOT_FOUND));

        // TINKOFF_CREDIT doesn't support item removal
        // MissingItemsStrategy#cancelOrderStrategy will be selected and order will be cancelled
        client.editOrder(
                order.getId(),
                ClientRole.SYSTEM,
                BuyerProvider.UID,
                singletonList(order.getRgb()),
                editRequest);

        validateQcCallAndSendCancelPaymentNotification(order, payment);

        var creditUnholdCount = trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .filter(url -> url.contains(
                        "/credit/" + payment.getBasketKey().getPurchaseToken() + "/unhold"))
                .count();

        assertThat("Must be only 1 unhold call in trust", creditUnholdCount, equalTo(1L));

        SoftAssertions.assertSoftly(softly -> multiOrder.getOrders()
                .stream()
                .map(Order::getId)
                .map(orderService::getOrder)
                .map(dbOrder -> {
                    softly.assertThat(dbOrder.getStatus()).isEqualTo(CANCELLED);
                    softly.assertThat(dbOrder.getSubstatus())
                            .isIn(OrderSubstatus.MISSING_ITEM, OrderSubstatus.USER_NOT_PAID);
                    return dbOrder.getPayment();
                })
                .filter(Objects::nonNull)
                .forEach(dbPayment -> softly.assertThat(dbPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED)));
    }

    private void validateQcCallAndSendCancelPaymentNotification(Order order, Payment payment) {
        assertThat(queuedCallService.findQueuedCalls(CheckouterQCType.ORDER_REFUND, order.getId()), hasSize(1));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        assertThat(queuedCallService.findQueuedCalls(CheckouterQCType.ORDER_REFUND, order.getId()), hasSize(1));
        assertThat(
                paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus(),
                equalTo(PaymentStatus.WAITING_BANK_DECISION)
        );
        setFixedTime(getClock().instant().plus(1, ChronoUnit.DAYS));
        orderPayHelper.notifyPaymentCancel(payment);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        assertThat(queuedCallService.findQueuedCalls(CheckouterQCType.ORDER_REFUND, order.getId()), hasSize(0));
    }

    @Test
    public void shouldCancelTinkoffCreditOrderFromUnpaidWaitingBankDecision() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOtherItem(10);
        parameters.getItems().forEach(item -> item.setManufacturerCountries(List.of(new Region())));

        parameters.addOrder(defaultBlueOrderParameters());

        parameters.setShowCredits(true);
        parameters.configureMultiCart(multiCart -> multiCart.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT));
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        var payment = orderPayHelper.payForOrdersWithoutNotification(multiOrder.getOrders());
        orderPayHelper.notifyWaitingBankDecision(payment);
        orderPayHelper.notifyPaymentCancel(payment);

        SoftAssertions.assertSoftly(softly -> multiOrder.getOrders()
                .stream()
                .map(Order::getId)
                .map(orderService::getOrder)
                .map(dbOrder -> {
                    softly.assertThat(dbOrder.getStatus()).isEqualTo(CANCELLED);
                    softly.assertThat(dbOrder.getSubstatus()).isEqualTo(OrderSubstatus.USER_NOT_PAID);
                    return dbOrder.getPayment();
                })
                .filter(Objects::nonNull)
                .forEach(dbPayment -> softly.assertThat(dbPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED)));

        var creditUnholdCount = trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .filter(url -> url.contains(payment.getBasketKey().getPurchaseToken() + "/unhold"))
                .count();

        assertThat(
                "Must be 0 unhold call in trust, because the payment has already been canceled",
                creditUnholdCount, equalTo(0L));
    }
}
