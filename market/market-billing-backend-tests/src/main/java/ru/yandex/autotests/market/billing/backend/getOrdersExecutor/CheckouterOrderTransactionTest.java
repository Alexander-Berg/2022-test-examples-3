package ru.yandex.autotests.market.billing.backend.getOrdersExecutor;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.cpa.CpaOrderTransaction;
import ru.yandex.autotests.market.billing.backend.data.wiki.ShopsForParallelTestingProvider;
import ru.yandex.autotests.market.billing.backend.steps.BillingDaoSteps;
import ru.yandex.autotests.market.billing.backend.steps.GetOrdersExecutorSteps;
import ru.yandex.autotests.market.checkouter.api.data.providers.payment.PaymentTestsDataProvider;
import ru.yandex.autotests.market.checkouter.api.steps.CheckoutSteps;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;
import ru.yandex.autotests.market.checkouter.client.body.request.checkout.CheckoutRequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckoutResponseBody;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.hazelcast.LockRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.market.checkouter.api.data.requests.checkout.CheckoutRequests.orderRequestForShopWithPrepaidYandex;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 01/21/15
 */
@Aqua.Test(title = "Транзакции по предоплатным заказам(платежи)")
@Feature("Cpa")
@Description("Подробное описание теста https://st.yandex-team.ru/AUTOTESTMARKET-102")
@RunWith(Parameterized.class)
public class CheckouterOrderTransactionTest {
    private static final Long shopId = ShopsForParallelTestingProvider.getInstance().takeCpaShop();
    @ClassRule
    public static final LockRule lockRule = new LockRule(shopId.toString());
    private static final long uid = PaymentTestsDataProvider.readWriteUserId1();
    private static final GetOrdersExecutorSteps getOrdersExecutorSteps = GetOrdersExecutorSteps.getInstance();
    private static final BillingDaoSteps billingDaoSteps = BillingDaoSteps.getInstance();
    private static final CheckoutSteps checkoutSteps = new CheckoutSteps();

    @Parameterized.Parameter
    public static CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> requestData;
    @Rule
    public ReportAvailabilityRule reportAvailabilityRule = new ReportAvailabilityRule();

    private TestDataOrder order;

    @Parameterized.Parameters(name = "Проверка последней транзакции по заказу {0}")
    public static Collection<Object[]> testData() {
        return new ArrayList<Object[]>() {{
            add(new Object[]{orderRequestForShopWithPrepaidYandex(shopId.intValue()).withUid(uid)});
        }};
    }

    @Before
    public void setUp() {
        order = checkoutSteps.createOrder(requestData);
    }

    @Test
    public void testPaymentTransaction() throws IOException {
        checkLastOrderTransaction(order, CpaOrderTransaction.Status.NEW);
    }

    private void checkLastOrderTransaction(TestDataOrder order, CpaOrderTransaction.Status expectedStatus) throws IOException {
        getOrdersExecutorSteps.runTmsJobGetOrdersExecutor();
        getOrdersExecutorSteps.skipTmsJobGetOrdersExecutor(1);

        List<CpaOrderTransaction> transactionList = billingDaoSteps.getCpaOrderTransactionsDesc(order.getId());

        assertThat(transactionList,
                describedAs("В таблице market_billing.cpa_order_transaction есть транзакции для заказа %0",
                        is(not(empty())), order.getId()));

        billingDaoSteps.checkCpaOrderTransactionStatus(transactionList.get(0), expectedStatus);
        getOrdersExecutorSteps.checkCpaOrderTransaction(transactionList.get(0), order);
    }
}
