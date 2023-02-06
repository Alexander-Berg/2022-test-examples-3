package ru.yandex.autotests.market.checkouter.api.orders.returns;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import ru.yandex.autotests.market.checkouter.api.condition.SupplierCondition;
import ru.yandex.autotests.market.checkouter.api.data.providers.shops.RegionProvider;
import ru.yandex.autotests.market.checkouter.api.data.providers.wiki.SupplierTag;
import ru.yandex.autotests.market.checkouter.api.data.requests.checkout.CheckoutRequestContext;
import ru.yandex.autotests.market.checkouter.api.rule.CheckouterRuleFactory;
import ru.yandex.autotests.market.checkouter.api.rule.supplier.SupplierRule;
import ru.yandex.autotests.market.checkouter.api.steps.CheckoutSteps;
import ru.yandex.autotests.market.checkouter.api.steps.OrdersStatusSteps;
import ru.yandex.autotests.market.checkouter.api.steps.PaySteps;
import ru.yandex.autotests.market.checkouter.api.steps.RefundSteps;
import ru.yandex.autotests.market.checkouter.api.steps.ReturnSteps;
import ru.yandex.autotests.market.checkouter.api.utils.TimeoutUtils;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.orders.pay.PaymentStatus;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.client.body.request.checkout.CheckoutRequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckoutResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.checkout.CheckoutResponseMultiOrder;
import ru.yandex.autotests.market.checkouter.client.body.response.order.OrderRefundsResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.payment.OrderReturnResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.payment.PaymentResponseBody;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;
import ru.yandex.autotests.market.checkouter.zookeeper.console.CheckouterZooConsole;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.market.checkouter.beans.orders.refund.Refund.Status.SUCCESS;
import static ru.yandex.autotests.market.checkouter.client.body.utils.BodyMapper.map;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;

public class AbstractReturnTest {

    protected static SupplierRule supplierRule = new SupplierRule(new SupplierCondition(
            SupplierTag.THIRD_PARTY, RegionProvider.REGION_MSK, 1)
    );
    @ClassRule
    public static RuleChain chain = CheckouterRuleFactory.defaultCheckouterRule(supplierRule);

    protected CheckouterZooConsole zooConsole;
    protected TestDataOrder order;
    protected OrderReturnResponseBody ret;

    protected RefundSteps refundSteps = new RefundSteps();
    protected ReturnSteps returnSteps = new ReturnSteps();

    private CheckoutSteps checkout = new CheckoutSteps();
    private PaySteps paySteps = new PaySteps();
    private OrdersStatusSteps statusSteps = new OrdersStatusSteps();


    @Before
    public void initZooConnection() {
        zooConsole = new CheckouterZooConsole();
    }

    protected void createOrderAndReturn(CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> request) {
        order = createAndProcessOrderToDelivered(request);
        ret = returnSteps.createFullOrderReturn(order);
        zooConsole.startReturnProcessor();
        TimeoutUtils.waitFor(30, TimeUnit.SECONDS);
        checkCompensations();
        checkRefund();
    }

    protected TestDataOrder createAndProcessOrderToDelivered(CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> request) {
        request = checkout.requestWithValidDeliveryIds(request, CheckoutRequestContext.ANY_BLUE_DELIVERY);
        CheckoutResponseMultiOrder responseOrders = checkout.createMultiOrder(request);
        assertStep("Заказ создался", responseOrders.getCheckedOut(), equalTo(true));
        TestDataOrder order = map(responseOrders.getOrders().get(0), TestDataOrder.class);
        paySteps.payAndDelivery(order, false);
        return statusSteps.changeOrderStatus(order, Status.DELIVERED);
    }

    private void checkCompensations() {
        List<PaymentResponseBody> compensations = returnSteps.getCompensations(order.getId(), ret.getId());
        assertStep("Создали 2 платежа", compensations, hasSize(2));
        assertStep("Платежи в cleared", compensations, everyItem(hasProperty("status", equalTo(PaymentStatus.CLEARED))));
    }

    private void checkRefund() {
        OrderRefundsResponseBody refund = refundSteps.getRefundsByOrderId(order.getId());
        assertStep("Создан 1 рефанд", refund.getRefunds(), hasSize(1));
        assertStep("Рефанд завершен", refund.getRefunds().get(0).getStatus(), equalTo(SUCCESS));
    }
}
