package ru.yandex.market.loyalty.admin.trigger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.trigger.restrictions.AmountRangeDto;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate.EFFECTIVELY_PROCESSING;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.loyalUserRestriction;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;

public class TriggerLoyalUserRestrictionTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoService promoService;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;

    private long orderIdCounter = 0;

    @Test
    public void shouldNotEmmitCoinForUserWithLessThanTargetDeliveredOrders() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                loyalUserRestriction(
                        new AmountRangeDto(BigDecimal.valueOf(2), null),
                        AmountRangeDto.empty()
                )
        );

        processOrder(OrderStatus.DELIVERED);
        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldEmmitCoinForUserWithMoreThanTargetDeliveredOrders() {
        createOrdersWithCoins(2, 0);

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                loyalUserRestriction(
                        new AmountRangeDto(BigDecimal.valueOf(2), null),
                        AmountRangeDto.empty()
                )
        );

        processOrder(OrderStatus.DELIVERED);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotEmmitCoinForUserWithMoreThanTargetDeliveredOrders() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                loyalUserRestriction(
                        new AmountRangeDto(null, BigDecimal.valueOf(3)),
                        AmountRangeDto.empty()
                )
        );

        createOrdersWithCoins(5, 0);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.valueOf(3)))
        );
    }

    @Test
    public void shouldNotEmmitCoinForUserWithMoreThanTargetCanceledOrders() {
        createOrdersWithCoins(10, 3);

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                loyalUserRestriction(
                        new AmountRangeDto(BigDecimal.valueOf(5), null),
                        new AmountRangeDto(null, BigDecimal.valueOf(0.1))
                )
        );

        processOrder(OrderStatus.DELIVERED);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldEmmitCoinForUserWithoutCanceledOrders() {
        createOrdersWithCoins(10, 0);

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                loyalUserRestriction(
                        new AmountRangeDto(BigDecimal.valueOf(5), null),
                        new AmountRangeDto(null, BigDecimal.valueOf(0.1))
                )
        );

        processOrder(OrderStatus.DELIVERED);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    private void createOrdersWithCoins(int delivered, int canceled) {
        Promo firstPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.valueOf(100))
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                firstPromo,
                orderRestriction(EFFECTIVELY_PROCESSING)
        );

        Promo secondPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.valueOf(100))
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                secondPromo,
                orderRestriction(EFFECTIVELY_PROCESSING)
        );

        for (int i = 0; i < delivered; i++) {
            processOrder(OrderStatus.DELIVERED);
        }
        for (int i = 0; i < canceled; i++) {
            processOrder(OrderStatus.CANCELLED);
        }

        promoService.updateStatus(firstPromo, PromoStatus.INACTIVE);
        promoService.updateStatus(secondPromo, PromoStatus.INACTIVE);
    }

    private void processOrder(OrderStatus finalStatus) {
        long orderId = orderIdCounter++;

        processEvent(OrderStatus.PENDING, ORDER_STATUS_UPDATED, orderId);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
        processEvent(finalStatus, ORDER_STATUS_UPDATED, orderId);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
    }

}
