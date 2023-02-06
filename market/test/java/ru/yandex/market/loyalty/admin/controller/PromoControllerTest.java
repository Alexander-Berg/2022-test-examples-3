package ru.yandex.market.loyalty.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.loyalty.admin.config.AuthorizationContext;
import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.controller.dto.CouponDto;
import ru.yandex.market.loyalty.admin.controller.dto.CouponPromoDto;
import ru.yandex.market.loyalty.admin.controller.dto.ExpirationPolicyDto;
import ru.yandex.market.loyalty.admin.controller.dto.HistoryStatus;
import ru.yandex.market.loyalty.admin.controller.dto.IdObject;
import ru.yandex.market.loyalty.admin.controller.dto.PromoDto;
import ru.yandex.market.loyalty.admin.controller.dto.PromoHistoryDto;
import ru.yandex.market.loyalty.admin.controller.dto.UsageRestrictionsDto.FilterRestriction;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.*;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.core.model.SortedResponse;
import ru.yandex.market.loyalty.core.model.accounting.Account;
import ru.yandex.market.loyalty.core.model.budgeting.AddedBudgetAudit;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.rule.FilterRule;
import ru.yandex.market.loyalty.core.rule.RuleFactory;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.budgeting.AddedBudgetAuditService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.TEST_TICKET_OK;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_PLATFORM;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.COUPON_EXPIRY_DAYS_QUANTITY;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.CLIENT_PLATFORM_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.SINGLE_USE_COUPON_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.WITHOUT_SHOP_DISCOUNT_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.ANOTHER_COUPON_CODE;
import static ru.yandex.market.loyalty.test.SameCollection.sameCollectionInAnyOrder;

/**
 * @author dinyat
 * 16/06/2017
 */
@TestFor(PromoController.class)
public class PromoControllerTest extends MarketLoyaltyAdminMockedDbTest {

    private static final int EXPIRATION_DAYS = 10;
    private static final BigDecimal COUPON_VALUE = BigDecimal.valueOf(300);
    private static final BigDecimal CHANGED_COUPON_VALUE = COUPON_VALUE.add(BigDecimal.valueOf(100));
    private static final String COUPON_INSUFFICIENT_TOTAL_MESSAGE = "i";
    private static final String COUPON_NOT_APPLICABLE_MESSAGE = "n";
    private static final String COUPON_ALREADY_SPEND_MESSAGE = "a";
    private static final TypeReference<SortedResponse<CouponPromoDto>> SORTED_RESPONSE_TYPE_REFERENCE =
            new TypeReference<>() {
            };
    private static final TypeReference<PagedResponse<CouponPromoDto>> PAGED_RESPONSE_TYPE_REFERENCE =
            new TypeReference<>() {
            };

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private RuleFactory ruleFactory;
    @Autowired
    private AddedBudgetAuditService addedBudgetAuditService;
    @Autowired
    protected AuthorizationContext authorizationContext;

    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;
    @Autowired
    private CouponService couponService;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private DiscountUtils discountUtils;

    private static final String ALREADY_SPENT_ERROR_VALUE = "Уже потрачено";

    private static final TypeReference<List<PromoDto>> LIST_OF_PROMO_DTO = new TypeReference<>() {
    };


    @Test
    public void getAll() throws Exception {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse().setCouponCode(ANOTHER_COUPON_CODE));

        String jsonResponse = mockMvc
                .perform(get("/api/promo"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<PromoDto> result = objectMapper.readValue(jsonResponse, LIST_OF_PROMO_DTO);
        assertEquals(2, result.size());
    }

    @Test
    public void getFilteredByDescription() throws Exception {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        String description = "anotherDescription";
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setDescription(description));

        assertEquals(1, getPromoByTerm(description).getData().size());
    }

    @Test
    public void getFilteredByCoinDescription() throws Exception {
        SmartShoppingPromoBuilder coinPromoBuilder = PromoUtils.SmartShopping.defaultFixed();
        coinPromoBuilder.getCoinDescription().setDescription("интересная монета для примера");
        promoManager.createSmartShoppingPromo(coinPromoBuilder);

        String description = "для примера";
        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        assertEquals(1, getPromoByTerm(description).getData().size());
    }

    @Test
    public void getFilteredById() throws Exception {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        String promoName = "secondPromo";
        long promoId = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setName(promoName))
                .getPromoId().getId();

