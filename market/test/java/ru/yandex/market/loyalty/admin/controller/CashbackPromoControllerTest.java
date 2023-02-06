package ru.yandex.market.loyalty.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.controller.dto.CashbackDetailsGroupDescriptorDto;
import ru.yandex.market.loyalty.admin.controller.dto.CashbackPromoDto;
import ru.yandex.market.loyalty.admin.controller.dto.IdObject;
import ru.yandex.market.loyalty.admin.controller.dto.UsageRestrictionsDto;
import ru.yandex.market.loyalty.admin.controller.dto.action.AdditionalActionsDto;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.discount.PaymentFeature;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.CashbackDetailsGroupDao;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.model.accounting.Account;
import ru.yandex.market.loyalty.core.model.accounting.AccountMatter;
import ru.yandex.market.loyalty.core.model.accounting.AccountType;
import ru.yandex.market.loyalty.core.model.action.PromoActionContainer;
import ru.yandex.market.loyalty.core.model.cashback.group.CashbackDetailsGroupDescriptorEntry;
import ru.yandex.market.loyalty.core.model.cashback.group.CashbackDetailsGroupStatus;
import ru.yandex.market.loyalty.core.model.cashback.group.QCashbackDetailsGroupDescriptorEntry;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.CashbackNominalType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.cashback.CashbackSource;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.cashback.CashbackService;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.TEST_TICKET_OK;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.TEST_TICKET_WRONG;
import static ru.yandex.market.loyalty.core.model.action.PromoActionParameterName.STATIC_PERK_NAME;
import static ru.yandex.market.loyalty.core.model.action.PromoActionType.STATIC_PERK_ADDITION_ACTION;
import static ru.yandex.market.loyalty.core.model.action.PromoActionType.STATIC_PERK_REVOCATION_ACTION;
import static ru.yandex.market.loyalty.core.model.cashback.group.CashbackDetailsGroupHealthStatus.OK;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_CREATION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_DELIVERED;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_PAID;
import static ru.yandex.market.loyalty.core.model.promo.CashbackLevelType.EXTERNAL;
import static ru.yandex.market.loyalty.core.model.promo.CashbackLevelType.ITEM;
import static ru.yandex.market.loyalty.core.model.promo.CashbackLevelType.MULTI_ORDER;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.DIRECT_PRIORITY;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.getActionsMapWithStaticPerkAddition;

