package ru.yandex.market.loyalty.core.trigger.restrictions;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType.MANDATORY_TRIGGERS;
import static ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.ActionOnceRestrictionType.CHECK_ACTION_PERIOD;
import static ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.ActionOnceRestrictionType.CHECK_USER;
import static ru.yandex.market.loyalty.core.utils.EventFactory.noAuth;
import static ru.yandex.market.loyalty.core.utils.EventFactory.orderStatusUpdated;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withMuid;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withOrderId;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.actionOnceRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class TriggerActionOnceRestrictionTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoService promoService;
    @Autowired
    private ClockForTests clock;

    @Test
    public void shouldCreateCoinForNoAuthUserWithNullUid() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, actionOnceRestriction(CHECK_USER));

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                b -> b.setUid(null),
                withMuid(DEFAULT_MUID),
                withOrderId(1L),
                noAuth()
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);
    }

    @Test
    public void shouldNotCreateCoinIfUserAlreadyGotCoinThisDay() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, actionOnceRestriction(CHECK_ACTION_PERIOD));

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(1L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(2L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);
    }

    @Test
    public void shouldNotCreateGroupCoinIfUserAlreadyGotGroupCoinThisDay() {
        final String promoGroupId = "test";
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setPromoGroupId(promoGroupId));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                actionOnceRestriction(CHECK_ACTION_PERIOD)
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(1L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(2L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);
    }

    @Test
    public void shouldCreateNewCoinNextDay() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, actionOnceRestriction(CHECK_ACTION_PERIOD));

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(1L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);

        spendDay(clock);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(3L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);


        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.valueOf(2L));
    }

    @Test
    public void shouldCreateNewGroupCoinNextDay() {
        final String promoGroupId = "test";
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping
                        .defaultFixed()
                        .setPromoGroupId(promoGroupId)
        );

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, actionOnceRestriction(CHECK_ACTION_PERIOD));

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(1L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);

        spendDay(clock);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(3L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.valueOf(2L));
    }

    @Test
    public void shouldCreateSingleGroupCoinSerially() {
        final String promoGroupId = "test";

        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setPromoGroupId(promoGroupId)
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo1,
                MANDATORY_TRIGGERS,
                actionOnceRestriction(CHECK_ACTION_PERIOD)
        );

        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setPromoGroupId(promoGroupId)
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo2,
                MANDATORY_TRIGGERS,
                actionOnceRestriction(CHECK_ACTION_PERIOD)
        );

        Promo promo3 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setPromoGroupId(promoGroupId)
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo3,
                MANDATORY_TRIGGERS,
                actionOnceRestriction(CHECK_ACTION_PERIOD)
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(1L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetsDecreasedBy(
                Arrays.asList(
                        DEFAULT_EMISSION_BUDGET_IN_COINS,
                        DEFAULT_EMISSION_BUDGET_IN_COINS,
                        DEFAULT_EMISSION_BUDGET_IN_COINS
                ),
                Arrays.asList(
                        promoService.getPromo(promo1.getId()).getCurrentEmissionBudget(),
                        promoService.getPromo(promo2.getId()).getCurrentEmissionBudget(),
                        promoService.getPromo(promo3.getId()).getCurrentEmissionBudget()
                ),
                BigDecimal.ONE
        );
    }

    @Test
    @Repeat(5)
    public void shouldCreateSingleGroupCoinParallel() {
        final String promoGroupId = "test";

        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setPromoGroupId(promoGroupId)
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo1,
                MANDATORY_TRIGGERS,
                actionOnceRestriction(CHECK_ACTION_PERIOD)
        );

        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setPromoGroupId(promoGroupId)
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo2,
                MANDATORY_TRIGGERS,
                actionOnceRestriction(CHECK_ACTION_PERIOD)
        );

        Promo promo3 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setPromoGroupId(promoGroupId)
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo3,
                MANDATORY_TRIGGERS,
                actionOnceRestriction(CHECK_ACTION_PERIOD)
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(1L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO, true, null);

        assertEmissionBudgetsDecreasedBy(
                Arrays.asList(
                        DEFAULT_EMISSION_BUDGET_IN_COINS,
                        DEFAULT_EMISSION_BUDGET_IN_COINS,
                        DEFAULT_EMISSION_BUDGET_IN_COINS
                ),
                Arrays.asList(
                        promoService.getPromo(promo1.getId()).getCurrentEmissionBudget(),
                        promoService.getPromo(promo2.getId()).getCurrentEmissionBudget(),
                        promoService.getPromo(promo3.getId()).getCurrentEmissionBudget()
                ),
                BigDecimal.ONE
        );
    }

    @Test
    public void shouldNotCreateCoinIfUserIsNoAuth() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, actionOnceRestriction(CHECK_ACTION_PERIOD));

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_MUID),
                noAuth(),
                withOrderId(1L)
        ));

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ZERO);
    }

    @Test
    public void shouldNotCreateCoinIfUserAlreadyGotCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, actionOnceRestriction(CHECK_USER));

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(1L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(2L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo, DEFAULT_EMISSION_BUDGET_IN_COINS, BigDecimal.ONE);
    }


    @Test
    public void shouldCreateGroupCoinForUserAndPeriodPromos() {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setPromoGroupId(
                "test"));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo1, actionOnceRestriction(CHECK_USER));

        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setPromoGroupId(
                "test"));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo2, actionOnceRestriction(CHECK_ACTION_PERIOD));

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(1L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        BigDecimal emissionBudget1 = promoService.getPromo(promo1.getId()).getCurrentEmissionBudget();
        BigDecimal emissionBudget2 = promoService.getPromo(promo2.getId()).getCurrentEmissionBudget();

        assertEmissionBudgetsDecreasedBy(
                Arrays.asList(DEFAULT_EMISSION_BUDGET_IN_COINS, DEFAULT_EMISSION_BUDGET_IN_COINS),
                Arrays.asList(emissionBudget1, emissionBudget2),
                BigDecimal.ONE
        );

        spendDay(clock);

        emissionBudget1 = promoService.getPromo(promo1.getId()).getCurrentEmissionBudget();
        emissionBudget2 = promoService.getPromo(promo2.getId()).getCurrentEmissionBudget();

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID),
                withOrderId(2L)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertEmissionBudgetDecreasedBy(promo1, emissionBudget1, BigDecimal.ZERO);
        assertEmissionBudgetDecreasedBy(promo2, emissionBudget2, BigDecimal.ONE);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertEmissionBudgetsDecreasedBy(
            List<BigDecimal> initialBudgets, List<BigDecimal> currentBudgets, BigDecimal spent
    ) {
        assertThat(
                currentBudgets.stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                comparesEqualTo(initialBudgets.stream().reduce(BigDecimal.ZERO, BigDecimal::add).subtract(
                        spent))
        );
    }

    private void spendDay(ClockForTests clock) {
        clock.spendTime(1, ChronoUnit.DAYS);
        this.clock.spendTime(1, ChronoUnit.SECONDS);
    }

    private void assertEmissionBudgetDecreasedBy(Promo promo, BigDecimal initialEmissionBudget, BigDecimal spent) {
        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(initialEmissionBudget.subtract(spent))
        );
    }
}
