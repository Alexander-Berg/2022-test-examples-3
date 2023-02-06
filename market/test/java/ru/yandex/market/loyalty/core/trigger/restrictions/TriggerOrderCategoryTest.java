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
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.FIRST_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PARENT_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withItem;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.categoryRestriction;

public class TriggerOrderCategoryTest extends MarketLoyaltyCoreMockedDbTestBase {
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
    public void shouldCreateCoinWhenCategoryInList() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, categoryRestriction(1));
        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withItem(defaultOrderItem().setCategoryId(1))));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldCreateCoinWhenCategoryInListParent() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, categoryRestriction(PARENT_CATEGORY_ID));
        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withItem(defaultOrderItem().setCategoryId(FIRST_CHILD_CATEGORY_ID))));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotCreateCoinWhenCategoryNotInList() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, categoryRestriction(1));

        triggerEventQueueService.addEventToQueue(
                EventFactory.orderStatusUpdated(withItem(defaultOrderItem().setCategoryId(2))));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotCreateCoinWhenCategoryInListBotNotSufficientTotal() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                categoryRestriction(BigDecimal.valueOf(1_000), 1)
        );
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withItem(defaultOrderItem()
                .setCategoryId(1)
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
        SetWithRelationDto<Integer> dto = new SetWithRelationDto<>();
        dto.setSetRelation(SetRelation.ALL_INCLUDED_IN_SET);
        dto.setGivenSet(ImmutableSet.of(0));
        dto.setMinTotal(BigDecimal.ZERO);
        OrderCategoryRestrictionFactory.OrderCategoryRestriction expected =
                triggerUtils.getRestrictionFactory(TriggerRestrictionType.ORDER_CATEGORY_RESTRICTION)
                        .create(null, objectMapper.writeValueAsString(dto));
        OrderCategoryRestrictionFactory.OrderCategoryRestriction actual =
                triggerUtils.getRestrictionFactory(TriggerRestrictionType.ORDER_CATEGORY_RESTRICTION)
                        .create(null, "{\"setRelation\":\"ALL_INCLUDED_IN_SET\",\"givenSet\":[0]}");
        assertThat(actual, samePropertyValuesAs(expected));
    }
}
