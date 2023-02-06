package ru.yandex.market.loyalty.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.CouponInfoDto;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.UserAccountCouponInfoDto;
import ru.yandex.market.loyalty.api.model.events.ForceCreateCouponEventDto;
import ru.yandex.market.loyalty.api.model.events.LoyaltyEvent;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.api.model.identity.YandexUid;
import ru.yandex.market.loyalty.back.config.MarketLoyaltyBack;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.IdentityService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.COUPON_EXPIRE_ON_END_OF_PROMO;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.COUPON_EXPIRY_DAYS_QUANTITY;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.SINGLE_USE_COUPON_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.WITHOUT_SHOP_DISCOUNT_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.withYandexUidBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author maratik
 */
@TestFor(CouponController.class)
public class CouponControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final String SOME_UID_1 = "12321";
    private static final String EMAIL = "1234";
    private static final String CLIENT_UNIQUE_KEY = "ORDER_ID_5";
    @Autowired
    private CouponControllerClient couponControllerClient;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private PromoService promoService;

    @Autowired
    @MarketLoyaltyBack
    RestTemplate restTemplate;
    @Autowired
    @MarketLoyaltyBack
    ObjectMapper objectMapper;

    private long promoId;

    @Before
    public void init() {
        promoId = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()).getId();
    }

    @Test
    public void testCreateOrGetCouponByPromoId() {
        CouponDto coupon = couponControllerClient.getOrCreateCoupon("3123", Identity.Type.UID, "123456", promoId);
        assertEquals(CouponStatus.INACTIVE, coupon.getStatus());
        assertNotNull(coupon.getCode());
    }

    @Test
    public void testActivateCoupon() {
        String couponCode = couponControllerClient.getOrCreateCoupon("5555", Identity.Type.UUID, "1234567", promoId).
                getCode();

        CouponDto coupon = couponControllerClient.activateCoupon(couponCode);
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
        assertEquals(couponCode, coupon.getCode());
    }

    @Test
    public void testCreateOrGetCouponWithNullIdentity() {
        CouponDto coupon = couponControllerClient.getOrCreateCoupon("3137", null, null, promoId);
        assertEquals(CouponStatus.INACTIVE, coupon.getStatus());
        assertNotEquals("", coupon.getCode());

        coupon = couponControllerClient.getOrCreateCoupon("31378", Identity.Type.UID, null, promoId);
        assertEquals(CouponStatus.INACTIVE, coupon.getStatus());
        assertNotNull(coupon.getCode());

        coupon = couponControllerClient.getOrCreateCoupon("313789", null, "12321", promoId);
        assertEquals(CouponStatus.INACTIVE, coupon.getStatus());
        assertNotNull(coupon.getCode());
    }

    @Test
    public void testCreateOrGetCouponEquality() {
        CouponDto coupon1 = couponControllerClient.getOrCreateCoupon("3151", Identity.Type.UID, "12321", promoId);

        CouponDto coupon2 = couponControllerClient.getOrCreateCoupon("3151", Identity.Type.YANDEX_UID, "100500",
                promoId);

        assertEquals(coupon1.getCode(), coupon2.getCode());
    }

    @Test
    public void testCreateOrGetCouponInequality() {
        CouponDto coupon1 = couponControllerClient.getOrCreateCoupon("3132", Identity.Type.UID, "12321", promoId);

        CouponDto coupon2 = couponControllerClient.getOrCreateCoupon("3133", Identity.Type.UID, "12321", promoId);

        assertNotEquals(coupon1.getCode(), coupon2.getCode());
    }

    @Test
    public void testEmissionBudgetExceeded() {
        BigDecimal emissionBudget = BigDecimal.valueOf(3);
        promoId = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEmissionBudget(emissionBudget)
        ).getId();

        couponControllerClient.getOrCreateCoupon("3132", Identity.Type.UID, "12321", promoId);
        couponControllerClient.getOrCreateCoupon("3133", Identity.Type.UID, "12322", promoId);
        couponControllerClient.getOrCreateCoupon("3134", Identity.Type.UID, "12323", promoId);

        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                couponControllerClient.getOrCreateCoupon("3135", Identity.Type.UID, "12324", promoId)
        ).getModel();

        assertEquals(MarketLoyaltyErrorCode.EMISSION_BUDGET_EXCEEDED.name(), error.getCode());
        assertEquals(MarketLoyaltyErrorCode.EMISSION_BUDGET_EXCEEDED.getDefaultDescription(), error.getMessage());
    }

    @Test
    public void testGetCouponsByUid() throws Exception {
        couponControllerClient.getOrCreateCoupon("3132", Identity.Type.UID, SOME_UID_1, promoId);
        CouponDto coupon;
        coupon = couponControllerClient.getOrCreateCoupon("3133", Identity.Type.UID, SOME_UID_1, promoId);
        couponControllerClient.activateCoupon(coupon.getCode());
        coupon = couponControllerClient.getOrCreateCoupon("3134", Identity.Type.UID, SOME_UID_1, promoId);
        couponControllerClient.activateCoupon(coupon.getCode());

        List<UserAccountCouponInfoDto> activeCoupons = couponControllerClient.requestCouponsByUid(Identity.Type.UID,
                SOME_UID_1);
        assertThat(activeCoupons, hasSize(2));
    }

    @Test
    public void testGetClientUniqueKey() throws Exception {
        Identity<?> identity = Identity.Type.UID.buildIdentity(SOME_UID_1);
        identityService.getOrCreate(identity);
        triggersFactory.createForceCreateCouponTrigger(promoService.getPromo(promoId));
        ForceCreateCouponEventDto request = generateForceCreateCouponEventDto(promoId, CLIENT_UNIQUE_KEY, EMAIL);
        processWithForceCouponCreation(request);

        List<UserAccountCouponInfoDto> couponList = couponService.getCouponsByIdentity(Identity.Type.UID, SOME_UID_1,
                discountUtils.getRulesPayload());
        assertEquals(CLIENT_UNIQUE_KEY, couponList.get(0).getClientUniqueKey());
    }

    @Test
    public void testCouponCreationTime() throws Exception {
        Instant beforeCreateTime = Instant.now().minusSeconds(2);
        Identity<?> identity = Identity.Type.UID.buildIdentity(SOME_UID_1);
        identityService.getOrCreate(identity);
        triggersFactory.createForceCreateCouponTrigger(promoService.getPromo(promoId));
        ForceCreateCouponEventDto request = generateForceCreateCouponEventDto(promoId, CLIENT_UNIQUE_KEY, EMAIL);
        processWithForceCouponCreation(request);
        List<UserAccountCouponInfoDto> couponList = couponService.getCouponsByIdentity(Identity.Type.UID, SOME_UID_1,
                discountUtils.getRulesPayload());
        Instant afterCreateTime = Instant.now().plusSeconds(2);
        assertTrue(beforeCreateTime.isBefore(couponList.get(0).getCreationTime().toInstant()));
        assertTrue(afterCreateTime.isAfter(couponList.get(0).getCreationTime().toInstant()));
    }

    @Test
    public void testGetCouponsByUnknownUid() throws Exception {
        couponControllerClient.requestCouponsByUid(Identity.Type.UID.getCode(), SOME_UID_1, status().isNotFound());
    }

    @Test
    public void testGetCouponsByUnknownUserType() throws Exception {
        couponControllerClient.requestCouponsByUid("unknown", SOME_UID_1, status().isNotFound());
    }

    @Test
    public void testGetCouponInfo() {
        String name = "CouponName";
        BigDecimal couponValue = BigDecimal.valueOf(300.00);
        BigDecimal budget = BigDecimal.valueOf(1000.01);
        BigDecimal emissionBudget = BigDecimal.valueOf(2000.02);
        BigDecimal minOrderTotal = BigDecimal.valueOf(20.03);
        int expirationDays = 90;
        CoreMarketPlatform marketPlatform = CoreMarketPlatform.BLUE;
        Date startDate = new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime();
        Date endDate = new GregorianCalendar(2030, Calendar.FEBRUARY, 1).getTime();
        Date startEmissionDate = new GregorianCalendar(2016, Calendar.FEBRUARY, 1).getTime();
        Date endEmissionDate = new GregorianCalendar(2029, Calendar.FEBRUARY, 1).getTime();

        ImmutableSet<Integer> categoryIds = ImmutableSet.of(100, 400);

        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setName(name)
                        .setCouponValue(couponValue, CoreCouponValueType.FIXED)
                        .setBudget(budget)
                        .setEmissionBudget(emissionBudget)
                        .setExpiration(ExpirationPolicy.expireByDays(expirationDays))
                        .addPromoRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL,
                                Collections.singleton(minOrderTotal))
                        .setPlatform(marketPlatform)
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .setStartEmissionDate(startEmissionDate)
                        .setEndEmissionDate(endEmissionDate)
                        .addPromoRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categoryIds)
                        .addPromoRule(WITHOUT_SHOP_DISCOUNT_FILTER_RULE)
        );

        YandexUid identity = new YandexUid("1234");

        CouponDto coupon = couponControllerClient.getOrCreateCoupon("TEST_COUPON", identity.getType(),
                identity.getValue(), promo.getId());

        DiscountRequestBuilder request = DiscountRequestBuilder
                .builder(orderRequestBuilder()
                        .withOrderItem(categoryId(categoryIds.iterator().next()))
                        .build()
                )
                .withPlatform(marketPlatform.getApiPlatform())
                .withOperationContext(withYandexUidBuilder(identity.getStringValue()).buildOperationContextDto());

        couponControllerClient.activateCoupon(coupon.getCode());

        String revertToken =
                marketLoyaltyClient.spendDiscount(request.withCoupon(coupon.getCode()).build())
                        .getOrders().get(0).getItems().get(0).getPromos().get(0).getDiscountToken();

        marketLoyaltyClient.revertDiscount(Collections.singleton(revertToken));
        marketLoyaltyClient.spendDiscount(request.withCoupon(coupon.getCode()).build());

        CouponInfoDto infoDto = marketLoyaltyClient.getCouponInfo(coupon.getCode());

        assertEquals(infoDto.getCode(), coupon.getCode());
        assertEquals(infoDto.getStatus(), CouponStatus.USED);
        assertThat(infoDto.getCouponValue(), comparesEqualTo(couponValue));

        assertEquals(infoDto.getPromoId(), promo.getId().longValue());
        assertEquals(infoDto.getPromoName(), name);
        assertEquals(infoDto.getPromoStatus(), PromoStatus.ACTIVE);
        assertEquals(infoDto.getMarketPlatform(), marketPlatform.getApiPlatform());
        assertThat(infoDto.getStartDateTime(), comparesEqualTo(startDate));
        assertThat(infoDto.getEndDateTime(), comparesEqualTo(endDate));
        assertThat(infoDto.getStartEmissionDateTime(), comparesEqualTo(startEmissionDate));
        assertThat(infoDto.getEndEmissionDateTime(), comparesEqualTo(endEmissionDate));

        List<CouponInfoDto.CouponRestrictionDto> restrictions = infoDto.getCouponRestrictions();
        assertEquals(restrictions.size(), 5);
        assertExists(restrictions,
                MIN_ORDER_TOTAL_CUTTING_RULE.getBeanName(),
                MIN_ORDER_TOTAL_CUTTING_RULE.getDescription(),
                new RestrictionParam(MIN_ORDER_TOTAL.getCode(), minOrderTotal.toString())
        );
        assertExists(restrictions,
                CATEGORY_FILTER_RULE.getBeanName(),
                CATEGORY_FILTER_RULE.getDescription(),
                new RestrictionParam(CATEGORY_ID.getCode(),
                        categoryIds.stream().map(String::valueOf).toArray(String[]::new))
        );
        assertExists(restrictions,
                SINGLE_USE_COUPON_RULE.getBeanName(),
                SINGLE_USE_COUPON_RULE.getDescription(),
                new RestrictionParam(COUPON_EXPIRY_DAYS_QUANTITY.getCode(), Integer.toString(expirationDays)),
                new RestrictionParam(COUPON_EXPIRE_ON_END_OF_PROMO.getCode(), Boolean.toString(false))
        );
        assertExists(restrictions,
                WITHOUT_SHOP_DISCOUNT_FILTER_RULE.getBeanName(),
                WITHOUT_SHOP_DISCOUNT_FILTER_RULE.getDescription()
        );

        assertEquals(infoDto.getCouponHistories().size(), 5);
        infoDto.getCouponHistories().forEach(
                history -> {
                    assertNotNull(history.getSourceKey());
                    assertNotNull(history.getActionType());
                    assertNotNull(history.getDateTime());
                }
        );

        infoDto.getCouponHistories().stream()
                .filter(history -> history.getActionType() == DiscountHistoryRecordType.USAGE)
                .forEach(
                        history -> {
                            assertEquals(identity.getType(), history.getIdentityType());
                            assertEquals(identity.getStringValue(), history.getIdentityValue());
                            assertNotNull(history.getOrderId());
                            assertNotNull(history.getFirstName());
                            assertNotNull(history.getLastName());
                        }
                );
    }

    private static void assertExists(List<CouponInfoDto.CouponRestrictionDto> restrictions,
                                     String restrictionName, String description,
                                     RestrictionParam... params) {
        CouponInfoDto.CouponRestrictionDto current = restrictions.stream()
                .filter(restriction -> restriction.getCode().equals(restrictionName))
                .findFirst().orElseThrow(() -> new AssertionError("Restriction code = " + restrictionName + " not " +
                        "found"));

        assertEquals(description, current.getDescription());
        assertThat(
                current.getParams().entrySet()
                        .stream()
                        .map(e -> Pair.of(e.getKey(), Arrays.asList(e.getValue().split(","))))
                        .collect(Collectors.toList()),
                containsInAnyOrder(
                        Arrays.stream(params)
                                .map(p -> Matchers.<Pair<String, List<String>>>allOf(
                                        hasProperty("key", equalTo(p.name)),
                                        hasProperty("value", containsInAnyOrder(p.values))
                                ))
                                .collect(Collectors.toList())
                )
        );
    }

    private CouponDto processWithForceCouponCreation(ForceCreateCouponEventDto request) throws Exception {
        String response = mockMvc.perform(post("/event/" + LoyaltyEvent.FORCE_CREATE_COUPON + "/process")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(log())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, CouponDto.class);
    }

    private static ForceCreateCouponEventDto generateForceCreateCouponEventDto(long id, String uniqueKey,
                                                                               String email) {
        ForceCreateCouponEventDto request = new ForceCreateCouponEventDto();
        request.setEmail(email);
        request.setPromoId(id);
        request.setUniqueKey(uniqueKey);
        request.setUid(Long.parseLong(SOME_UID_1));
        return request;
    }

    private static class RestrictionParam {
        private final String name;
        private final String[] values;

        public RestrictionParam(String name, String... values) {
            this.name = name;
            this.values = values;
        }
    }
}
