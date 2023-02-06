package ru.yandex.market.loyalty.core.trigger.restrictions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.TriggerMapper;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggerUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withItem;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.vendorsRestriction;

public class TriggerOrderVendorTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private TriggerUtils triggerUtils;
    @Autowired
    @TriggerMapper
    private ObjectMapper objectMapper;

    private Promo promo;

    @Before
    public void init() {
        promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
    }

    @Test
    public void shouldCreateCoinWhenVendorInList() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, vendorsRestriction(1));
        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withItem(defaultOrderItem().setVendorId(1L))));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotCreateCoinWhenVendorNotInList() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, vendorsRestriction(1));
        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withItem(defaultOrderItem().setVendorId(2L))));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotCreateCoinWhenVendorInListBotNotSufficientTotal() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                vendorsRestriction(BigDecimal.valueOf(1_000), 1)
        );

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withItem(defaultOrderItem()
                .setVendorId(1L)
                .setPrice(BigDecimal.valueOf(500))
                .setCount(BigDecimal.ONE)
        )));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldStoredDtoParserBackwardCompatible() throws JsonProcessingException {
        SetWithRelationDto<Long> dto = new SetWithRelationDto<>();
        dto.setSetRelation(SetRelation.ALL_INCLUDED_IN_SET);
        dto.setGivenSet(ImmutableSet.of(0L));
        dto.setMinTotal(BigDecimal.ZERO);
        OrderVendorRestrictionFactory.OrderVendorRestriction expected =
                triggerUtils.getRestrictionFactory(TriggerRestrictionType.ORDER_VENDOR_RESTRICTION)
                        .create(null, objectMapper.writeValueAsString(dto));
        OrderVendorRestrictionFactory.OrderVendorRestriction actual =
                triggerUtils.getRestrictionFactory(TriggerRestrictionType.ORDER_VENDOR_RESTRICTION)
                        .create(null, "{\"setRelation\":\"ALL_INCLUDED_IN_SET\",\"givenSet\":[0]}");
        assertThat(actual, samePropertyValuesAs(expected));
    }
}
