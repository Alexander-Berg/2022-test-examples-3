package ru.yandex.market.loyalty.core.service.antifraud;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.core.config.properties.AntifraudExecutorProperties;
import ru.yandex.market.loyalty.core.mock.AntiFraudMockUtil;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.UserInfo;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.monitor.CoreMonitorType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.DiscountAntifraudService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.CoinRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.monitoring.PushMonitor;

import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.api.model.bundle.util.BundleAdapterUtils.adaptFrom;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PHARMA_LIST_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@Log4j2
@ActiveProfiles("monitor-mock-test")
public class AntiFraudExecutorTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final int QUEUE_OVERLOAD = 20;

    @Autowired
    private AntiFraudMockUtil antiFraudMockUtil;
    @Autowired
    private AntifraudExecutorProperties executorProperties;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private AntifraudUserRestrictionsSupplier userRestrictionsSupplier;
    @Autowired
    private DiscountAntifraudService discountAntifraudService;
    @Autowired
    private AntiFraudService antiFraudService;
    @Autowired
    private PushMonitor monitor;

    @Test
    public void checkDetectPoolException() throws Exception {
        antiFraudMockUtil.detectWithSleep();

        var coinKey = createCoin(promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setPlatform(CoreMarketPlatform.RED)
        ));

        var request = DiscountRequestBuilder.builder(
                OrderRequestUtils.orderRequestBuilder()
                        .withOrderItem()
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                categoryId(PHARMA_LIST_CATEGORY_ID)
                        )
                        .build()
        )
                .withCoins(coinKey)
                .withPlatform(MarketPlatform.BLUE)
                .withOperationContext(OperationContextFactory.withUidBuilder(10).buildOperationContext())
                .build();

        runExecutionAndCheckTasks(
                () -> spendDiscount(request),
                executorProperties.getDetectPool() + executorProperties.getDetectQueue() + QUEUE_OVERLOAD
        );

        verifyErrorsThroughPushMonitor(AntifraudApi.DETECT);
    }

    @Test
    public void checkOrdersCountPoolException() throws Exception {
        antiFraudMockUtil.ordersCountWithSleep();

        runExecutionAndCheckTasks(
                () -> discountAntifraudService.createAntifraudCheckFuture(10L),
                executorProperties.getOrdersCountPool()
                        + executorProperties.getOrdersCountQueue()
                        + QUEUE_OVERLOAD * 10
        );

        verifyErrorsThroughPushMonitor(AntifraudApi.ORDERS_COUNT_V2);
    }

    @Test
    public void checkRestrictionsPoolException() throws Exception {
        antiFraudMockUtil.restrictionsWithSleep();
        var userInfo = UserInfo.builder().setUid(10L).build();

        runExecutionAndCheckTasks(
                () -> userRestrictionsSupplier.getImmediateRestrictionForUserWithUid(userInfo),
                executorProperties.getRestrictionsPool()
                        + executorProperties.getRestrictionsQueue()
                        + QUEUE_OVERLOAD
        );

        verifyErrorsThroughPushMonitor(AntifraudApi.RESTRICTIONS);
    }

    @Test
    public void checkRestrictionsBonusPoolException() throws Exception {
        antiFraudMockUtil.restrictionsBonusWithSleep();

        var coinKey = createCoin(promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setPlatform(CoreMarketPlatform.RED)
        ));

        var request = DiscountRequestBuilder.builder(
                OrderRequestUtils.orderRequestBuilder()
                        .withOrderItem()
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                categoryId(PHARMA_LIST_CATEGORY_ID)
                        )
                        .build()
        )
                .withCoins(coinKey)
                .withPlatform(MarketPlatform.BLUE)
                .withOperationContext(OperationContextFactory.withUidBuilder(10).buildOperationContext())
                .build();

        runExecutionAndCheckTasks(
                () -> spendDiscount(request),
                executorProperties.getRestrictionsBonusPool()
                        + executorProperties.getRestrictionsBonusQueue()
                        + QUEUE_OVERLOAD
        );

        verifyErrorsThroughPushMonitor(AntifraudApi.RESTRICTIONS_BONUS);
    }

    @Test
    public void checkCancellationExceptionCaught() {
        antiFraudMockUtil.restrictionsBonusWithSleep();

        var future = antiFraudService.isBonusRestricted(10L, "10");
        future.get();   // тут в мониторинг сообщаем о TimeoutException
        future.get();   // тут - CancellationException
        // если не перехватим - тест упадет сам по себе

        verify(monitor, times(1))
                .addTemporaryWarning(
                        eq(CoreMonitorType.ANTIFRAUD_SERVICE_FUTURE_FETCH_ERROR),
                        contains("CancellationException"),
                        eq(10L),
                        eq(TimeUnit.MINUTES)
                );
    }

    private CoinKey createCoin(Promo promo) {
        return coinService.create.createCoin(
                promo,
                CoinRequestUtils.defaultAuth(DEFAULT_UID)
                        .setStatus(ACTIVE)
                        .build()
        );
    }

    private MultiCartWithBundlesDiscountResponse spendDiscount(MultiCartDiscountRequest discountRequest) {
        var applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        return discountService.spendDiscount(adaptFrom(discountRequest), applicabilityPolicy, null);
    }

    private <T> void runExecutionAndCheckTasks(Callable<T> callable, int threadsCount) throws Exception {
        var executor = Executors.newFixedThreadPool(threadsCount);

        var tasks = new ArrayList<Callable<T>>();
        for (int i = 0; i < threadsCount; i++) {
            tasks.add(() -> {
                try {
                    return callable.call();
                } catch (Exception ex) {
                    log.error("Error in task:", ex);
                    return null;
                }
            });
        }

        var futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        futures.forEach(future -> assertTrue(
                "Some exception in future: " + future,
                future.toString().contains("[Completed normally]")
        ));
    }

    private void verifyErrorsThroughPushMonitor(AntifraudApi api) {
        verify(monitor, atLeast(QUEUE_OVERLOAD / 10))
                .addTemporaryCritical(
                        eq(CoreMonitorType.ANTIFRAUD_SERVICE_FULL_QUEUE),
                        contains(api.toString()),
                        eq(30L),
                        eq(TimeUnit.MINUTES)
                );

        verify(monitor, times(0))
                .addTemporaryWarning(
                        eq(CoreMonitorType.ANTIFRAUD_SERVICE_FUTURE_FETCH_ERROR),
                        and(
                                contains(api.toString()),
                                or(contains("ExecutionException"), contains("InterruptedException"))
                        ),
                        eq(10L),
                        eq(TimeUnit.MINUTES)
                );
    }

}
