package ru.yandex.market.loyalty.core.trigger.restrictions;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.DataVersionDao;
import ru.yandex.market.loyalty.core.dao.UserBlackListDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.BlacklistRecord;
import ru.yandex.market.loyalty.core.model.DataVersion;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.service.BadUeUsersService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.UserBlacklistService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.utils.EventFactory.DEFAULT_ORDER_STATUS_PREDICATE;
import static ru.yandex.market.loyalty.core.utils.EventFactory.noAuth;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUserEmail;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;

public class BadUeUsersRestrictionTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoService promoService;
    @Autowired
    private UserBlackListDao userBlackListDao;
    @Autowired
    private UserBlacklistService userBlacklistService;
    @Autowired
    private BadUeUsersService badUeUsersService;
    @Autowired
    private DataVersionDao dataVersionDao;
    @Autowired
    private TriggerEventDao triggerEventDao;

    @Before
    public void init() {
        badUeUsersService.reloadBlacklist();
    }

    @Test
    public void shouldNotEmmitCoinForUserFromUidBlacklist() {
        long uid = 12312313123L;
        userBlackListDao.addRecord(new BlacklistRecord.Uid(uid));
        userBlacklistService.reloadBlacklist();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(uid)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotEmmitCoinForUserFromMarketdiscount2917UidBlacklist() {
        long uid = 12312313123L;

        badUeUsersService.addUid(1, uid);
        dataVersionDao.saveDataVersion(DataVersion.BAD_UE_USERS, 1);
        badUeUsersService.reloadBlacklist();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(uid)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldEmmitCoinForUserNotFromUidBlacklist() {
        long uid = 12312313123L;

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(uid)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotEmitCoinForUserFromCallcenter() {
        long uid = 893185881L;
        userBlacklistService.reloadBlacklist();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(uid)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotEmitCoinForUserFromCallcenterByEmail() {
        String email = "stationorders@yandex.ru";
        userBlacklistService.reloadBlacklist();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUserEmail(email), noAuth()));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotEmmitCoinForUserFromEmailBlacklist() {
        String email = "someFraud@example.com";
        userBlackListDao.addRecord(new BlacklistRecord.Email(email));
        userBlacklistService.reloadBlacklist();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUserEmail(email), noAuth()));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }


    @Test
    public void shouldEmmitCoinForBadUeUserIfPromoAllowThis() {
        long uid = 12312313123L;

        badUeUsersService.addUid(1, uid);
        dataVersionDao.saveDataVersion(DataVersion.BAD_UE_USERS, 1);
        badUeUsersService.reloadBlacklist();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTrigger(
                promo,
                triggersFactory.createCoinAction("{\"badUeUserAllowed\": true}"),
                TriggerGroupType.MANDATORY_TRIGGERS
        );

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(uid)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );


        final List<TriggerEvent> events = triggerEventDao.getAll();

        assertEquals(1, events.size());
        assertThat(
                events.get(0),
                allOf(
                        hasProperty("processedResult", equalTo(TriggerEventProcessedResult.FORBIDDEN))
                )
        );
    }
}
