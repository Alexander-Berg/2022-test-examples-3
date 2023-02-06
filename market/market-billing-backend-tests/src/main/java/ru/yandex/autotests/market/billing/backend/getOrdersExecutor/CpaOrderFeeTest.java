package ru.yandex.autotests.market.billing.backend.getOrdersExecutor;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.billing.configuration.MarketBillingDataSource;
import ru.yandex.autotests.market.billing.backend.core.dao.billing.repository.CpaOrderRepository;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.cpa.CpaOrder;
import ru.yandex.autotests.market.billing.backend.data.wiki.ShopsProvider;
import ru.yandex.autotests.market.billing.backend.steps.CpaOrderFeeSteps;
import ru.yandex.autotests.market.billing.backend.steps.GetOrdersExecutorSteps;
import ru.yandex.autotests.market.billing.util.ReactiveJUnitErrorRule;
import ru.yandex.autotests.market.checkouter.api.data.configuration.CheckouterApiDataSource;
import ru.yandex.autotests.market.checkouter.api.data.providers.payment.PaymentTestsDataProvider;
import ru.yandex.autotests.market.checkouter.api.data.providers.report.ReportItemsProvider;
import ru.yandex.autotests.market.checkouter.api.data.providers.shops.CurrencyProvider;
import ru.yandex.autotests.market.checkouter.api.data.requests.checkout.CheckoutRequests;
import ru.yandex.autotests.market.checkouter.api.steps.CheckoutSteps;
import ru.yandex.autotests.market.checkouter.api.steps.PaySteps;
import ru.yandex.autotests.market.checkouter.api.utils.TimeoutUtils;
import ru.yandex.autotests.market.checkouter.beans.Buyer;
import ru.yandex.autotests.market.checkouter.beans.Currency;
import ru.yandex.autotests.market.checkouter.beans.OfferCount;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataItem;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataMultiOrder;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;
import ru.yandex.autotests.market.checkouter.client.body.request.checkout.CheckoutRequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckoutResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.utils.BodyMapper;
import ru.yandex.autotests.market.checkouter.zookeeper.console.CheckouterZooConsole;
import ru.yandex.autotests.market.common.spring.CommonSpringTest;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.autotests.market.report.util.offers.OffersParser;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * @author Dmitriy Polyanov <a href="mailto:neiwick@yandex-team.ru"></a>
 * @since 18.11.16
 * <p>
 * Тест умеет запускаться параллельно по параметрам
 */
