package ru.yandex.market.checkout.checkouter.refund;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.backbone.fintech.AccountPaymentFeatureToggle;
import ru.yandex.market.checkout.checkouter.b2b.NotifyBillPaidRequest;
import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkouter.jooq.Tables;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class AbstractRefundTestBase extends AbstractPaymentTestBase {
    private static final String CLEAN_ORDERS = "TRUNCATE " + Tables.REFUND.getName() + " CASCADE";

    @Autowired
    protected RefundService refundService;
    @Autowired
    protected RefundHelper refundHelper;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private CheckouterFeatureWriter featureWriter;
    protected Order order;
    @BeforeEach
    public void beforeEach() {
        cleanOrders();
        cleanRefunds();
    }

    @AfterEach
    public void afterEach() {
        cleanOrders();
        cleanRefunds();
    }

    protected void cleanRefunds() {
        transactionTemplate.execute(ts -> {
            jdbcTemplate.execute(CLEAN_ORDERS);
            return null;
        });
    }

    protected Refund prepareRefund() throws IOException {
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());
        return prepareRefund(order);
    }

    protected Refund prepareRefund(Order order) throws IOException {
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERY));
        RefundableItems items = refundHelper.getRefundableItemsFor(order);
        trustMockConfigurer.mockCreateRefund(BasketStatus.error);
        trustMockConfigurer.mockCheckBasket(
                CheckBasketParams.buildCheckBasketWithConfirmedRefund("123", order.getBuyerTotal()));
        trustMockConfigurer.mockStatusBasket(
                CheckBasketParams.buildCheckBasketWithConfirmedRefund("123", order.getBuyerTotal()), null);
        Assertions.assertNotNull(order.getPayment());
        refundService.createRefund(
                order.getId(),
                order.getBuyerTotal(),
                "Just Test",
                ClientInfo.SYSTEM,
                RefundReason.ORDER_CANCELLED,
                order.getPayment().getType(),
                false,
                items.toRefundItems(),
                false,
                null,
                false
        );

        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertThat(refunds, hasSize(1));
        var refund = refunds.iterator().next();
        var isAsync = refundHelper.isAsyncRefundStrategyEnabled(refund);
        assertThat(refund.getStatus(), equalTo(isAsync ? RefundStatus.DRAFT : RefundStatus.RETURNED));

        return refund;
    }

    protected Refund prepareB2bRefund() throws IOException {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.ON);

        b2bCustomersMockConfigurer.mockGeneratePaymentInvoice();
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);

        var parameters = B2bCustomersTestProvider.defaultB2bParameters();
        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY));
        order = orderCreateHelper.createOrder(parameters);
        client.payments().generatePaymentInvoice(order.getId());
        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(order.getId())));

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERY));
        RefundableItems items = refundHelper.getRefundableItemsFor(order);
        trustMockConfigurer.mockCreateRefund(BasketStatus.error);
        trustMockConfigurer.mockCheckBasket(
                CheckBasketParams.buildCheckBasketWithConfirmedRefund("123", order.getBuyerTotal()));
        trustMockConfigurer.mockStatusBasket(
                CheckBasketParams.buildCheckBasketWithConfirmedRefund("123", order.getBuyerTotal()), null);
        Assertions.assertNotNull(order.getPayment());
        Assertions.assertEquals(PaymentGoal.ORDER_ACCOUNT_PAYMENT, order.getPayment().getType());

        refundService.createRefund(
                order.getId(),
                order.getBuyerTotal(),
                "Just Test",
                ClientInfo.SYSTEM,
                RefundReason.ORDER_CANCELLED,
                order.getPayment().getType(),
                false,
                items.toRefundItems(),
                false,
                null,
                false
        );

        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertThat(refunds, hasSize(1));
        var refund = refunds.iterator().next();
        var isAsync = refundHelper.isAsyncRefundStrategyEnabled(refund);
        assertThat(refund.getStatus(), equalTo(isAsync ? RefundStatus.DRAFT : RefundStatus.RETURNED));

        return refund;
    }
}
