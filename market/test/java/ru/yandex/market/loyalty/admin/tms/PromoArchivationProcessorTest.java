package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.CorePromoType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.BindingUtils;
import ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.RefreshableBindingService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.loyalty.api.model.PromoStatus.ACTIVE;
import static ru.yandex.market.loyalty.api.model.PromoStatus.ARCHIVED;
import static ru.yandex.market.loyalty.api.model.PromoStatus.ARCHIVING;
import static ru.yandex.market.loyalty.api.model.PromoStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.PROMO_GROUP_ID;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_ARCHIVING_AGE;
import static ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.RefreshableBindingPredicate.adapt;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.defaultPercent;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;

@TestFor(PromoArchivationProcessor.class)
public class PromoArchivationProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    final static Duration JOB_DURATION = Duration.ofSeconds(1);
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoArchivationProcessor promoArchivationProcessor;
    @Autowired
    private RefreshableBindingService refreshableBindingService;
    @Autowired
    private TriggersFactory triggersFactory;

    @Test
    public void shouldUpdatePromoState() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE,
                CashbackLevelType.ITEM));

        promoService.updateStatus(promo, PromoStatus.ARCHIVING);

        promoArchivationProcessor.archivePromos(JOB_DURATION);

        assertEquals(ARCHIVED, promoService.getPromo(promo.getPromoId().getId()).getStatus());
    }

    @Test
    public void shouldDeletePromoBindings() {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        var userUid1 = 1L;
        var userUid2 = 2L;

        refreshableBindingService.bind(Set.of(userUid1, userUid2), BindingUtils.getBindingKey(promo1), adapt(b -> false));
        refreshableBindingService.bind(Set.of(userUid1, userUid2), BindingUtils.getBindingKey(promo2), adapt(b -> false));

        promoService.updateStatus(promo1, PromoStatus.ARCHIVING);
        promoArchivationProcessor.archivePromos(CorePromoType.SMART_SHOPPING, JOB_DURATION);

        // should delete bindings for promo1...
        var actual = refreshableBindingService.getBindings(
                BindingUtils.getBindingKey(promo1), Set.of(userUid1, userUid2));
        assertTrue(actual.isEmpty());
        // ...but not for promo2
        actual = refreshableBindingService.getBindings(
                BindingUtils.getBindingKey(promo2), Set.of(userUid1, userUid2));
        assertThat(actual.size(), is(2));
        var deletedKey = BindingUtils.getBindingKey(promo1);
        assertTrue(actual.stream().noneMatch((b) -> b.getBindingKey().equals(deletedKey)));
    }

    @Test
    public void shouldDeletePromoGroupAndTriggerBindings() {
        var promoGroupId = "test_group";
        Promo promo1 = promoManager.createSmartShoppingPromoWithParams(PromoUtils.SmartShopping.defaultFixed(),
                Map.of(PROMO_GROUP_ID, promoGroupId));
        Promo promo2 = promoManager.createSmartShoppingPromoWithParams(PromoUtils.SmartShopping.defaultFixed(),
                Map.of(PROMO_GROUP_ID, promoGroupId));
        var groupKey = BindingUtils.getBindingKey(promo1);

        var user1 = 1L;
        var user2 = 2L;
        refreshableBindingService.bind(Set.of(user1, user2), groupKey, adapt(b -> false));

        var triggerKey = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo1, orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        ).getId().toString();
        refreshableBindingService.bind(Set.of(user1, user2), triggerKey, adapt(b -> false));

        // should not delete group bindings for promo1...
        promoService.updateStatus(promo1, PromoStatus.ARCHIVING);
        promoArchivationProcessor.archivePromos(CorePromoType.SMART_SHOPPING, JOB_DURATION);
        var actual = refreshableBindingService.getBindings(groupKey, Set.of(user1, user2));
        assertThat(actual.size(), is(2));
        // ...but trigger bindings only
        actual = refreshableBindingService.getBindings(triggerKey, Set.of(user1, user2));
        assertTrue(actual.isEmpty());

        // should delete all bindings for promo group
        promoService.updateStatus(promo2, PromoStatus.ARCHIVING);
        promoArchivationProcessor.archivePromos(CorePromoType.SMART_SHOPPING, JOB_DURATION);
        actual = refreshableBindingService.getBindings(groupKey, Set.of(user1, user2));
        assertTrue(actual.isEmpty());
    }

    @Test
    public void shouldUpdatePromoStatusToArchiving() {
        int promoAge = 6;
        configurationService.set(PROMO_ARCHIVING_AGE, promoAge);
        Date inFuture = Date.from(LocalDateTime.now(clock).plusMonths(promoAge).toInstant(ZoneOffset.UTC));
        Date inMonthBeforePromoAge = Date.from(LocalDateTime.now(clock).minusMonths(1 + promoAge).toInstant(ZoneOffset.UTC));

        // fresh promo
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEndDate(inFuture)
                .setStatus(ACTIVE));
        // old but active promo
        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEndDate(inMonthBeforePromoAge)
                .setStatus(ACTIVE));
        // old inactive promo
        Promo promo3 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEndDate(inMonthBeforePromoAge)
                .setStatus(INACTIVE));

        promoArchivationProcessor.archivePromos(JOB_DURATION);

        // should not update promo status
        assertEquals(ACTIVE, promoService.getPromo(promo1.getPromoId().getId()).getStatus());
        // should deactivate promo by end date
        assertEquals(INACTIVE, promoService.getPromo(promo2.getPromoId().getId()).getStatus());
        // should update promo status
        assertEquals(ARCHIVING, promoService.getPromo(promo3.getPromoId().getId()).getStatus());
    }
}