@TestFor(CashbackPromoController.class)
public class CashbackPromoControllerTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PromoService promoService;
    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private CashbackDetailsGroupDao cashbackDetailsGroupDao;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private CashbackService cashbackService;

    @Test
    public void shouldCreateAndGetPromo() throws Exception {
        int percent = 5;
        CashbackPromoDto cashbackPromoDto = createPromoDto(percent);
        long createdPromoId = performCreatePromo(cashbackPromoDto);
        CashbackPromoDto downloadedPromo = performGetPromo(createdPromoId);
        assertEquals("cashback_promo_name", downloadedPromo.getName());
        assertThat(downloadedPromo.getNominal(), comparesEqualTo(BigDecimal.valueOf(percent)));
    }

    @Test
    public void failCreatePromoWithWrongTicket() throws Exception {
        CashbackPromoDto cashbackPromoDto = createPromoDto(5, null, false, "WRONG_TICKET_FORMAT");
        failCreatePromo(cashbackPromoDto);
    }

    @Test
    public void shouldNotAllowDisabledLevelAndNominalCombination() throws Exception {
        int percent = 5;
        CashbackPromoDto cashbackPromoDto = createPromoDto(percent);
        cashbackPromoDto.setNominalType(CashbackNominalType.FIXED);
        cashbackPromoDto.setLevelType(ITEM);
        failCreatePromo(cashbackPromoDto);
    }

    @Test
    public void shouldCreateAndChangeStatus() throws Exception {
        int percent = 5;
        CashbackPromoDto cashbackPromoDto = createPromoDto(percent, BigDecimal.valueOf(1_000_000));
        long promoId = performCreatePromo(cashbackPromoDto);

        changeStatus(promoId, PromoStatus.INACTIVE, HttpStatus.OK);
        assertEquals(PromoStatus.INACTIVE, promoService.getPromo(promoId).getStatus());

        changeStatus(promoId, PromoStatus.ACTIVE, HttpStatus.OK);
        assertEquals(PromoStatus.ACTIVE, promoService.getPromo(promoId).getStatus());
    }

    @Test
    public void createThenFailChangeStatusWithWrongTicketStatus() throws Exception {
        CashbackPromoDto cashbackPromoDto = createPromoDto(5, BigDecimal.valueOf(1_000_000), false, TEST_TICKET_WRONG);
        long promoId = performCreatePromo(cashbackPromoDto);

        changeStatus(promoId, PromoStatus.ACTIVE, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    public void shouldCreateThenUpdateAndGetPromo() throws Exception {
        CashbackPromoDto cashbackPromoDto = createPromoDto(3);
        Long createdPromoId = performCreatePromo(cashbackPromoDto);
        String updatedName = "updated_name";
        String updatedDescription = "updated_description";
        cashbackPromoDto.setId(createdPromoId);
        cashbackPromoDto.setName(updatedName);
        cashbackPromoDto.setDescription(updatedDescription);
        cashbackPromoDto.setNominal(BigDecimal.valueOf(4));
        Long updatedPromoId = performUpdatePromo(cashbackPromoDto);
        CashbackPromoDto downloadedPromo = performGetPromo(updatedPromoId);
        assertEquals(updatedName, downloadedPromo.getName());
        assertEquals(updatedDescription, downloadedPromo.getDescription());
        assertThat(downloadedPromo.getNominal(), comparesEqualTo(BigDecimal.valueOf(4)));
    }

    @Test
    public void shouldIncrementCashbackPromoVersion() throws Exception {
        CashbackPromoDto cashbackPromoDto = createPromoDto(3);
        final long promoId = performCreatePromo(cashbackPromoDto);
        final long versionBeforeUpdate = promoService.getPromo(promoId).getVersion();

        cashbackPromoDto.setId(promoId);
        cashbackPromoDto.setName("updated_name");
        performUpdatePromo(cashbackPromoDto);

        final long versionAfterUpdate = promoService.getPromo(promoId).getVersion();

        assertTrue(versionAfterUpdate > versionBeforeUpdate);
    }

    @Test
    public void shouldGetPerkTypes() throws Exception {
        String jsonResponse = mockMvc
                .perform(get("/api/cashback/promo/perks"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeReference<Map<PerkType, String>> mapType =
                new TypeReference<>() {
                };
        Map<PerkType, String> perks = objectMapper.readValue(jsonResponse, mapType);
        assertThat(perks, not(hasEntry(equalTo(PerkType.UNKNOWN), anything())));
        assertThat(perks.size(), equalTo(PerkType.values().length - 1));
        assertThat(perks, hasEntry(PerkType.YANDEX_CASHBACK, PerkType.YANDEX_CASHBACK.getRusDescription()));
    }

    @Test
    public void shouldGetActiveGroups() throws Exception {
        cashbackDetailsGroupDao.save(CashbackDetailsGroupDescriptorEntry.builder()
                .setName("test")
                .setTitle("название")
                .setStatus(CashbackDetailsGroupStatus.ACTIVE)
                .setHealthStatusReport(OK)
                .build());

        String jsonResponse = mockMvc
                .perform(get("/api/cashback/promo/groups"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeReference<List<CashbackDetailsGroupDescriptorDto>> mapType =
                new TypeReference<>() {
                };
        CashbackDetailsGroupDescriptorDto group = objectMapper.readValue(jsonResponse, mapType).stream()
                .filter(g -> g.getName().equals("test"))
                .findFirst()
                .orElseThrow();
        assertThat(group, allOf(
                hasProperty("name", equalTo("test")),
                hasProperty("title", equalTo("название"))
        ));
    }

    @Test
    public void shouldSaveNewGroup() throws Exception {
        mockMvc
                .perform(post("/api/cashback/promo/group")
                        .content(objectMapper.writeValueAsString(new CashbackDetailsGroupDescriptorDto("test",
                                "название")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());


        QCashbackDetailsGroupDescriptorEntry c =
                QCashbackDetailsGroupDescriptorEntry.cashbackDetailsGroupDescriptorEntry;
        CashbackDetailsGroupDescriptorEntry group = cashbackDetailsGroupDao.findOne(c.name.eq("test")).get();
        assertThat(group, allOf(
                hasProperty("name", equalTo("test")),
                hasProperty("title", equalTo("название")),
                hasProperty("status", equalTo(CashbackDetailsGroupStatus.ACTIVE))
        ));
    }

    @Test
    public void shouldNotGetInactiveGroups() throws Exception {
        cashbackDetailsGroupDao.save(CashbackDetailsGroupDescriptorEntry.builder()
                .setName("test")
                .setTitle("название")
                .setStatus(CashbackDetailsGroupStatus.INACTIVE)
                .setHealthStatusReport(OK)
                .build());

        String jsonResponse = mockMvc
                .perform(get("/api/cashback/promo/groups"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TypeReference<List<CashbackDetailsGroupDescriptorDto>> mapType =
                new TypeReference<>() {
                };
        assertThat(objectMapper.readValue(jsonResponse, mapType).stream()
                        .filter(g -> g.getName().equals("test"))
                        .count(),
                equalTo(0L)
        );
    }

    @Test
    public void storeAndGetCashbackPriority() throws Exception {
        int percent = 5;
        int frontPriority = 10;
        int backPriority = -10;
        CashbackPromoDto cashbackPromoDto = createPromoDto(percent, frontPriority);
        long promoId = performCreatePromo(cashbackPromoDto);
        // front works with reverted backend priority
        assertThat(promoService.getPromoParamValue(promoId, DIRECT_PRIORITY).orElse(null), is(backPriority));

        cashbackPromoDto = performGetPromo(promoId);
        assertNotNull(cashbackPromoDto);
        assertEquals(frontPriority, cashbackPromoDto.getPriority().intValue());
    }

    @Test
    public void should400ErrorWithRuleFileSizeToLarge() throws Exception {
        int percent = 5;
        int priority = 10;
        CashbackPromoDto cashbackPromoDto = createPromoDto(percent, priority);
        Set<Integer> categories = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            categories.add(i);
        }
        cashbackPromoDto.getUsageRestrictions().setCategoriesRestriction(new UsageRestrictionsDto.FilterRestriction<>(categories, false));
        performCreatePromo(cashbackPromoDto, status().isUnprocessableEntity());
    }

    @Test
    public void shouldCreateBudgetForCashbackPromoIfSpecified() throws Exception {
        CashbackPromoDto cashbackPromoDto = createPromoDto(3, BigDecimal.valueOf(10_000));
        long createdPromoId = performCreatePromo(cashbackPromoDto);
        final Promo createdCashbackPromo = promoDao.getPromo(createdPromoId);

        final Account emissionAccount = accountDao.getAccount(createdCashbackPromo.getBudgetEmissionAccountId());
        final Account spendingEmissionAccount =
                accountDao.getAccount(createdCashbackPromo.getSpendingEmissionAccountId());

        assertThat(createdCashbackPromo.getBudgetAccountId(), is(nullValue()));
        assertThat(createdCashbackPromo.getSpendingAccountId(), is(nullValue()));

        assertThat(
                emissionAccount,
                allOf(
                        hasProperty("type", equalTo(AccountType.ACTIVE)),
                        hasProperty("balance", comparesEqualTo(BigDecimal.valueOf(10000))),
                        hasProperty("matter", equalTo(AccountMatter.MONEY))
                )
        );
        assertThat(
                spendingEmissionAccount,
                allOf(
                        hasProperty("type", equalTo(AccountType.PASSIVE)),
                        hasProperty("balance", comparesEqualTo(BigDecimal.ZERO)),
                        hasProperty("matter", equalTo(AccountMatter.MONEY))
                )
        );

        final CashbackPromoDto createdCashbackPromoDto = performGetPromo(createdPromoId);

        assertThat(
                createdCashbackPromoDto,
                allOf(
                        hasProperty("currentBudget", is(nullValue())),
                        hasProperty("spentBudget", is(nullValue())),
                        hasProperty("currentEmissionBudget", comparesEqualTo(BigDecimal.valueOf(10000))),
                        hasProperty("spentEmissionBudget", comparesEqualTo(BigDecimal.ZERO))
                )
        );
    }

    @Test
    public void shouldAddBudgetForCashbackPromo() throws Exception {
        CashbackPromoDto cashbackPromoDto = createPromoDto(3, BigDecimal.valueOf(10000), true);
        long createdPromoId = performCreatePromo(cashbackPromoDto);
        addBudget(createdPromoId, BigDecimal.valueOf(10000));
        final Promo createdCashbackPromo = promoDao.getPromo(createdPromoId);

        assertThat(
                createdCashbackPromo,
                allOf(
                        hasProperty("currentBudget", is(nullValue())),
                        hasProperty("spentBudget", is(nullValue())),
                        hasProperty("currentEmissionBudget", comparesEqualTo(BigDecimal.valueOf(20000))),
                        hasProperty("spentEmissionBudget", comparesEqualTo(BigDecimal.ZERO))
                )
        );
        assertEquals(
                createdCashbackPromo.getPromoParamRequired(PromoParameterName.BUDGET_MODE),
                BudgetMode.ASYNC
        );
    }

    @Test
    public void shouldAddAdditionalActions() throws Exception {
        var additionPerkName = "test_perk";
        var revocationPerkName1 = "test_perk1";
        var revocationPerkName2 = "test_perk2";
        var actionsMap = getActionsMapWithStaticPerkAddition(additionPerkName, ORDER_CREATION);

        actionsMap.add(PromoActionContainer.builder(STATIC_PERK_REVOCATION_ACTION, ORDER_PAID)
                .withSingleParam(STATIC_PERK_NAME, revocationPerkName1)
                .build()
        );
        actionsMap.add(PromoActionContainer.builder(STATIC_PERK_REVOCATION_ACTION, ORDER_DELIVERED)
                .withSingleParam(STATIC_PERK_NAME, revocationPerkName2)
                .build()
        );

        var cashbackPromoDto = createPromoDto(3, BigDecimal.valueOf(10000), true, MULTI_ORDER);
        cashbackPromoDto.setAdditionalActions(new AdditionalActionsDto(actionsMap));
        long createdPromoId = performCreatePromo(cashbackPromoDto);

        var createdCashbackProps = cashbackService.getCashbackPropsByPromoId(createdPromoId);

        assertThat(createdCashbackProps.getPromoActionsMap().getAllContainers(), hasSize(3));

        var additionActions = createdCashbackProps.getPromoActionsMap().getContainers(STATIC_PERK_ADDITION_ACTION);
        var revocationActions = createdCashbackProps.getPromoActionsMap().getContainers(STATIC_PERK_REVOCATION_ACTION);
        assertThat(additionActions, allOf(
                iterableWithSize(1),
                hasItem(hasProperty("orderStage", equalTo(ORDER_CREATION)))
        ));
        assertThat(revocationActions, allOf(
                iterableWithSize(2),
                containsInAnyOrder(
                        hasProperty("orderStage", equalTo(ORDER_PAID)),
                        hasProperty("orderStage", equalTo(ORDER_DELIVERED))
                )
        ));

        assertEquals("Wrong addition perk name", additionPerkName,
                additionActions.get(0).getSingleParamRequired(STATIC_PERK_NAME));
        assertTrue("Doesn't contains first revocation perk name",
                revocationActions.stream()
                        .anyMatch(a -> a.getSingleParamRequired(STATIC_PERK_NAME).equals(revocationPerkName1))
        );
        assertTrue("Doesn't contains second revocation perk name",
                revocationActions.stream()
                        .anyMatch(a -> a.getSingleParamRequired(STATIC_PERK_NAME).equals(revocationPerkName2))
        );
    }

    @Test
    public void shouldNotUpdateAnaplanId() throws Exception {
        var anaplanId = "anaplanId";
        var expected = createPromoDto(10);
        expected.setAnaplanId(anaplanId);
        assertThat(expected.getAnaplanId(), equalTo(anaplanId));

        var promoId = performCreatePromo(expected);
        var actual = performGetPromo(promoId);
        assertThat(actual.getAnaplanId(), equalTo(expected.getAnaplanId()));

        var updated = actual;
        updated.setAnaplanId("updated anaplanId");
        performUpdatePromo(updated);
        actual = performGetPromo(promoId);
        assertThat(actual.getAnaplanId(), equalTo(expected.getAnaplanId()));
    }

    @Test
    public void shouldCreateAndGetExternalCashback() throws Exception {
        CashbackPromoDto cashbackPromoDto = createExternalCashback();
        long createdPromoId = performCreatePromo(cashbackPromoDto);
        CashbackPromoDto downloadedPromo = performGetPromo(createdPromoId);

        assertTrue(downloadedPromo.isExternalCashback());
        assertThat(downloadedPromo, allOf(
                hasProperty("name", equalTo(cashbackPromoDto.getName())),
                hasProperty("promoSubType", equalTo(PromoSubType.EXTERNAL_CASHBACK)),
                hasProperty("nominal", comparesEqualTo(BigDecimal.ZERO)),
                hasProperty("nominalType", equalTo(CashbackNominalType.EXTERNAL)),
                hasProperty("levelType", equalTo(EXTERNAL)),
                hasProperty("dontUploadToIdx", equalTo(true)),
                hasProperty("cashbackSource", equalTo(cashbackPromoDto.getCashbackSource()))
        ));
    }

    /**
     * Общий тест для CRUD-тестирования промо рулов
     * @throws Exception
     */
    @Test
    public void crudPromoUsageRestrictions() throws Exception {
        // create
        CashbackPromoDto promoDto = createPromoDto(10, null, false, MULTI_ORDER);
        promoDto.getUsageRestrictions().setOrderTotalMax(BigDecimal.valueOf(15_000));
        promoDto.getUsageRestrictions().setPaymentFeatures(Set.of(PaymentFeature.YA_BANK));
        long promoId = performCreatePromo(promoDto);

        CashbackPromoDto actual = performGetPromo(promoId);
        assertThat(actual.getUsageRestrictions().getOrderTotalMax(), equalTo(BigDecimal.valueOf(15_000)));
        assertThat(actual.getUsageRestrictions().getPaymentFeatures(), equalTo(Set.of(PaymentFeature.YA_BANK)));
        // update
        actual.getUsageRestrictions().setOrderTotalMax(BigDecimal.valueOf(40_000));
        actual.getUsageRestrictions().setPaymentFeatures(Set.of(PaymentFeature.YA_BANK, PaymentFeature.UNKNOWN));
        actual = performUpdateAndGetPromo(actual);
        assertThat(actual.getUsageRestrictions().getOrderTotalMax(), equalTo(BigDecimal.valueOf(40_000)));
        assertThat(actual.getUsageRestrictions().getPaymentFeatures(),
                equalTo(Set.of(PaymentFeature.YA_BANK, PaymentFeature.UNKNOWN)));
        // delete restrictions
        actual.getUsageRestrictions().setOrderTotalMax(null);
        actual.getUsageRestrictions().setPaymentFeatures(null);
        actual = performUpdateAndGetPromo(actual);
        assertThat(actual.getUsageRestrictions().getOrderTotalMax(), nullValue());
        assertThat(actual.getUsageRestrictions().getPaymentFeatures(), nullValue());
    }

    /**
     * Общий тест для CRUD-тестирования промо параметров
     * @throws Exception
     */
    @Test
    public void crudPromoParams() throws Exception {
        var promoDto = createPromoDto(5);
        promoDto.setAgitationPriority(10);
        // create
        long promoId = performCreatePromo(promoDto);
        CashbackPromoDto actual = performGetPromo(promoId);
        assertThat(actual.getAgitationPriority(), equalTo(10));
        // update
        actual.setAgitationPriority(20);
        actual = performUpdateAndGetPromo(actual);
        assertThat(actual.getAgitationPriority(), equalTo(20));
        // delete
        actual.setAgitationPriority(null);
        actual = performUpdateAndGetPromo(actual);
        assertThat(actual.getAgitationPriority(), nullValue());
    }

    // private

    private CashbackPromoDto performGetPromo(long promoId) throws Exception {
        String jsonResponse = mockMvc
                .perform(get("/api/cashback/promo/{id}", promoId))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(jsonResponse, CashbackPromoDto.class);
    }

    private void changeStatus(long promoId, PromoStatus promoStatus, HttpStatus expectedStatus) throws Exception {
        mockMvc
                .perform(
                        put("/api/cashback/promo/{promoId}/changeStatus/{status}", promoId, promoStatus.getCode())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .with(csrf())
                )
                .andDo(log())
                .andExpect(status().is(expectedStatus.value()));
    }

    private long performCreatePromo(CashbackPromoDto cashbackPromoDto) throws Exception {
        String jsonResponse = mockMvc
                .perform(post("/api/cashback/promo/create")
                        .content(objectMapper.writeValueAsString(cashbackPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(objectMapper.readValue(jsonResponse, IdObject.class).getId());
    }

    private void failCreatePromo(CashbackPromoDto cashbackPromoDto) throws Exception {
        mockMvc
                .perform(post("/api/cashback/promo/create")
                        .content(objectMapper.writeValueAsString(cashbackPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isUnprocessableEntity());
    }

    private Long addBudget(long promoId, BigDecimal amount) throws Exception {
        String jsonResponse = mockMvc
                .perform(put("/api/promo/{id}/add/budget?addedEmissionBudget={budget}", promoId, amount.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(objectMapper.readValue(jsonResponse, IdObject.class).getId());
    }

    private String performCreatePromo(CashbackPromoDto cashbackPromoDto, ResultMatcher statusMatcher) throws Exception {
        return mockMvc
                .perform(post("/api/cashback/promo/create")
                        .content(objectMapper.writeValueAsString(cashbackPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(statusMatcher)
                .andReturn().getResponse().getContentAsString();
    }

    private Long performUpdatePromo(CashbackPromoDto cashbackPromoDto) throws Exception {
        String jsonResponse = mockMvc
                .perform(put("/api/cashback/promo/update")
                        .content(objectMapper.writeValueAsString(cashbackPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(objectMapper.readValue(jsonResponse, IdObject.class).getId());
    }

    private CashbackPromoDto performUpdateAndGetPromo(CashbackPromoDto promoDto) throws Exception {
        return performGetPromo(performUpdatePromo(promoDto));
    }

    private static CashbackPromoDto createPromoDto(int percent) {
        return createPromoDto(percent, null);
    }

    private static CashbackPromoDto createPromoDto(
            int percent, BigDecimal currentEmissionBudget
    ) {
        return createPromoDto(percent, currentEmissionBudget, false, null, ITEM);
    }

    private static CashbackPromoDto createPromoDto(
            int percent, BigDecimal currentEmissionBudget, boolean canUseDeferredTransactions
    ) {
        return createPromoDto(percent, currentEmissionBudget, canUseDeferredTransactions, null, ITEM);
    }

    private static CashbackPromoDto createPromoDto(
            int percent,
            BigDecimal currentEmissionBudget,
            boolean canUseDeferredTransactions,
            String ticket
    ) {
        return createPromoDto(percent, currentEmissionBudget, canUseDeferredTransactions, ticket, ITEM);
    }

    private static CashbackPromoDto createPromoDto(
            int percent, BigDecimal currentEmissionBudget,
            boolean canUseDeferredTransactions, CashbackLevelType levelType
    ) {
        return createPromoDto(percent, currentEmissionBudget, canUseDeferredTransactions, null, levelType);
    }

    private static CashbackPromoDto createPromoDto(
            int percent,
            BigDecimal currentEmissionBudget,
            boolean canUseDeferredTransactions,
            String ticket,
            CashbackLevelType levelType
    ) {
        return CashbackPromoDto.builder()
                .setMarketPlatform(MarketPlatform.BLUE)
                .setPromoSubType(PromoSubType.YANDEX_CASHBACK)
                .setName("cashback_promo_name")
                .setTicketNumber(ticket != null ? ticket : TEST_TICKET_OK)
                .setStatus(PromoStatus.ACTIVE)
                .setCanUseDeferredTransactions(canUseDeferredTransactions)
                .setDescription("cashback_promo_desc")
                .setCurrentEmissionBudget(currentEmissionBudget)
                .setStartDate(Date.from(
                        LocalDate.of(2017, 10, 10).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .setEndDate(Date.from(
                        LocalDate.of(3000, 10, 14).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .setNominal(BigDecimal.valueOf(percent))
                .setNominalType(CashbackNominalType.PERCENT)
                .setLevelType(levelType)
                .setUsageRestrictions(new UsageRestrictionsDto())
                .setAdditionalActions(new AdditionalActionsDto())
                .setPromoSource(LOYALTY_VALUE)
                .build();
    }

    private static CashbackPromoDto createPromoDto(int percent, int priority) {
        CashbackPromoDto promoDto = createPromoDto(percent);
        promoDto.setPriority(priority);
        return promoDto;
    }

    private static CashbackPromoDto createExternalCashback() {
        return CashbackPromoDto.builder()
                .setMarketPlatform(MarketPlatform.BLUE)
                .setPromoSubType(PromoSubType.EXTERNAL_CASHBACK)
                .setCashbackSource(CashbackSource.YANDEX_BANK)
                .setName("external_cashback_promo_name")
                .setTicketNumber(TEST_TICKET_OK)
                .setStatus(PromoStatus.ACTIVE)
                .setDescription("cashback_promo_desc")
                .setStartDate(Date.from(
                        LocalDate.of(2017, 10, 10).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .setEndDate(Date.from(
                        LocalDate.of(3000, 10, 14).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .setUsageRestrictions(new UsageRestrictionsDto())
                .setAdditionalActions(new AdditionalActionsDto())
                .setPromoSource(LOYALTY_VALUE)
                .build();
    }

}
