package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.loyalty.api.model.StageDetails;
import ru.yandex.market.loyalty.back.controller.model.GrowingCashbackStateResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.action.PromoActionContainer;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.perks.StaticPerkService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.loyalty.core.model.action.PromoActionParameterName.STATIC_PERK_NAME;
import static ru.yandex.market.loyalty.core.model.action.PromoActionType.STATIC_PERK_ADDITION_ACTION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_CREATION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_DELIVERED;

@TestFor(GrowingCashbackController.class)
public class GrowingCashbackControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    private StaticPerkService staticPerkService;
    @Autowired
    private PromoManager promoManager;
    private final AtomicInteger uidCounter = new AtomicInteger(1);


    @Before
    public void setUp() throws Exception {
        prepareGrowingCashbackPromos();
    }

    @Test
    public void shouldReturnAllStagesNotStarted() throws Exception {
        final int USER_ID = uidCounter.getAndIncrement();
        String responseJson = mockMvc.perform(get("/growing/details")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("uid", String.valueOf(USER_ID))
        ).andReturn().getResponse().getContentAsString();
        GrowingCashbackStateResponse growingCashbackStateResponse =
                objectMapper.readValue(responseJson,
                        GrowingCashbackStateResponse.class);
        List<StageDetails> stages = growingCashbackStateResponse.getStages();
        assertThat(stages, hasSize(3));
        assertThat(stages, everyItem(
                hasProperty("stage", equalTo(StageDetails.Stage.NOT_STARTED))
        ));
        Integer lastPriority = Integer.MIN_VALUE;
        for (StageDetails stage : stages) {
            assertThat(stage.getPriority(), greaterThan(lastPriority));
            lastPriority = stage.getPriority();
        }
    }

    @Test
    public void shouldReturnAllStagesFinished() throws Exception {
        final int USER_ID = uidCounter.getAndIncrement();
        staticPerkService.providePerkToUser(USER_ID, "perk1");
        staticPerkService.providePerkToUser(USER_ID, "perk1_1");
        staticPerkService.providePerkToUser(USER_ID, "perk2");
        staticPerkService.providePerkToUser(USER_ID, "perk2_2");
        staticPerkService.providePerkToUser(USER_ID, "perk3");
        staticPerkService.providePerkToUser(USER_ID, "perk3_3");

        String responseJson = mockMvc.perform(get("/growing/details")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("uid", String.valueOf(USER_ID))
        ).andReturn().getResponse().getContentAsString();

        GrowingCashbackStateResponse growingCashbackStateResponse =
                objectMapper.readValue(responseJson,
                        GrowingCashbackStateResponse.class);
        List<StageDetails> stages = growingCashbackStateResponse.getStages();
        assertThat(stages, hasSize(3));
        assertThat(stages, everyItem(
                hasProperty("stage", equalTo(StageDetails.Stage.FINISHED))
        ));
        Integer lastPriority = Integer.MIN_VALUE;
        for (StageDetails stage : stages) {
            assertThat(stage.getPriority(), greaterThan(lastPriority));
            lastPriority = stage.getPriority();
        }
    }

    @Test
    public void shouldReturnAllStagesInProgress() throws Exception {
        final int USER_ID = uidCounter.getAndIncrement();
        staticPerkService.providePerkToUser(USER_ID, "perk1");
        staticPerkService.providePerkToUser(USER_ID, "perk2");
        staticPerkService.providePerkToUser(USER_ID, "perk3");

        String responseJson = mockMvc.perform(get("/growing/details")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("uid", String.valueOf(USER_ID))
        ).andReturn().getResponse().getContentAsString();

        GrowingCashbackStateResponse growingCashbackStateResponse =
                objectMapper.readValue(responseJson,
                        GrowingCashbackStateResponse.class);
        List<StageDetails> stages = growingCashbackStateResponse.getStages();
        assertThat(stages, hasSize(3));
        assertThat(stages, everyItem(
                hasProperty("stage", equalTo(StageDetails.Stage.IN_PROGRESS))
        ));
        Integer lastPriority = Integer.MIN_VALUE;
        for (StageDetails stage : stages) {
            assertThat(stage.getPriority(), greaterThan(lastPriority));
            lastPriority = stage.getPriority();
        }
    }

    private void prepareGrowingCashbackPromos() {
        Promo promo1 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.TEN,
                        CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(1000))
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                        .withSingleParam(STATIC_PERK_NAME, "perk1"))
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                        .withSingleParam(STATIC_PERK_NAME, "perk1_1"))
                .setPriority(-98)
        );
        Promo promo2 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.TEN,
                        CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(1000))
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                        .withSingleParam(STATIC_PERK_NAME, "perk2"))
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                        .withSingleParam(STATIC_PERK_NAME, "perk2_2"))
                .setPriority(-99)
        );
        Promo promo3 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.TEN,
                        CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(1000))
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                        .withSingleParam(STATIC_PERK_NAME, "perk3"))
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                        .withSingleParam(STATIC_PERK_NAME, "perk3_3"))
                .setPriority(-100)
        );
        configurationService.set(ConfigurationService.GROWING_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.GROWING_CASHBACK_PROMO_KEYS,
                String.join(";", promo1.getPromoKey(), promo2.getPromoKey(), promo3.getPromoKey()));
        reloadPromoCache();
    }
}
