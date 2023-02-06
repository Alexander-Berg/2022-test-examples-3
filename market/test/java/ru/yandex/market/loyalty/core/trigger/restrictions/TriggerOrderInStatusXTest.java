package ru.yandex.market.loyalty.core.trigger.restrictions;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType.RANDOM_TRIGGERS;
import static ru.yandex.market.loyalty.core.utils.EventFactory.orderStatusUpdated;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withOrderId;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withPaymentType;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;

public class TriggerOrderInStatusXTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;

    private Promo promo;

    @Before
    public void init() {
        promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );
    }

    @Test
    public void shouldCreateCoinWhenStatusIsDeliver() {
        triggerEventQueueService.addEventToQueue(orderStatusUpdated());
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldCreateCoinOnNextOrderIfAtFirstPromoIsDisabled() {
        promoService.updateStatus(promoService.getPromo(promo.getId()), PromoStatus.INACTIVE);
        triggerEventQueueService.addEventToQueue(orderStatusUpdated());
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );

        promoService.updateStatus(promoService.getPromo(promo.getId()), PromoStatus.ACTIVE);
        triggerEventQueueService.addEventToQueue(orderStatusUpdated());
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }


    @Test
    public void shouldCreateCoinOnPrepaidProcessing() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );
        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withPaymentType(PaymentType.PREPAID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotCreateCoinOnPostpaidProcessing() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );
        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withPaymentType(PaymentType.POSTPAID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldCreateOnlyOneCoinForProcessingAndPendingStatusesInEffectivelyProcessing() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );

        long orderId = 123L;
        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withOrderId(orderId)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withOrderId(orderId)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getSpentEmissionBudget(),
                comparesEqualTo(BigDecimal.ONE)
        );
    }

    @Test
    public void shouldCreateOnlyOneCoinForProcessingAndPendingStatusesInProcessingPrepaid() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        long orderId = 123L;
        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withOrderId(orderId),
                withPaymentType(PaymentType.PREPAID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withOrderId(orderId),
                withPaymentType(PaymentType.PREPAID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getSpentEmissionBudget(),
                comparesEqualTo(BigDecimal.ONE)
        );
    }

    @Test
    public void shouldCreateTwoCoinsForProcessingAndPendingStatusesForTwoTriggers() {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo1, orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );

        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo2, orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        long orderId = 123L;
        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withOrderId(orderId),
                withPaymentType(PaymentType.PREPAID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withOrderId(orderId),
                withPaymentType(PaymentType.PREPAID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo1.getId()).getSpentEmissionBudget(),
                comparesEqualTo(BigDecimal.ONE)
        );

        assertThat(
                promoService.getPromo(promo2.getId()).getSpentEmissionBudget(),
                comparesEqualTo(BigDecimal.ONE)
        );
    }

    @Test
    public void shouldOneRandomCoinForProcessingAndPendingStatusesForTwoTriggers() {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo1, RANDOM_TRIGGERS, orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );

        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo2, RANDOM_TRIGGERS, orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        long orderId = 123L;
        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withOrderId(orderId),
                withPaymentType(PaymentType.PREPAID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withOrderId(orderId),
                withPaymentType(PaymentType.PREPAID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo1.getId()).getSpentEmissionBudget().add(promoService.getPromo(promo2.getId()).getSpentEmissionBudget()),
                comparesEqualTo(BigDecimal.ONE)
        );
    }
}
