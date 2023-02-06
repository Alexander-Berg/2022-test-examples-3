package ru.yandex.market.loyalty.core.trigger.restrictions.actiononce;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResult;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.trigger.EventHandleMode;
import ru.yandex.market.loyalty.core.service.trigger.TriggerDataCache;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.EmptyProcessResult;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResultStatus.SUCCESS;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUserEmail;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withPersonalPhoneId;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.actionOnceRestriction;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class ActionOnceRestrictionFactoryTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final Promo PROMO = PromoUtils.Coupon.defaultSingleUse().basePromo();
    @Autowired
    private ActionOnceRestrictionFactory actionOnceRestrictionFactory;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PromoManager promoManager;

    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private RefreshableBindingService refreshableBindingService;
    @Autowired
    private DiscountUtils discountUtils;

    @Test
    public void testFitCondition() {
        ActionOnceRestriction restriction = actionOnceRestrictionFactory.create(1L, null);

        OrderStatusUpdatedEvent event = EventFactory.orderStatusUpdated();

        final TriggerDataCache triggerDataCache = new TriggerDataCache();
        assertTrue(restriction.fitCondition(event, triggersFactory.createDefaultPromoTriggerEventData(PROMO),
                triggerDataCache).isMatched());

        restriction.done(event, EmptyProcessResult.getInstance(),
                triggersFactory.createDefaultPromoTriggerEventData(PROMO), EventHandleMode.NORMAL, null);

        assertFalse(restriction.fitCondition(event, triggersFactory.createDefaultPromoTriggerEventData(PROMO),
                new TriggerDataCache()).isMatched());
    }

    @Test
    public void testFitConditionWithDataCache() {
        Trigger<OrderStatusUpdatedEvent> firstTrigger = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()),
                actionOnceRestriction()
        );

        OrderStatusUpdatedEvent firstEvent = EventFactory.orderStatusUpdated();
        List<TriggerActionResult> triggerActionResults = triggerEventQueueService.insertAndProcessEvent(firstEvent,
                discountUtils.getRulesPayload(), BudgetMode.SYNC
        );
        assertThat(triggerActionResults, contains(
                allOf(
                        hasProperty("triggerId", equalTo(firstTrigger.getId())),
                        hasProperty("status", equalTo(SUCCESS))
                )
        ));

        Trigger<OrderStatusUpdatedEvent> secondTrigger = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()),
                actionOnceRestriction()
        );

        OrderStatusUpdatedEvent secondEvent = EventFactory.orderStatusUpdated();
        triggerActionResults = triggerEventQueueService.insertAndProcessEvent(secondEvent,
                discountUtils.getRulesPayload(), BudgetMode.SYNC
        );

        assertThat(triggerActionResults, contains(
                allOf(
                        hasProperty("triggerId", equalTo(secondTrigger.getId())),
                        hasProperty("status", equalTo(SUCCESS))
                )
        ));

    }

    @Test
    public void testFitConditionAnyMatch() {
        ActionOnceRestriction restriction = actionOnceRestrictionFactory.create(1L, null);

        long uid = 123L;
        String email = "fonar101@yandex.ru";
        String personalPhoneId = "+71234567890_id";

        String anotherEmail = "valter@yandex.ru";
        String anotherPersonalPhoneId = "+76666661111_id";

        OrderStatusUpdatedEvent event = EventFactory.orderStatusUpdated(withUid(uid), withUserEmail(email),
                withPersonalPhoneId(personalPhoneId));

        OrderStatusUpdatedEvent eventWithSameUid = EventFactory.orderStatusUpdated(withUid(uid),
                withUserEmail(anotherEmail), withPersonalPhoneId(anotherPersonalPhoneId));


        restriction.done(event,
                EmptyProcessResult.getInstance(),
                triggersFactory.createDefaultPromoTriggerEventData(PROMO),
                EventHandleMode.NORMAL,
                null
        );

        assertFalse(restriction.fitCondition(event, triggersFactory.createDefaultPromoTriggerEventData(PROMO),
                new TriggerDataCache()).isMatched());
        assertFalse(restriction.fitCondition(eventWithSameUid,
                triggersFactory.createDefaultPromoTriggerEventData(PROMO), new TriggerDataCache()).isMatched());
    }

    @Test
    public void shouldBindNotNormalizedPhoneNumber() {
        transactionTemplate.execute((t) -> refreshableBindingService.bind(
                DEFAULT_UID,
                "test",
                RefreshableBindingPredicate.adapt(b -> false)
        ));
    }

    @Test
    public void testFitConditionMoreThanOneRecord() {
        long uid = 123L;

        long triggerId = triggersFactory
                .createOrderStatusUpdatedTriggerForCoin(
                        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()),
                        orderRestriction(OrderStatusPredicate.DELIVERED)
                ).getId();
        String bindingKey = Long.toString(triggerId);

        transactionTemplate.execute(t ->
                refreshableBindingService.bind(
                        uid,
                        bindingKey,
                        RefreshableBindingPredicate.adapt(b -> false)
                )
        );

        assertThat(
                refreshableBindingService.getBindings(uid)
                        .keySet(),
                contains(bindingKey)
        );
    }

    @Test
    public void testFitConditionNullParams() {
        ActionOnceRestriction restriction = actionOnceRestrictionFactory.create(1L, null);

        long uid = 123L;
        String email = "fonar101@yandex.ru";
        String personalPhoneId = "+71234567890_id";

        OrderStatusUpdatedEvent eventWithNullPhone = EventFactory.orderStatusUpdated(withUid(uid), withUserEmail(email),
                withPersonalPhoneId(null));

        OrderStatusUpdatedEvent eventWithNullMail = EventFactory.orderStatusUpdated(withUid(uid), withUserEmail(null),
                withPersonalPhoneId(personalPhoneId));

        assertTrue(restriction.fitCondition(eventWithNullPhone,
                triggersFactory.createDefaultPromoTriggerEventData(PROMO), new TriggerDataCache()).isMatched());
        assertTrue(restriction.fitCondition(eventWithNullMail, triggersFactory.createDefaultPromoTriggerEventData(PROMO), new TriggerDataCache()).isMatched());
    }
}