@Aqua.Test(title = "Проверка скидки на предоплатные заказы")
@Feature("Cpa")
@Issue("AUTOTESTMARKET-3917")
@CommonSpringTest(classes = {
        CpaOrderFeeSteps.class,
        CheckoutSteps.class,
        CheckouterZooConsole.class,
        PaySteps.class,
        GetOrdersExecutorSteps.class
})
@CheckouterApiDataSource
@MarketBillingDataSource
@RunWith(Parameterized.class)
public class CpaOrderFeeTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    private static final double DEFAULT_PRICE = 250.0;

    private static final long SHOP_ID = ShopsProvider.getShop("CpaOrderFeeTest");
    private static final int FEED_ID = 200305673;
    private static final int REGION_ID = 2;
    private static final Currency CURRENCY = CurrencyProvider.getCurrencyByShopId(SHOP_ID);

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public final ReportAvailabilityRule reportAvailabilityRule = new ReportAvailabilityRule();
    @Rule
    public final ReactiveJUnitErrorRule reactiveJUnitErrorRule = new ReactiveJUnitErrorRule();

    @Parameterized.Parameter(0)
    public OfferCount offer;

    @Parameterized.Parameter(1)
    @Parameter("Коммиссия(fee) заказа")
    public Double fee;

    @Autowired
    private CheckoutSteps checkoutSteps;
    @Autowired
    private PaySteps paySteps;
    @Autowired
    private CpaOrderRepository cpaOrderRepository;
    @Autowired
    private CpaOrderFeeSteps cpaOrderFeeSteps;
    @Autowired
    private GetOrdersExecutorSteps getOrdersExecutorSteps;
    @Autowired
    private ApplicationContext context;

    @Parameterized.Parameters(name = "Тест скидки {1} на заказе {0}")
    public static Collection<Object[]> fees() {
        return Arrays.asList(
                // [offer, fee]
                new Object[]{new OfferCount("5000", 1), 0.50},
                new Object[]{new OfferCount("1000", 1), 0.10},
                new Object[]{new OfferCount("500", 1), 0.05},
                new Object[]{new OfferCount("300", 1), 0.03},
                new Object[]{new OfferCount("200", 1), 0.02},
                new Object[]{new OfferCount("100", 1), 0.01}
        );
    }

    @Test
    @Title("Тест коммиссии {1} на заказе {0}")
    public void test() throws IOException {
        TestDataItem testDataItem = ReportItemsProvider.itemForFeed(
                FEED_ID, offer.getOfferId(), offer.getCount(), Integer.toString(REGION_ID));
        testDataItem.setPrice(DEFAULT_PRICE);
        testDataItem.setBuyerPrice(DEFAULT_PRICE);
        testDataItem.setCount(offer.getCount());

        TestDataOrder testDataOrder = (TestDataOrder) context.getBean("testDataOrder",
                SHOP_ID, new TestDataItem[]{testDataItem});

        TestDataMultiOrder testDataMultiOrder = (TestDataMultiOrder) context.getBean("testDataMultiOrder",
                testDataOrder);

        CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> checkouterApiRequestData =
                (CheckoutApiRequest) context.getBean("checkouterApiRequestData", testDataMultiOrder);

        testDataOrder = checkoutSteps.createOrder(checkouterApiRequestData);
        paySteps.payAndDelivery(testDataOrder, false);

        TimeoutUtils.waitFor(65, TimeUnit.SECONDS);

        getOrdersExecutorSteps.runTmsJobGetOrdersExecutor();

        CpaOrder cpaOrder = cpaOrderRepository.findOne(testDataOrder.getId().intValue());

        assertThat(cpaOrder, describedAs("Заказа с orderId %0 нет в базе биллинга",
                is(notNullValue()), testDataOrder.getId()));

        double expectedFeeSum = cpaOrderFeeSteps.calculateFeeSum(cpaOrder.getItemsTotalUe(), fee);
        assertThat(cpaOrder.getFeeSum(),
                describedAs("fee_sum заказа должна равняться items_total_ue(%0) * fee(%1) = fee_sum(%2)",
                        equalTo(expectedFeeSum),
                        cpaOrder.getItemsTotalUe(), fee, expectedFeeSum));

        double expectedFeeCorrect = cpaOrderFeeSteps.calculateFeeCorrect(cpaOrder.getItemsTotalUe(), fee);
        assertThat(cpaOrder.getFeeCorrect(),
                describedAs("fee_correct заказа должна равняться " +
                                "items_total_ue(%0 ) * (fee(%1) - DISCOUNT(%2)) = fee_correct(%3)",
                        equalTo(expectedFeeCorrect),
                        cpaOrder.getItemsTotalUe(), fee, CpaOrderFeeSteps.DISCOUNT, expectedFeeCorrect));
    }

    /**
     *
     */
    @Configuration
    static class CpaOrderFeeSupport {
        // default buyer for checkouter context
        private final Buyer defaultBuyer;

        @Autowired
        CpaOrderFeeSupport(Buyer defaultBuyer) {
            this.defaultBuyer = defaultBuyer;
        }

        /**
         * FIXME extract to report context
         *
         * @return
         */
        @Bean
        OffersParser offersParser() {
            return new OffersParser();
        }

        @Bean
        @Scope(SCOPE_PROTOTYPE)
        TestDataMultiOrder testDataMultiOrder(TestDataOrder testDataOrder) {
            TestDataMultiOrder testDataMultiOrder = new TestDataMultiOrder();

            testDataMultiOrder.setBuyerRegionId(REGION_ID);
            testDataMultiOrder.setBuyerCurrency(CURRENCY.name());
            testDataMultiOrder.setPaymentType(PaymentType.PREPAID);
            testDataMultiOrder.setPaymentMethod(PaymentMethod.YANDEX);
            testDataMultiOrder.setOrders(Collections.singletonList(testDataOrder));
            testDataMultiOrder.setBuyer(defaultBuyer);

            return testDataMultiOrder;
        }

        /**
         * Has external parameter {@code TestDataOrder}, that should be passed manually or through another bean
         * Typical usage is:
         * <pre>
         *     &#064;Autowired
         *     ApplicationContext context;
         *     ...
         *
         *     CheckouterApiRequestData request = context.getBean(CheckouterApiRequestData.class, testDataMultiOrder);
         * </pre>
         *
         * @param testDataMultiOrder - created order body for
         * @return
         */
        @Bean
        @Scope(SCOPE_PROTOTYPE)
        CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> checkouterApiRequestData(TestDataMultiOrder testDataMultiOrder) {
            // Create checkout request
            return CheckoutRequests.request()
                    .withBody(BodyMapper.map(testDataMultiOrder, CheckoutRequestBody.class))
                    .withUid(PaymentTestsDataProvider.readWriteUserId1());
        }
    }
}
