package ru.yandex.market.loyalty.admin.promo.restrictions;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.config.qualifier.Tags;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.tags.TagsMatchResponse;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.ENABLE_USER_TAGS;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class UserSegmentRestrictionTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    @Tags
    @Autowired
    public RestTemplate tagsTemplate;
    @Blackbox
    @Autowired
    public RestTemplate blackBoxTemplate;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    private DiscountService discountService;

    @Before
    public void setUp() throws Exception {
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        configurationService.set(ENABLE_USER_TAGS, true);

        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(100))
                .addCashbackRule(RuleType.ALLOWED_SEGMENT_CUTTING_RULE, RuleParameterName.SEGMENT, "tag_1")
                .setName("Test 1")
                .setPromoKey("faskjflahfleiawhf")
                .setStartDate(java.sql.Date.from(clock.instant()))
                .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setCampaignName("test_1")
                .setEmissionBudget(BigDecimal.valueOf(100000))
                .setBudget(BigDecimal.valueOf(100000))
        );

        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(200))
                .addCashbackRule(RuleType.RESTRICTED_SEGMENT_CUTTING_RULE, RuleParameterName.SEGMENT, "tag_1")
                .setName("Test 2")
                .setPromoKey("faskjflahfleiawhfbcxv")
                .setStartDate(java.sql.Date.from(clock.instant()))
                .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setCampaignName("test_2")
                .setEmissionBudget(BigDecimal.valueOf(100000))
                .setBudget(BigDecimal.valueOf(100000))
        );
        deferredMetaTransactionService.consumeBatchOfTransactions(10);
        reloadPromoCache();
    }

    @Test
    public void shouldEmitCashbackByPromoWithTag() {
        when(tagsTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.of(Optional.of(new TagsMatchResponse(List.of("tag_1")))));
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId("1")
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(3000),
                                quantity(1)
                        )
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build())
                .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID).buildOperationContext())
                .build();
        MultiCartWithBundlesDiscountResponse response = spendRequest(request);
        assertThat(response.getCashback().getEmit().getAmount(), Matchers.comparesEqualTo(BigDecimal.valueOf(100)));
    }

    @Test
    public void shouldEmitCashbackByPromoWithoutTag() {
        when(tagsTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.of(Optional.of(new TagsMatchResponse(List.of("tag_2")))));
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId("1")
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(3000),
                                quantity(1)
                        )
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build())
                .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID).buildOperationContext())
                .build();
        MultiCartWithBundlesDiscountResponse response = spendRequest(request);
        assertThat(response.getCashback().getEmit().getAmount(), Matchers.comparesEqualTo(BigDecimal.valueOf(200)));
    }

    private MultiCartWithBundlesDiscountResponse spendRequest(MultiCartWithBundlesDiscountRequest discountRequest) {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        return discountService.spendDiscount(discountRequest, applicabilityPolicy, "");
    }
}
