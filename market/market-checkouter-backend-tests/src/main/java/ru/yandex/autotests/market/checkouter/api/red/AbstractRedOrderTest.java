package ru.yandex.autotests.market.checkouter.api.red;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.market.checkouter.api.condition.SupplierCondition;
import ru.yandex.autotests.market.checkouter.api.data.providers.wiki.SupplierTag;
import ru.yandex.autotests.market.checkouter.api.data.requests.checkout.CheckoutRequestContext;
import ru.yandex.autotests.market.checkouter.api.data.requests.checkout.CheckoutRequests;
import ru.yandex.autotests.market.checkouter.api.rule.CheckouterRuleFactory;
import ru.yandex.autotests.market.checkouter.api.rule.ShopIdsProvider;
import ru.yandex.autotests.market.checkouter.api.rule.supplier.SupplierRule;
import ru.yandex.autotests.market.checkouter.api.steps.CheckoutSteps;
import ru.yandex.autotests.market.checkouter.api.steps.OrdersStatusSteps;
import ru.yandex.autotests.market.checkouter.api.steps.OrdersSteps;
import ru.yandex.autotests.market.checkouter.api.steps.PaySteps;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.SubStatus;
import ru.yandex.autotests.market.checkouter.beans.orders.pay.Payment;
import ru.yandex.autotests.market.checkouter.beans.orders.pay.PaymentStatus;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.client.body.request.checkout.CheckoutRequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckBasketResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckBasketResponseBody.BalancePaymentStatus;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckoutResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.OrderResponseBody;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;
import ru.yandex.autotests.market.checkouter.zk.steps.TaskSteps;
import ru.yandex.autotests.market.checkouter.zookeeper.console.CheckouterZooConsole;
import ru.yandex.autotests.market.pushapi.data.wiki.ShopTags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.market.checkouter.api.data.providers.shops.RegionProvider.REGION_MSK;
import static ru.yandex.autotests.market.checkouter.beans.Status.CANCELLED;
import static ru.yandex.autotests.market.checkouter.beans.Status.DELIVERY;

/**
 * @author kukabara
 */
public abstract class AbstractRedOrderTest {
    private static final Integer REGION_ID = REGION_MSK;

    private static final long BALANCE_TIMEOUT = 60000;

    @Rule
    public RuleChain chain = CheckouterRuleFactory.defaultCheckouterRule();
    @Rule
    public SupplierRule supplierRule = new SupplierRule(new SupplierCondition(SupplierTag.RED, REGION_ID, 1));

    private static volatile boolean isInit;
    protected static long shopId;
    protected static long shopId2;
    protected static final Map<Set<Long>, CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody>> REQUEST_CACHE =
            new ConcurrentHashMap<>();

    private CheckouterZooConsole zooConsole;
    protected final TaskSteps taskSteps = new TaskSteps();
    protected final CheckoutSteps checkoutSteps = new CheckoutSteps();
    protected final PaySteps paySteps = new PaySteps();
    protected final OrdersStatusSteps ordersStatusSteps = new OrdersStatusSteps();
    protected final OrdersSteps ordersSteps = new OrdersSteps();

    @Before
    public void prepareZk() {
        zooConsole = new CheckouterZooConsole();
    }

    @After
    public void closeZoo() throws IOException {
        if (zooConsole != null) {
            zooConsole.close();
        }
    }

    @Parameterized.Parameter(0)
    public boolean sandbox;

