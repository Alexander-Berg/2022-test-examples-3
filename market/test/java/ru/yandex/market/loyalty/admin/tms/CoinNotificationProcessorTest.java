package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.OperationContextDao;
import ru.yandex.market.loyalty.core.logbroker.LogBrokerEvent;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.antifraud.AsyncUserRestrictionResult;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coin.SourceKey;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COIN_ACTIVATED;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COIN_BIND;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COIN_CREATED;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COIN_EXPIRED;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COIN_UPDATED;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COIN_USED;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason.MARKETING;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.DEFAULT_ACTIVATION_TOKEN;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UUID;

@TestFor(CoinNotificationProcessor.class)
public class CoinNotificationProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Captor
    private ArgumentCaptor<List<LogBrokerEvent>> logBrokerEventsCaptor;

    @Autowired
    private CoinNotificationProcessor coinNotificationProcessor;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private MetaTransactionDao metaTransactionDao;
    @Autowired
    private OperationContextDao operationContextDao;
    @Autowired
    private CoinEndProcessor coinEndProcessor;
    private LocalDateTime LAST_MONTH;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        LAST_MONTH = LocalDateTime.now(clock).minusMonths(1).plusMinutes(1);
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutAuthCoinCreation() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        coinNotificationProcessor.processCreatedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should().pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getAllValues(), contains(
                containsInAnyOrder(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_CREATED)),
                                hasProperty("user", allOf(
                                        hasProperty("uid", equalTo(DEFAULT_UID)),
                                        hasProperty("uuid", nullValue())
                                )),
                                hasProperty("coin", allOf(
                                        hasProperty("id", equalTo(coinKey.getId())),
                                        hasProperty("platform", equalTo(MarketPlatform.BLUE))
                                ))
                        ),
                        allOf(
                                hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                                hasProperty("requireAuth", equalTo(false))
                        )
                )
        ));
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutNoAuthCoinCreation() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        coinNotificationProcessor.processCreatedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should().pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getAllValues(), contains(
                containsInAnyOrder(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_CREATED)),
                                hasProperty("user", allOf(
                                        hasProperty("uid", nullValue()),
                                        hasProperty("uuid", equalTo(DEFAULT_UUID))
                                )),
                                hasProperty("coin", allOf(
                                        hasProperty("id", equalTo(coinKey.getId())),
                                        hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                                        hasProperty("requireAuth", equalTo(true)),
                                        hasProperty("platform", equalTo(MarketPlatform.BLUE))
                                ))
                        ),
                        allOf(
                                hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                                hasProperty("requireAuth", equalTo(true))
                        )
                )
        ));
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutCoinWithoutUserInfoCreation() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        Set<SourceKey> sourceKeys = Collections.singleton(new SourceKey(UUID.randomUUID().toString()));
        coinService.create.insertNoAuthCoins(promo.getPromoId().getId(), sourceKeys, MARKETING);

        coinNotificationProcessor.processCreatedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should().pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getAllValues(), contains(
                containsInAnyOrder(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_CREATED)),
                                hasProperty("user", allOf(
                                        hasProperty("uid", nullValue()),
                                        hasProperty("uuid", nullValue())
                                )),
                                hasProperty("coin", allOf(
                                        hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                                        hasProperty("requireAuth", equalTo(true)),
                                        hasProperty("platform", equalTo(MarketPlatform.BLUE))
                                ))
                        ),
                        allOf(
                                hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                                hasProperty("requireAuth", equalTo(true))
                        )
                )
        ));
    }

    @Test
    public void shouldSendReasonAndReasonParamToCrm() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coin = coinService.create.createCoin(promo, defaultAuth()
                .setReason(CoreCoinCreationReason.ORDER)
                .setReasonParam("1")
                .build()
        );

        coinNotificationProcessor.processCreatedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(times(1)).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getAllValues(), contains(
                containsInAnyOrder(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_CREATED)),
                                hasProperty("user", allOf(
                                        hasProperty("uid", equalTo(DEFAULT_UID)),
                                        hasProperty("uuid", nullValue())
                                )),
                                hasProperty("coin",
                                        allOf(hasProperty("id", equalTo(coin.getId())),
                                                hasProperty("reason", equalTo(CoinCreationReason.ORDER)),
                                                hasProperty("reasonParam", equalTo("1")),
                                                hasProperty("platform", equalTo(MarketPlatform.BLUE))
                                        )
                                )
                        ),
                        allOf(
                                hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                                hasProperty("requireAuth", equalTo(false))
                        )
                ))
        );
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutCoinCreationOnlyOnce() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        coinService.create.createCoin(promo, defaultAuth().build());

        coinNotificationProcessor.processCreatedCoinsNotifications(50, LAST_MONTH);
        coinNotificationProcessor.processCreatedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(times(1)).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getAllValues(), contains(containsInAnyOrder(
                hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                hasProperty("eventType", equalTo(COIN_CREATED))
        )));
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutCoinUsage() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setPlatform(
                CoreMarketPlatform.RED));

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());
        long operationContextId = operationContextDao.save(OperationContextFactory.uidOperationContext());
        coinService.lifecycle.useCoin(coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                ImmutableSet.of(createCommonTransactionId()), operationContextId);

        coinNotificationProcessor.processUsedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(only()).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getValue(),
                contains(allOf(
                        hasProperty("eventType", equalTo(COIN_USED)),
                        hasProperty("coin", allOf(
                                hasProperty("id", equalTo(coinKey.getId())),
                                hasProperty("platform", equalTo(MarketPlatform.RED))
                        ))
                        )
                )
        );
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutCoinExpiration() throws InterruptedException {
        Promo promo =
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setPlatform(CoreMarketPlatform.RED));

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        coinNotificationProcessor.processExpiredCoinsNotifications(50, LAST_MONTH);
        then(logBrokerClient).should(never()).pushEvent(any());

        clock.spendTime(Duration.ofDays(180));
        coinEndProcessor.process(10_000L);
        coinNotificationProcessor.processExpiredCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(only()).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getValue(),
                contains(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_EXPIRED)),
                                hasProperty("user", hasProperty("uid", equalTo(DEFAULT_UID))),
                                hasProperty("coin", allOf(
                                        hasProperty("id", equalTo(coinKey.getId())),
                                        hasProperty("platform", equalTo(MarketPlatform.RED)))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutCoinUsageOnlyOnce() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());
        long operationContextId = operationContextDao.save(OperationContextFactory.uidOperationContext());
        coinService.lifecycle.useCoin(coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                ImmutableSet.of(createCommonTransactionId()), operationContextId);

        coinNotificationProcessor.processUsedCoinsNotifications(50, LAST_MONTH);
        coinNotificationProcessor.processUsedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(only()).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getValue(),
                contains(
                        hasProperty("eventType", equalTo(COIN_USED))
                )
        );
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutCoinBound() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        coinNotificationProcessor.processBoundCoinsNotifications(50, LAST_MONTH);
        then(logBrokerClient).should(never()).pushEvent(any());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN);
        coinNotificationProcessor.processBoundCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(only()).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getValue(),
                contains(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_BIND)),
                                hasProperty("user", allOf(
                                        hasProperty("uid", equalTo(DEFAULT_UID)),
                                        hasProperty("uuid", equalTo(DEFAULT_UUID))
                                )),
                                hasProperty("coin", allOf(
                                        hasProperty("id", equalTo(coinKey.getId())),
                                        hasProperty("platform", equalTo(MarketPlatform.BLUE)))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldSendNotificationToLogBrokerAboutCoinBoundOnlyOnce() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        coinService.create.createCoin(promo, defaultNoAuth().build());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN);

        coinNotificationProcessor.processBoundCoinsNotifications(50, LAST_MONTH);
        coinNotificationProcessor.processBoundCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(only()).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getValue(),
                contains(
                        hasProperty("eventType", equalTo(COIN_BIND))
                )
        );
    }

    @Test
    public void shouldSendNotificationToLogBrokerWhenGivenActivatedBlueCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setPlatform(CoreMarketPlatform.BLUE));

        CoinKey coinKey = coinService.create
                .createCoin(promo, defaultAuth(DEFAULT_UID)
                        .setStatus(INACTIVE)
                        .build()
                );

        List<Coin> coins = coinService.search
                .getCoin(coinKey)
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);

        coinService.lifecycle.activateInactiveCoins(CoreMarketPlatform.BLUE, coins);

        coinNotificationProcessor.processActivatedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(times(1)).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getValue(),
                contains(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_ACTIVATED)),
                                hasProperty("user", allOf(
                                        hasProperty("uid", equalTo(DEFAULT_UID))
                                )),
                                hasProperty("coin", allOf(
                                        hasProperty("id", equalTo(coinKey.getId())),
                                        hasProperty("platform", equalTo(MarketPlatform.BLUE)))
                                )
                        )
                )
        );
    }


    @Test
    public void shouldSendNotificationToLogBrokerWhenGivenActivatedRedCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setPlatform(CoreMarketPlatform.RED));

        CoinKey coinKey = coinService.create
                .createCoin(promo, defaultAuth(DEFAULT_UID)
                        .setStatus(INACTIVE)
                        .build()
                );

        List<Coin> coins = coinService.search
                .getCoin(coinKey)
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);

        coinService.lifecycle.activateInactiveCoins(CoreMarketPlatform.RED, coins);

        coinNotificationProcessor.processActivatedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(times(1)).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getValue(),
                contains(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_ACTIVATED)),
                                hasProperty("user", allOf(
                                        hasProperty("uid", equalTo(DEFAULT_UID))
                                )),
                                hasProperty("coin", allOf(
                                        hasProperty("id", equalTo(coinKey.getId())),
                                        hasProperty("platform", equalTo(MarketPlatform.RED)))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldSendNotificationToLogBrokerWhenGivenUpdatedCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinKey = coinService.create
                .createCoin(
                        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()),
                        defaultAuth(DEFAULT_UID)
                                .setStatus(INACTIVE)
                                .build()
                );

        coinService.lifecycle.updateCoinsProps(Set.of(coinKey), promo.getCoinPropsId(), null);

        coinNotificationProcessor.processUpdatedCoinsNotifications(50, LAST_MONTH);

        then(logBrokerClient).should(times(1)).pushEvents(logBrokerEventsCaptor.capture());

        assertThat(logBrokerEventsCaptor.getValue(),
                contains(
                        allOf(
                                hasProperty("eventType", equalTo(COIN_UPDATED)),
                                hasProperty("user", allOf(
                                        hasProperty("uid", equalTo(DEFAULT_UID))
                                )),
                                hasProperty("coin", allOf(
                                        hasProperty("id", equalTo(coinKey.getId()))
                                ))
                        )
                )
        );
    }

    @Test
    public void testSendNotificationsAmountLimits() {
        final int testCoinAmount = 50;
        var promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionBudget(BigDecimal.valueOf(100_000_000))
        );
        var coins = coinService.create.createCoinsBatch(
                promo,
                LongStream.range(0, testCoinAmount).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        ).getCoinIds().size();

        assertThat(coins, equalTo(testCoinAmount));
        // should push only 10 events
        coinNotificationProcessor.processCreatedCoinsNotifications(10, LAST_MONTH);
        then(logBrokerClient).should(times(10)).pushEvents(logBrokerEventsCaptor.capture());
        // should push all 50 events
        coinNotificationProcessor.processCreatedCoinsNotifications(10_000, LAST_MONTH);
        then(logBrokerClient).should(times(testCoinAmount)).pushEvents(logBrokerEventsCaptor.capture());
    }


    private long createCommonTransactionId() {
        return metaTransactionDao.createTransaction("asdas", 123L, null);
    }
}