        List<CouponPromoDto> data = getPromoByTerm(String.valueOf(promoId)).getData();
        assertEquals(1, data.size());
        assertEquals(promoName, data.get(0).getName());
    }

    @Test
    public void addBudgetPromoNotActiveTest() throws Exception {
        Promo promo =
                promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse().setStatus(PromoStatus.INACTIVE));

        String jsonResponse = mockMvc
                .perform(put("/api/promo/" + promo.getPromoId().getId() + "/add/budget?addedBudget=100")
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long resultId = Long.parseLong(objectMapper.readValue(jsonResponse, IdObject.class).getId());
        assertEquals(promo.getPromoId().getId(), resultId);
    }

    @Test
    public void createNewPromo() throws Exception {
        IdObject result = createPromo(createPromoDto());

        Promo promo = promoService.getPromo(Long.parseLong(result.getId()));

        assertEquals(ALREADY_SPENT_ERROR_VALUE, promo.getPromoParamRequired(PromoParameterName.COUPON_ALREADY_SPENT));
    }

    @Test
    public void createPromoWithFixExpirationToEndOfPromo() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        ExpirationPolicyDto expirationPolicy = new ExpirationPolicyDto();
        expirationPolicy.setType(ExpirationPolicy.Type.TO_END_OF_PROMO);
        promoDto.getCoupon().setExpirationPolicy(expirationPolicy);
        IdObject result = createPromo(promoDto);

        CouponPromoDto promo = getPromo(Long.parseLong(result.getId()));

        assertTrue(promo.getCoupon().getExpirationPolicy().toExpirationPolicy().isToEndOfPromo());
    }

    @Test
    public void updateErrorMessages() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        CouponPromoDto promoDto = createPromoDto();
        promoDto.setCouponNotApplicableMessage("COUPON_NOT_APPLICABLE");
        promoDto.setInsufficientTotalMessage("INSUFFICIENT_TOTAL");
        promoDto.setId(promoId);

        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        Promo promo = promoService.getPromo(promoId);
        assertEquals("COUPON_NOT_APPLICABLE", promo.getPromoParamRequired(PromoParameterName.COUPON_NOT_APPLICABLE));
        assertEquals("INSUFFICIENT_TOTAL", promo.getPromoParamRequired(PromoParameterName.INSUFFICIENT_TOTAL));
        assertEquals(ALREADY_SPENT_ERROR_VALUE, promo.getPromoParamRequired(PromoParameterName.COUPON_ALREADY_SPENT));

        promoDto = getPromo(promoId);
        promoDto.setCouponAlreadySpendMessage("Ошибка");
        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        promo = promoService.getPromo(promoId);
        assertEquals("COUPON_NOT_APPLICABLE", promo.getPromoParamRequired(PromoParameterName.COUPON_NOT_APPLICABLE));
        assertEquals("INSUFFICIENT_TOTAL", promo.getPromoParamRequired(PromoParameterName.INSUFFICIENT_TOTAL));
        assertEquals("Ошибка", promo.getPromoParamRequired(PromoParameterName.COUPON_ALREADY_SPENT));
    }

    @Test
    public void createPromoWithCategories() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        ImmutableSet<Integer> categories = ImmutableSet.of(100, 200);
        promoDto.getUsageRestrictions().setCategoriesRestriction(new FilterRestriction<>(categories, false));
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        Promo promo = promoService.getPromo(promoId);
        FilterRule filterRule = ruleFactory.getPromoRule(promo.getRulesContainer(), CATEGORY_FILTER_RULE,
                discountUtils.getRulesPayload());
        assertThat(
                promo.getRulesContainer().get(filterRule.getType()).getParams(CATEGORY_ID),
                sameCollectionInAnyOrder(categories)
        );
    }

    @Test
    public void addCategoriesToExistedPromo() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        CouponPromoDto promoDto = getPromo(promoId);
        ImmutableSet<Integer> categories = ImmutableSet.of(100, 200);
        promoDto.getUsageRestrictions().setCategoriesRestriction(new FilterRestriction<>(categories, false));
        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        assertThat(
                promoService.getPromo(promoId).getRulesContainer().get(CATEGORY_FILTER_RULE).getParams(CATEGORY_ID),
                sameCollectionInAnyOrder(categories)
        );
    }

    @Test
    public void updateCategories() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getUsageRestrictions().setCategoriesRestriction(
                new FilterRestriction<>(ImmutableSet.of(100, 200), false)
        );
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        promoDto = getPromo(promoId);
        ImmutableSet<Integer> categoriesOnUpdate = ImmutableSet.of(200, 500, 600);
        promoDto.getUsageRestrictions().setCategoriesRestriction(new FilterRestriction<>(
                categoriesOnUpdate,
                false
        ));
        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        assertThat(
                promoService.getPromo(promoId).getRulesContainer().get(CATEGORY_FILTER_RULE).getParams(CATEGORY_ID),
                sameCollectionInAnyOrder(categoriesOnUpdate)
        );
    }

    @Test
    public void removeCategories() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getUsageRestrictions().setCategoriesRestriction(new FilterRestriction<>(ImmutableSet.of(100, 200),
                false));
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        promoDto = getPromo(promoId);
        promoDto.getUsageRestrictions().setCategoriesRestriction(new FilterRestriction<>(Collections.emptySet(),
                false));
        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        assertFalse(promoService.getPromo(promoId).getRulesContainer().hasRule(CATEGORY_FILTER_RULE));
    }

    @Test
    public void addMinOrderTotalIfNotSpecified() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        Promo promo = promoService.getPromo(promoId);
        assertFalse(promo.getRulesContainer().hasRule(MIN_ORDER_TOTAL_CUTTING_RULE));

        CouponPromoDto promoDto = getPromo(promoId);
        promoDto.getUsageRestrictions().setMinOrderTotal(BigDecimal.valueOf(1500));
        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        promo = promoService.getPromo(promoId);
        assertThat(
                promo.getRulesContainer().get(MIN_ORDER_TOTAL_CUTTING_RULE).getSingleParamRequired(MIN_ORDER_TOTAL),
                comparesEqualTo(BigDecimal.valueOf(1500))
        );
    }

    @Test
    public void updateMinOrderTotal() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getUsageRestrictions().setMinOrderTotal(BigDecimal.valueOf(1500));
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        Promo promo = promoService.getPromo(promoId);
        assertThat(
                promo.getRulesContainer().get(MIN_ORDER_TOTAL_CUTTING_RULE).getSingleParamRequired(MIN_ORDER_TOTAL),
                comparesEqualTo(BigDecimal.valueOf(1500))
        );

        promoDto = getPromo(promoId);
        promoDto.getUsageRestrictions().setMinOrderTotal(BigDecimal.valueOf(2000));
        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        promo = promoService.getPromo(promoId);
        assertThat(
                promo.getRulesContainer().get(MIN_ORDER_TOTAL_CUTTING_RULE).getSingleParamRequired(MIN_ORDER_TOTAL),
                comparesEqualTo(BigDecimal.valueOf(2000))
        );
    }

    @Test
    public void removeMinOrderTotal() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getUsageRestrictions().setMinOrderTotal(BigDecimal.valueOf(1500));
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        Promo promo = promoService.getPromo(promoId);
        assertThat(
                promo.getRulesContainer().get(MIN_ORDER_TOTAL_CUTTING_RULE).getSingleParamRequired(MIN_ORDER_TOTAL),
                comparesEqualTo(BigDecimal.valueOf(1500))
        );

        promoDto = getPromo(promoId);
        promoDto.getUsageRestrictions().setMinOrderTotal(null);
        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        promo = promoService.getPromo(promoId);
        assertFalse(promo.getRulesContainer().hasRule(MIN_ORDER_TOTAL_CUTTING_RULE));
    }

    @Test
    public void updateOfActivePromo() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        setActiveStatus(promoId);

        CouponPromoDto promoDto = getPromo(promoId);
        promoDto.setCouponAlreadySpendMessage("Ошибка");
        assertEquals(new IdObject(promoId), updatePromo(promoDto));

        Promo promo = promoService.getPromo(promoId);
        assertEquals("Ошибка", promo.getPromoParamRequired(PromoParameterName.COUPON_ALREADY_SPENT));
    }

    @Test
    public void updateExpirationDays() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        CouponPromoDto promoDto = getPromo(promoId);
        assertEquals(EXPIRATION_DAYS, promoDto.getCoupon().getExpirationPolicy().getParam().intValue());

        promoDto.getCoupon().getExpirationPolicy().setParam(5);

        updatePromo(promoDto);
        Promo promo = promoService.getPromo(promoId);
        assertEquals(
                5,
                promo.getRulesContainer().get(SINGLE_USE_COUPON_RULE).getSingleParamRequired(COUPON_EXPIRY_DAYS_QUANTITY).intValue()
        );
    }

    @Test
    public void shouldUpdateBudgetThreshold() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        CouponPromoDto promoDto = getPromo(promoId);
        assertNull(promoDto.getBudgetThreshold());

        BigDecimal budgetThreshold = BigDecimal.valueOf(1000);
        promoDto.setBudgetThreshold(budgetThreshold);

        updatePromo(promoDto);

        assertThat(getPromo(promoId).getBudgetThreshold(), comparesEqualTo(budgetThreshold));

        Promo promo = promoService.getPromo(promoId);
        Account account = budgetService.getAccount(promo.getBudgetAccountId());
        assertThat(account.getBudgetThreshold(), comparesEqualTo(budgetThreshold));
    }

    @Test
    public void shouldUpdateCanBeRestoredFromReserveBudget() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        setActiveStatus(promoId);

        CouponPromoDto promoDto = getPromo(promoId);
        assertFalse(promoDto.getCanBeRestoredFromReserveBudget());

        promoDto.setCanBeRestoredFromReserveBudget(true);

        updatePromo(promoDto);

        assertTrue(getPromo(promoId).getCanBeRestoredFromReserveBudget());

        Promo promo = promoService.getPromo(promoId);
        Account account = budgetService.getAccount(promo.getBudgetAccountId());
        assertTrue(account.getCanBeRestoredFromReserveBudget());
    }

    @Test
    public void createPromoWithOnlyForItemsWithoutShopDiscount() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getUsageRestrictions().setOnlyForItemsWithoutShopDiscount(true);
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        assertTrue(promoService.getPromo(promoId).getRulesContainer().hasRule(WITHOUT_SHOP_DISCOUNT_FILTER_RULE));
    }

    @Test
    public void removeOnlyForItemsWithoutShopDiscount() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getUsageRestrictions().setOnlyForItemsWithoutShopDiscount(true);
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        promoDto = getPromo(promoId);
        promoDto.getUsageRestrictions().setOnlyForItemsWithoutShopDiscount(false);
        updatePromo(promoDto);

        assertFalse(promoService.getPromo(promoId).getRulesContainer().hasRule(WITHOUT_SHOP_DISCOUNT_FILTER_RULE));
    }

    @Test
    public void setOnlyForItemsWithoutShopDiscount() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        assertFalse(promoService.getPromo(promoId).getRulesContainer().hasRule(WITHOUT_SHOP_DISCOUNT_FILTER_RULE));

        CouponPromoDto promoDto = getPromo(promoId);
        promoDto.getUsageRestrictions().setOnlyForItemsWithoutShopDiscount(true);
        updatePromo(promoDto);

        assertTrue(promoService.getPromo(promoId).getRulesContainer().hasRule(WITHOUT_SHOP_DISCOUNT_FILTER_RULE));
    }

    @Test
    public void shouldReturnEmptyCouponPromoMessages() throws Exception {
        // этот тест появился после того как было решено не возвращать на фронт дефолтные сообщения если они
        // не заданы в базе так как эта логика в общем неправильная (в админке показаны сообщения не такие какие
        // будут отданы чекаутеру в случае ошибки)
        long promoId = Long.parseLong(createPromo(createPromoDtoWithEmptyMessages()).getId());

        assertNull(getPromo(promoId).getInsufficientTotalMessage());
        assertNull(getPromo(promoId).getCouponNotApplicableMessage());
        assertNull(getPromo(promoId).getCouponAlreadySpendMessage());


        CouponPromoDto promo;
        promo = getPromo(promoId);
        promo.setCouponNotApplicableMessage(COUPON_NOT_APPLICABLE_MESSAGE);
        promo.setCouponAlreadySpendMessage(COUPON_ALREADY_SPEND_MESSAGE);
        promo.setInsufficientTotalMessage(COUPON_INSUFFICIENT_TOTAL_MESSAGE);
        updatePromo(promo);

        assertEquals(COUPON_INSUFFICIENT_TOTAL_MESSAGE, getPromo(promoId).getInsufficientTotalMessage());
        assertEquals(COUPON_NOT_APPLICABLE_MESSAGE, getPromo(promoId).getCouponNotApplicableMessage());
        assertEquals(COUPON_ALREADY_SPEND_MESSAGE, getPromo(promoId).getCouponAlreadySpendMessage());

        promo = getPromo(promoId);
        promo.setCouponNotApplicableMessage(null);
        promo.setCouponAlreadySpendMessage(null);
        promo.setInsufficientTotalMessage(null);
        updatePromo(promo);

        assertNull(getPromo(promoId).getInsufficientTotalMessage());
        assertNull(getPromo(promoId).getCouponNotApplicableMessage());
        assertNull(getPromo(promoId).getCouponAlreadySpendMessage());
    }

    @Test
    public void shouldCreatePromoWithAllowedClientDeviceType() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getUsageRestrictions().setAllowedClientDeviceTypes(Collections.singleton(UsageClientDeviceType.APPLICATION));
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        Promo promo = promoService.getPromo(promoId);
        assertTrue(promo.getRulesContainer().hasRule(CLIENT_PLATFORM_CUTTING_RULE));
        assertEquals(UsageClientDeviceType.APPLICATION,
                promo.getRulesContainer().get(CLIENT_PLATFORM_CUTTING_RULE).getSingleParamRequired(CLIENT_PLATFORM));

        assertEquals(Collections.singleton(UsageClientDeviceType.APPLICATION),
                getPromo(promoId).getUsageRestrictions().getAllowedClientDeviceTypes());
    }

    @Test
    public void shouldAddAllowedClientDeviceType() throws Exception {
        long promoId = Long.parseLong(createPromo(createPromoDto()).getId());

        assertFalse(promoService.getPromo(promoId).getRulesContainer().hasRule(CLIENT_PLATFORM_CUTTING_RULE));

        CouponPromoDto promoDto = getPromo(promoId);
        promoDto.getUsageRestrictions().setAllowedClientDeviceTypes(Collections.singleton(UsageClientDeviceType.APPLICATION));
        updatePromo(promoDto);

        assertTrue(promoService.getPromo(promoId).getRulesContainer().hasRule(CLIENT_PLATFORM_CUTTING_RULE));
    }

    @Test
    public void shouldRemoveAllowedClientDeviceType() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getUsageRestrictions().setAllowedClientDeviceTypes(Collections.singleton(UsageClientDeviceType.APPLICATION));
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        promoDto = getPromo(promoId);
        promoDto.getUsageRestrictions().setAllowedClientDeviceTypes(null);
        updatePromo(promoDto);

        assertFalse(promoService.getPromo(promoId).getRulesContainer().hasRule(CLIENT_PLATFORM_CUTTING_RULE));
    }

    @Test
    public void shouldAllowModifyCouponAmountForInfinityCoupon() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getCoupon().setSingleUse(false);

        long promoId = Long.parseLong(createPromo(promoDto).getId());
        promoDto = getPromo(promoId);
        assertTrue(promoDto.getCoupon().isMayChangeAmount());

        promoDto.getCoupon().setAmount(CHANGED_COUPON_VALUE, CoreCouponValueType.FIXED);
        updatePromo(promoDto);

        assertThat(getPromo(promoId).getCoupon().getAmount(), comparesEqualTo(CHANGED_COUPON_VALUE));
    }

    @Test
    public void shouldAllowModifyCouponAmountForSingleUseCouponBeforeAnyEmitted() throws Exception {
        CouponPromoDto promoDto = createPromoDto();

        long promoId = Long.parseLong(createPromo(promoDto).getId());
        promoDto = getPromo(promoId);
        assertTrue(promoDto.getCoupon().isMayChangeAmount());

        promoDto.getCoupon().setAmount(CHANGED_COUPON_VALUE, CoreCouponValueType.FIXED);
        updatePromo(promoDto);

        assertThat(getPromo(promoId).getCoupon().getAmount(), comparesEqualTo(CHANGED_COUPON_VALUE));
    }

    @Test
    public void shouldForbidModifyCouponAmountForSingleUseCouponAfterAnyEmitted() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        long promoId = Long.parseLong(createPromo(promoDto).getId());
        setActiveStatus(promoId);

        couponService.createOrGetCoupon(CouponCreationRequest.builder("someKey", promoId)
                .identity(new Uid(2L))
                .build(), discountUtils.getRulesPayload());

        promoDto = getPromo(promoId);
        assertFalse(promoDto.getCoupon().isMayChangeAmount());

        promoDto.getCoupon().setAmount(CHANGED_COUPON_VALUE, CoreCouponValueType.FIXED);
        mockMvc
                .perform(put("/api/promo/update")
                        .content(objectMapper.writeValueAsString(promoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity());

        assertThat(getPromo(promoId).getCoupon().getAmount(), comparesEqualTo(COUPON_VALUE));
    }

    @Test
    public void shouldForbidUpdateOnFailCouponAndPromoDatesCheck() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        long promoId = Long.parseLong(createPromo(promoDto).getId());

        promoDto = getPromo(promoId);

        promoDto.getCoupon().getExpirationPolicy().setParam(100);
        mockMvc
                .perform(put("/api/promo/update")
                        .content(objectMapper.writeValueAsString(promoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(EXPIRATION_DAYS, getPromo(promoId).getCoupon().getExpirationPolicy().getParam().intValue());
    }

    @Test
    public void shouldForbidCreateOnFailCouponAndPromoDatesCheck() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getCoupon().getExpirationPolicy().setParam(100);
        createPromo(promoDto, status().isUnprocessableEntity());
    }

    @Test
    public void shouldCreateCouponActiveToEndOfPromo() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getCoupon().getExpirationPolicy().setType(ExpirationPolicy.Type.TO_END_OF_PROMO);
        long promoId = Long.parseLong(createPromo(promoDto).getId());
        CouponPromoDto promo = getPromo(promoId);
        assertTrue(promo.getCoupon().getExpirationPolicy().toExpirationPolicy().isToEndOfPromo());
    }

    @Test
    public void shouldRespond422OnCouponCodeDuplicate() throws Exception {
        CouponPromoDto promoDto = createInfinitePromoDto();
        createPromo(promoDto);
        createPromo(promoDto, status().isUnprocessableEntity());
    }

    @Test
    public void shouldFindPromoByInfiniteCouponCode() throws Exception {
        Long id = Long.parseLong(createPromo(createInfinitePromoDto("MARKETMAMA43")).getId());
        createPromo(createInfinitePromoDto("MARKETPAPA43"));
        assertThat(getPromoByTerm("MARKETMAMA43").getData(), hasSize(1));
        assertEquals(getPromoByTerm("MARKETMAMA43").getData().get(0).getId(), id);
    }

    @Test
    public void shouldFindPromoBySingleCouponCode() throws Exception {
        long id = Long.parseLong(createPromo(createPromoDto()).getId());
        setActiveStatus(id);
        CouponCreationRequest request = CouponCreationRequest.builder("coupon-key", id).build();
        String couponCode = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload()).getCode();

        assertThat(getPromoByTerm(couponCode).getData(), hasSize(1));
        assertThat(getPromoByTerm(couponCode).getData().get(0).getId(), equalTo(id));
    }

    @Test
    public void shouldReturnCommonData() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        promoDto.getCoupon().getExpirationPolicy().setType(ExpirationPolicy.Type.TO_END_OF_PROMO);
        long promoId = Long.parseLong(createPromo(promoDto).getId());
        PromoDto commonPromo = getCommonPromo(promoId);
        assertEquals(PromoStatus.INACTIVE, commonPromo.getStatus());
    }


    @Test
    public void shouldUpdateInternalPromoCode() throws Exception {
        String testInternalActionCode = "someInternalPromoCode";
        long savedPromoID = Long.parseLong(createPromo(createPromoDto()).getId());
        CouponPromoDto promoDto = getPromo(savedPromoID);
        promoDto.setActionCodeInternal(testInternalActionCode);

        updatePromo(promoDto);

        CouponPromoDto updatedPromo = getPromo(savedPromoID);
        assertEquals(testInternalActionCode, updatedPromo.getActionCodeInternal());
    }

    @Test
    public void shouldSetAuditInformationForAddedPromoBudgetCase() throws Exception {
        Long promoId = Long.parseLong(createPromo(createPromoDto()).getId());
        promoManager.addBudget(BigDecimal.ONE, BigDecimal.TEN, null, null, promoId,
                authorizationContext.getAppUserLogin());
        AddedBudgetAudit auditBudgetByPromoId = addedBudgetAuditService.getAddedBudgetForPromoByPromoId(promoId);
        assertEquals(BigDecimal.valueOf(1.00).setScale(2), auditBudgetByPromoId.getAddedBudget());
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), auditBudgetByPromoId.getAddedEmissionBudget());
        assertEquals(promoId, auditBudgetByPromoId.getPromoId());
        assertFalse(auditBudgetByPromoId.isReserveBudget());
        assertNotNull(auditBudgetByPromoId.getDateAdded());
        assertNotNull(auditBudgetByPromoId.getAppUserLogin());
    }

    @Test
    public void shouldSetAuditInformationForAddedReserveBudgetCase() {
        supplementaryDataLoader.createReserveIfNotExists(BigDecimal.valueOf(0));
        promoService.addReserveBudget(BigDecimal.ONE, authorizationContext.getAppUserLogin());
        List<AddedBudgetAudit> addedBudgetForReserveByBudgetVal = addedBudgetAuditService
                .getAddedBudgetForReserveByBudgetVal(BigDecimal.ONE);
        assertTrue(addedBudgetForReserveByBudgetVal.size() > 0);
        AddedBudgetAudit addedBudgets = addedBudgetForReserveByBudgetVal
                .get(0);
        assertNotNull(addedBudgets);
        BigDecimal expected = BigDecimal.valueOf(1.00).setScale(2);
        assertEquals(expected, addedBudgets.getAddedBudget());
        assertTrue(addedBudgets.isReserveBudget());
        assertNull(addedBudgets.getPromoId());
        assertNotNull(addedBudgets.getDateAdded());
        assertNotNull(addedBudgets.getAppUserLogin());
    }

    @Test
    public void shouldRespond422OnRuleFileSizeToLargeOnlyForNewPromo() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        Set<Integer> categories = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            categories.add(i);
        }
        promoDto.getUsageRestrictions().setCategoriesRestriction(new FilterRestriction<>(categories, false));
        createPromo(promoDto, status().isUnprocessableEntity());
    }

    @Test
    public void should200OkWithRuleFileSizeToLargeAndValidOldPromo() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        Set<Integer> categories = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            categories.add(i);
        }
        promoDto.getUsageRestrictions().setCategoriesRestriction(new FilterRestriction<>(categories, false));
        promoDto.setId(30677L);
        createPromo(promoDto, status().isOk());
    }

    @Test
    public void should400WithRuleFileSizeToLargeAndNotValidOldPromo() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        IdObject promoId = createPromo(promoDto);
        Set<Integer> categories = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            categories.add(i);
        }
        promoDto.getUsageRestrictions().setCategoriesRestriction(new FilterRestriction<>(categories, false));
        promoDto.setId(Long.parseLong(promoId.getId()));
        createPromo(promoDto, status().isUnprocessableEntity());
    }

    @Test
    public void checkPromoHistoryApi() throws Exception {
        CouponPromoDto promoDto = createPromoDto();
        IdObject promoId = createPromo(promoDto);
        PromoHistoryDto promoHistoryDto = objectMapper.readValue(mockMvc
                .perform(get("/api/promo/" + promoId.getId() + "/history"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PromoHistoryDto.class);
        assertEquals(promoHistoryDto.getId().toString(), promoId.getId());
        promoService.setPromoParam(Long.parseLong(promoId.getId()), PromoParameterName.ANAPLAN_ID, "#123");
        //Выставляем новй параметр
        promoHistoryDto = objectMapper.readValue(mockMvc
                .perform(get("/api/promo/" + promoId.getId() + "/history"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PromoHistoryDto.class);
        assertTrue(promoHistoryDto.getVersions().size() > 1);
        assertEquals(1, promoHistoryDto
                .getVersions()
                .get(1)
                .getParams()
                .stream()
                .filter(param -> param.getValue().equals("#123")
                        && param.getHistoryStatus().equals(HistoryStatus.ADDED))
                .count());
        //Модифицируем параметр
        promoService.setPromoParam(Long.parseLong(promoId.getId()), PromoParameterName.ANAPLAN_ID, "#321");
        promoHistoryDto = objectMapper.readValue(mockMvc
                .perform(get("/api/promo/" + promoId.getId() + "/history"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PromoHistoryDto.class);
        assertTrue(promoHistoryDto.getVersions().size() > 2);
        //Операция обновления состоит из 2х (Удаление + Добавление нового значения)
        assertEquals(2, promoHistoryDto
                .getVersions()
                .get(2)
                .getParams()
                .stream()
                .filter(param -> param.getValue().equals("#321")
                        && param.getHistoryStatus().equals(HistoryStatus.ADDED)
                        ||
                        param.getValue().equals("#123")
                                && param.getHistoryStatus().equals(HistoryStatus.DELETED)
                )
                .count());
        //Удаляем ранее выставленный параметр
        promoService.setPromoParam(Long.parseLong(promoId.getId()), PromoParameterName.ANAPLAN_ID, null);
        promoHistoryDto = objectMapper.readValue(mockMvc
                .perform(get("/api/promo/" + promoId.getId() + "/history"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PromoHistoryDto.class);
        assertTrue(promoHistoryDto.getVersions().size() > 3);
        assertEquals(1, promoHistoryDto
                .getVersions()
                .get(3)
                .getParams()
                .stream()
                .filter(param -> param.getValue().equals("#321")
                        && param.getHistoryStatus().equals(HistoryStatus.DELETED))
                .count());
        Promo promo = promoService.getPromo(Long.parseLong(promoId.getId()));
        promoService.updateStatus(promo, PromoStatus.ACTIVE);
        //Меняем Статус промки
        promoHistoryDto = objectMapper.readValue(mockMvc
                .perform(get("/api/promo/" + promoId.getId() + "/history"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PromoHistoryDto.class);
        assertTrue(promoHistoryDto.getVersions().size() > 4);
        assertEquals(1, promoHistoryDto
                .getVersions()
                .get(4)
                .getPromoFields()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals("status")
                        && entry.getValue().equals(PromoStatus.ACTIVE.getCode())
                )
                .count());
    }

    private CouponPromoDto getPromo(Long promoId) throws Exception {
        return objectMapper.readValue(mockMvc
                .perform(get("/api/promo/" + promoId))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), CouponPromoDto.class);
    }

    private PromoDto getCommonPromo(Long promoId) throws Exception {
        return objectMapper.readValue(mockMvc
                .perform(get("/api/promo/common/" + promoId))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PromoDto.class);
    }

    private PagedResponse<CouponPromoDto> getPromoByTerm(String term) throws Exception {
        return objectMapper.readValue(mockMvc
                .perform(get("/api/promo/paged")
                        .param("term", term)
                        .param("currentPage", "1")
                        .param("pageSize", "5"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PAGED_RESPONSE_TYPE_REFERENCE);
    }

    private IdObject updatePromo(CouponPromoDto promoDto) throws Exception {
        return objectMapper.readValue(mockMvc
                .perform(put("/api/promo/update")
                        .content(objectMapper.writeValueAsString(promoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), IdObject.class);
    }

    private void setActiveStatus(Long promoId) throws Exception {
        objectMapper.readValue(mockMvc
                .perform(put("/api/promo/" + promoId + "/changeStatus/" + PromoStatus.ACTIVE.getCode())
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), IdObject.class);
    }

    private IdObject createPromo(CouponPromoDto couponPromoDto) throws Exception {
        String jsonResponse = createPromo(couponPromoDto, status().isOk());
        return objectMapper.readValue(jsonResponse, IdObject.class);
    }

    private String createPromo(CouponPromoDto couponPromoDto, ResultMatcher statusMatcher) throws Exception {
        return mockMvc
                .perform(put("/api/promo/create")
                        .content(objectMapper.writeValueAsString(couponPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(statusMatcher)
                .andReturn().getResponse().getContentAsString();
    }

    private static CouponPromoDto createPromoDto() {
        CouponDto couponDto = new CouponDto();
        couponDto.setInfinityCouponCode("some code");
        couponDto.setAmount(COUPON_VALUE, CoreCouponValueType.FIXED);
        couponDto.setSingleUse(true);
        ExpirationPolicyDto expirationPolicyDto = new ExpirationPolicyDto();
        expirationPolicyDto.setType(ExpirationPolicy.Type.EXPIRE_BY_DAY);
        expirationPolicyDto.setParam(EXPIRATION_DAYS);
        couponDto.setExpirationPolicy(expirationPolicyDto);

        return createPromo(couponDto);
    }

    private static CouponPromoDto createInfinitePromoDto() {
        return createInfinitePromoDto("some code");
    }

    private static CouponPromoDto createInfinitePromoDto(String code) {
        CouponDto couponDto = new CouponDto();
        couponDto.setInfinityCouponCode(code);
        couponDto.setAmount(COUPON_VALUE, CoreCouponValueType.FIXED);
        couponDto.setSingleUse(false);
        ExpirationPolicyDto expirationPolicyDto = new ExpirationPolicyDto();
        expirationPolicyDto.setType(ExpirationPolicy.Type.EXPIRE_BY_DAY);
        expirationPolicyDto.setParam(EXPIRATION_DAYS);
        couponDto.setExpirationPolicy(expirationPolicyDto);

        return createPromo(couponDto);
    }

    private static CouponPromoDto createPromo(CouponDto couponDto) {
        CouponPromoDto promoDto = new CouponPromoDto();
        promoDto.setCoupon(couponDto);
        promoDto.setMarketPlatform(MarketPlatform.BLUE);
        promoDto.setPromoSubType(PromoSubType.COUPON);
        promoDto.setName("some name");
        promoDto.setStatus(PromoStatus.INACTIVE);
        promoDto.setDescription("bla bla");
        promoDto.setTicketNumber(TEST_TICKET_OK);
        promoDto.setCurrentBudget(BigDecimal.valueOf(3000));
        promoDto.setCurrentEmissionBudget(BigDecimal.valueOf(3000));
        promoDto.setStartDate(Date.from(LocalDate.of(2017, 10, 10).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        promoDto.setEndDate(Date.from(LocalDate.of(3000, 10, 14).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        promoDto.setEmissionDateFrom(Date.from(LocalDate.of(2017, 10, 10).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        promoDto.setEmissionDateTo(Date.from(LocalDate.of(3000, 10, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        promoDto.setCouponAlreadySpendMessage(ALREADY_SPENT_ERROR_VALUE);
        promoDto.setCanBeRestoredFromReserveBudget(false);
        promoDto.setConversion(BigDecimal.valueOf(100));
        return promoDto;
    }

    private static CouponPromoDto createPromoDtoWithEmptyMessages() {
        CouponPromoDto couponPromoDto = createPromoDto();
        couponPromoDto.setInsufficientTotalMessage(null);
        couponPromoDto.setCouponAlreadySpendMessage(null);
        couponPromoDto.setCouponNotApplicableMessage(null);
        return couponPromoDto;
    }

}