    @Parameterized.Parameters(name = "Case: sandbox={0}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {true},
                {false}
        });
    }

    /**
     * Заранее загрузим список Красных магазинов и создадим запрос на создание заказа.
     */
    @Before
    public void prepare() {
        zooConsole = new CheckouterZooConsole();
        if (isInit) {
            return;
        }

        synchronized (AbstractRedOrderTest.class) {
            if (isInit) {
                return;
            }
            List<Long> shopIds = ShopIdsProvider.takeShopIdsByTag(ShopTags.CHECKOUTER_RED, ShopTags.DELIVERY_POST);
            assertThat("Can't find RED shops on wiki", shopIds.size(), Matchers.greaterThanOrEqualTo(1));

            shopId = shopIds.get(0);
            shopId2 = shopIds.get(1 % shopIds.size());
            isInit = true;
        }
    }

    TestDataOrder checkoutRedOrder(long shopId) {
        return checkoutRedMultiOrder(shopId).get(0);
    }

    TestDataOrder checkoutRedFFOrder() {
        return checkoutRedFFMultiOrder().get(0);
    }

    private List<TestDataOrder> checkoutRedMultiOrder(Long... shopIds) {
        List<TestDataOrder> orders = new ArrayList<>();
        for (Long shop : shopIds) {
            CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> request =
                    REQUEST_CACHE.computeIfAbsent(ImmutableSet.of(shop), (k) -> constructRequest(shop));

            List<TestDataOrder> o = checkoutSteps.createRedOrder(request.withSandbox(sandbox));
            orders.add(o.get(0));
        }
        return orders;
    }

    private List<TestDataOrder> checkoutRedFFMultiOrder() {
        CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> ffRequest = constructFFRequest();
        return checkoutSteps.createRedOrder(ffRequest);
    }

    protected List<TestDataOrder> checkoutRedMultiOrderConsequently(Long... shopIds) {
        return Arrays.stream(shopIds)
                .map(s -> checkoutRedMultiOrder(shopId).stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Can't checkout order for shopId=" + shopId)))
                .collect(Collectors.toList());
    }

    private CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> constructRequest(Long... shopIds) {
        CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> request = CheckoutRequests.redOrder(
                Arrays.asList(shopIds), REGION_ID, sandbox
        );
        request = checkoutSteps.requestWithActualPrice(
                checkoutSteps.requestWithValidDeliveryIds(request, CheckoutRequestContext.ANY_RED_DELIVERY)
        );
        return request;
    }

    private CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> constructFFRequest() {
        CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> request = CheckoutRequests.redFFOrder(
                REGION_ID, sandbox, supplierRule.getSupplier()
        );
        request = checkoutSteps.requestWithActualPrice(
                checkoutSteps.requestWithValidDeliveryIds(request, CheckoutRequestContext.ANY_RED_DELIVERY)
        );
        return request;
    }

    List<TestDataOrder> processRedMultiOrder(Status... statuses) throws Exception {
        List<TestDataOrder> orders = checkoutRedMultiOrder(shopId, shopId2);
        Payment payment = paySteps.payOrder(sandbox, orders.toArray(new TestDataOrder[0]));

        Map<Long, Status> orderId2Status = ImmutableMap.of(
                orders.get(0).getId(), statuses[0],
                orders.get(1).getId(), statuses[1]
        );

        moveOrderStatuses(orders, orderId2Status);
        checkHoldPayment(orders, payment, orderId2Status);

        startMultiPaymentProcessor();

        checkClearedOrCancelledPayment(payment, orderId2Status);
        return orders;
    }

    private void moveOrderStatuses(List<TestDataOrder> orders, Map<Long, Status> orderId2Status) {
        for (TestDataOrder order : orders) {
            ordersStatusSteps.changeOrderStatus(order, Status.PROCESSING);

            if (orderId2Status.get(order.getId()) == DELIVERY) {
                ordersStatusSteps.changeOrderStatus(order, DELIVERY);
            } else {
                ordersStatusSteps.changeOrderStatusByShop(order, CANCELLED, SubStatus.SHOP_FAILED);
            }
        }
    }

    private void checkHoldPayment(List<TestDataOrder> orders, Payment payment, Map<Long, Status> orderId2Status) {
        for (TestDataOrder order : orders) {
            OrderResponseBody orderById = ordersSteps.getOrderById(order.getId());
            assertThat("No correct order status orderId=" + order.getId(),
                    orderById.getStatus(), equalTo(orderId2Status.get(order.getId())));

            assertThat("Payment status must be HOLD", orderById.getPayment().getStatus(), equalTo(PaymentStatus.HOLD));
        }
        Payment holdPayment = paySteps.getPaymentById(payment.getId());
        assertThat("Payment status must be HOLD", holdPayment.getStatus(), equalTo(PaymentStatus.HOLD));
        if (!sandbox) {
            CheckBasketResponseBody basket = paySteps.checkBasket(holdPayment);
            paySteps.isBasketCorrect(basket, holdPayment);
            assertThat(basket.getPaymentStatus(), equalTo(BalancePaymentStatus.authorized));
        }
    }

    private void startMultiPaymentProcessor() throws Exception {
        taskSteps.startTask("ProcessHeldPaymentsTask", 1);
    }

    private void checkClearedOrCancelledPayment(Payment payment, Map<Long, Status> orderId2Status) {
        PaymentStatus expectedInitStatus = orderId2Status.values().stream().anyMatch(Predicate.isEqual(DELIVERY)) ?
                PaymentStatus.CLEARED : PaymentStatus.CANCELLED;

        Payment resultPayment = paySteps.checkMovePaymentStatus(
                payment.getId(), PaymentStatus.HOLD, expectedInitStatus, BALANCE_TIMEOUT
        );
        assertThat("Payment status must be " + expectedInitStatus, resultPayment.getStatus(), equalTo(expectedInitStatus));

        if (sandbox) {
            return;
        }
        
        boolean fullyCancelled = orderId2Status.values().stream().allMatch(s -> s == CANCELLED);
        BalancePaymentStatus expectedStatus;
        if (fullyCancelled) {
            expectedStatus = BalancePaymentStatus.canceled;
        } else {
            if (orderId2Status.values().stream().anyMatch(s -> s == CANCELLED)) {
                expectedStatus = BalancePaymentStatus.refunded;
            } else {
                expectedStatus = BalancePaymentStatus.cleared;
            }
        }
        CheckBasketResponseBody basket = paySteps.checkBasket(resultPayment.getId(), expectedStatus);
        assertThat(basket.getPaymentStatus(), equalTo(expectedStatus));

        if (fullyCancelled) {
            Assert.assertThat("No correct basket status",
                    basket.getPaymentStatus(), equalTo(expectedStatus));
            assertThat(Arrays.stream(basket.getRefunds())
                    .mapToDouble(CheckBasketResponseBody.CheckBasketRefunds::getAmount)
                    .sum(), equalTo(resultPayment.getTotalAmount()));
            assertThat(basket.getPostAuthAmount(), equalTo(0d));
            assertThat(basket.getCurrentAmount(), equalTo(0d));
        } else {
            paySteps.isBasketCorrect(basket, resultPayment);

            assertThat("Деньги списались: postаuthAmount = totalAmount",
                    basket.getClearedAmount().doubleValue(), closeTo(resultPayment.getTotalAmount(), 0.01));
        }
    }
}
