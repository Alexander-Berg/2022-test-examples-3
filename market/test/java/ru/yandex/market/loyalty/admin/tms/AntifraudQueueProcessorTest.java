package ru.yandex.market.loyalty.admin.tms;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType;
import ru.yandex.market.loyalty.api.model.coin.CoinHistoryReason;
import ru.yandex.market.loyalty.core.config.properties.AntifraudExecutorProperties;
import ru.yandex.market.loyalty.core.dao.antifraud.FraudCoinDisposeQueueDao;
import ru.yandex.market.loyalty.core.mock.AntiFraudMockUtil;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.antifraud.AntiFraudService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.DiscountHistoryService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;

@TestFor(AntifraudQueueProcessor.class)
public class AntifraudQueueProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private AntiFraudMockUtil antiFraudMockUtil;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private FraudCoinDisposeQueueDao fraudCoinDisposeQueueDao;
    @Autowired
    private AntifraudQueueProcessor antifraudQueueProcessor;
    @Autowired
    private DiscountHistoryService discountHistoryService;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private AntiFraudService antifraudService;
    @Autowired
    private AntifraudExecutorProperties executorProps;

    @Test
    public void shouldProcessUsedCoin() throws Exception {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinForAuthKey = coinService.create.createCoin(promo, defaultAuth().build());

        antiFraudMockUtil.coinWasUsed(coinForAuthKey);

        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .build()
        )
                .withCoins(coinForAuthKey);

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(),null);

        antifraudService.awaitAllExecutors(executorProps);
        Thread.sleep(50);

        checkFraudProcessing(coinForAuthKey, CoinHistoryReason.USED_BY_OTHER_UID);
    }

    private void checkFraudProcessing(CoinKey coinForAuthKey, CoinHistoryReason coinHistoryReason) {
        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(1), is(not(anEmptyMap())));

        assertThat(coinService.search.getCoin(coinForAuthKey).orElseThrow(AssertionError::new),
                hasProperty("status", equalTo(CoreCoinStatus.ACTIVE))
        );

        assertThat(discountHistoryService.getCoinHistoryRecords(coinForAuthKey), not(hasItem(
                hasProperty("recordType", equalTo(DiscountHistoryRecordType.REVOCATION))
        )));

        antifraudQueueProcessor.processQueue();

        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(1), is(anEmptyMap()));

        assertThat(coinService.search.getCoin(coinForAuthKey).orElseThrow(AssertionError::new),
                hasProperty("status", equalTo(CoreCoinStatus.REVOKED))
        );

        assertThat(discountHistoryService.getCoinHistoryRecords(coinForAuthKey), hasItem(allOf(
                hasProperty("recordType", equalTo(DiscountHistoryRecordType.REVOCATION)),
                hasProperty("reason", equalTo(coinHistoryReason))
        )));

        clock.spendTime(2, ChronoUnit.HOURS);

        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(1), is(not(anEmptyMap())));

        antifraudQueueProcessor.processQueue();

        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(1), is(anEmptyMap()));

        clock.spendTime(2, ChronoUnit.HOURS);

        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(1), is(anEmptyMap()));
    }

    @Test
    public void shouldProcessBlacklist() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinForAuthKey = coinService.create.createCoin(promo, defaultAuth().build());

        antiFraudMockUtil.userInBlacklist();

        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .build()
        )
                .withCoins(coinForAuthKey);

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);

        antifraudService.awaitAllExecutors(executorProps);
        discountService.awaitClearDatasourceTypeExecutor();

        checkFraudProcessing(coinForAuthKey, CoinHistoryReason.BLACKLIST);
    }
}
