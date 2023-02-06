package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.PromoGroupDao;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.coin.CoinDescription;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.TicketDescription;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupPromo;
import ru.yandex.market.loyalty.core.model.promogroup.brandday.BrandDayDates;
import ru.yandex.market.loyalty.core.model.promogroup.brandday.BrandDayPromoGroupResponse;
import ru.yandex.market.loyalty.core.model.promogroup.brandday.BrandDayPromoToday;
import ru.yandex.market.loyalty.core.model.promogroup.brandday.BrandDayPromoTodayResponse;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.avatar.AvatarImageId;
import ru.yandex.market.loyalty.core.service.promogroup.PromoGroupService;
import ru.yandex.market.loyalty.core.service.promogroup.PromoGroupUtils;
import ru.yandex.market.loyalty.core.service.promogroup.brandday.BrandDayPromoGroupService;
import ru.yandex.market.loyalty.lightweight.DateUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 22.07.2021
 */
@TestFor(PromoGroupController.class)
public class PromoGroupControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final String PROMO_GROUP_BASE_URL = "/promoGroup";
    private static final String PROMO_KEY = "promoLey";
    private static final String ANOTHER_PROMO_KEY = "another promo";

    @Autowired
    private BrandDayPromoGroupService brandDayPromoGroupService;
    @Autowired
    private PromoGroupService promoGroupService;
    @Autowired
    private PromoGroupDao promoGroupDao;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    protected ClockForTests clock;
    @Autowired
    private PromoManager promoManager;

    @Before
    public void init() {
        configurationService.set(ConfigurationService.BRAND_DAY_ENABLED, true);
        promoGroupService.insertOrUpdatePromoGroupReturningId(PromoGroupUtils.createBrandDayPromoGroup(
                clock.dateTime().minusDays(2), clock.dateTime().plusDays(1), "token3"
        ));
        promoGroupService.insertOrUpdatePromoGroupReturningId(PromoGroupUtils.createBrandDayPromoGroup(
                clock.dateTime().minusDays(2), clock.dateTime().minusHours(1), "token2"
        ));
        long token1 = promoGroupService.insertOrUpdatePromoGroupReturningId(PromoGroupUtils.createBrandDayPromoGroup(
                clock.dateTime().minusDays(1), clock.dateTime().plusDays(1), "token1"
        ));
        promoGroupService.insertOrUpdatePromoGroupReturningId(PromoGroupUtils.createBrandDayPromoGroup(
                clock.dateTime().minusHours(1), clock.dateTime().plusDays(1), "token0"
        ));

        SmartShoppingPromoBuilder<?> promoBuilder = getSmartShoppingPromoBuilder(PROMO_KEY);
        Promo promo = promoManager.createSmartShoppingPromo(promoBuilder);

        SmartShoppingPromoBuilder<?> dailyPromoBuilder = getSmartShoppingPromoBuilder(ANOTHER_PROMO_KEY);
        dailyPromoBuilder.addCoinRule(RuleType.HIDDEN_DATA_UNTIL_BIND_RESTRICTION_RULE,
                RuleParameterName.HIDDEN_DATA_UNTIL_BIND, Set.of(true))
                .setActionCode("dailyActionCode");
        Promo dailyPromo = promoManager.createSmartShoppingPromo(dailyPromoBuilder);

        promoGroupDao.insertPromoGroupPromo(List.of(
                new PromoGroupPromo(10L, token1, promo.getPromoId().getId(), 0)
        ));
        promoGroupDao.insertPromoGroupPromo(List.of(
                new PromoGroupPromo(11L, token1, dailyPromo.getPromoId().getId(), 6)
        ));
    }

    private SmartShoppingPromoBuilder<?> getSmartShoppingPromoBuilder(String promoKey) {
        SmartShoppingPromoBuilder<?> promoBuilder = SmartShoppingPromoBuilder.percent(BigDecimal.ONE);
        promoBuilder.getCoinProps()
                .setExpirationPolicy(ExpirationPolicy.expireByDays(3))
                .setPromoKey(promoKey);
        promoBuilder
                .setPromoSource(LOYALTY_VALUE)
                .setPromoKey(promoKey)
                .setActionCode("actionCode")
                .setCoinCreationReason(CoreCoinCreationReason.EMAIL_COMPANY)
                .setCoinDescription(CoinDescription.builder()
                        .setTitle("title")
                        .setRestrictionDescription("restrictionDescription")
                        .setDescription("descr")
                        .setAvatarImageId(new AvatarImageId(100, "groupName")))
                .setStartDate(Date.from(Instant.now().minus(Duration.ofDays(30))))
                .setEndDate(Date.from(Instant.now().plus(Duration.ofDays(30))))
                .setTicketDescription(new TicketDescription("text", "number", false))
                .setName("promoName");
        return promoBuilder;
    }

    @Test
    public void getAllValidPromoGroupForBrandDay() {
        Date endDayMax = DateUtils.toDate(clock.dateTime().plusDays(2));
        promoGroupService.insertOrUpdatePromoGroupReturningId(PromoGroupUtils.createBrandDayPromoGroup(
                clock.dateTime().plusDays(1), clock.dateTime().plusDays(2), "token4"
        ));
        BrandDayPromoGroupResponse brandDayPromoGroups = brandDayPromoGroupService.getBrandDayPromoGroups();
        assertEquals(4, brandDayPromoGroups.getPromoGroups().size());

        assertEquals(0, brandDayPromoGroups.getBrandDayEndDate().compareTo(endDayMax));
    }

    @Test
    public void checkResponse() throws Exception {
        String response =
                mockMvc.perform(
                        get(PROMO_GROUP_BASE_URL + "/brandDay/groups")
                )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        BrandDayPromoGroupResponse brandDayPromoGroupResponse = objectMapper.readValue(response,
                BrandDayPromoGroupResponse.class);
        assertEquals(3, brandDayPromoGroupResponse.getPromoGroups().size());
    }

    @Test
    public void checkOrderPromoByStartDate() {
        BrandDayPromoGroupResponse brandDayPromoGroups = brandDayPromoGroupService.getBrandDayPromoGroups();
        assertEquals(3, brandDayPromoGroups.getPromoGroups().size());
        assertEquals("token3", brandDayPromoGroups.getPromoGroups().get(0).getPromoGroupToken());
        assertEquals("token1", brandDayPromoGroups.getPromoGroups().get(1).getPromoGroupToken());
        assertEquals("token0", brandDayPromoGroups.getPromoGroups().get(2).getPromoGroupToken());

    }

    @Test
    public void checkStartEndDates() throws Exception {
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_START_DATE, "2021-08-10T00:00:00");
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_END_DATE, "2021-08-11T00:00:00");
        String response =
                mockMvc.perform(
                        get(PROMO_GROUP_BASE_URL + "/brandDay/dates")
                )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        BrandDayDates result = objectMapper.readValue(response, BrandDayDates.class);
        assertNotNull(result.getStartDay());
        assertNotNull(result.getEndDay());
        assertEquals(result.getSaleStartDay(), DateUtils.toDate(LocalDateTime.parse("2021-08-10T00:00:00")));
        assertEquals(result.getSaleEndDay(), DateUtils.toDate(LocalDateTime.parse("2021-08-11T00:00:00")));
    }

    @Test
    public void shouldNotReturnBrandDayGroupIfConfigDisable() throws Exception {
        configurationService.set(ConfigurationService.BRAND_DAY_ENABLED, false);
        String response =
                mockMvc.perform(
                        get(PROMO_GROUP_BASE_URL + "/brandDay/groups")
                )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        BrandDayPromoGroupResponse brandDayPromoGroupResponse = objectMapper.readValue(response,
                BrandDayPromoGroupResponse.class);
        assertEquals(0, brandDayPromoGroupResponse.getPromoGroups().size());
        String responseDates =
                mockMvc.perform(
                        get(PROMO_GROUP_BASE_URL + "/brandDay/dates")
                )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        BrandDayDates dates = objectMapper.readValue(responseDates, BrandDayDates.class);
        assertNull(dates.getEndDay());
        assertNull(dates.getStartDay());
    }

    @Test
    public void checkGetBrandDayPromosTodayResponse() throws Exception {
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_PAGE_URL_PREFIX,
                "https://market.yandex.ru/special/aykovlevv-brandday-live?rearr-factors=brand-day-sale" +
                        "&bonusPromoSource={activationCode}");
        configurationService.reloadCache();
        String response =
                mockMvc.perform(
                        get(PROMO_GROUP_BASE_URL + "/brandDay/promos/today")
                )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        BrandDayPromoTodayResponse brandDayPromoGroupResponse = objectMapper.readValue(response,
                BrandDayPromoTodayResponse.class);
        Assert.assertNotNull(brandDayPromoGroupResponse.getShortcuts());
        assertFalse(brandDayPromoGroupResponse.getShortcuts().isEmpty());
        assertEquals(1, brandDayPromoGroupResponse.getShortcuts().size());
        BrandDayPromoToday brandDayPromoToday = brandDayPromoGroupResponse.getShortcuts().get(0);
        assertEquals("restrictionDescription", brandDayPromoToday.getSubtitle());
        assertEquals("title", brandDayPromoToday.getTitle());
        assertEquals("promoName", brandDayPromoToday.getName());
        assertTrue(brandDayPromoToday.getAction().getUrl().contains("actionCode"));
    }

    @Test
    public void checkStartEndDatesWithForceRearrFlag() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_START_DATE,
                now.plusDays(1).truncatedTo(ChronoUnit.DAYS));
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_END_DATE,
                now.plusDays(2).truncatedTo(ChronoUnit.DAYS));
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_FORCE_DATES_REARR, "market_force_secret_sale=1");
        String response =
                mockMvc.perform(
                        get(PROMO_GROUP_BASE_URL + "/brandDay/dates")
                                .header("X-Market-Rearrfactors", "market_force_secret_sale=1")
                )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        BrandDayDates result = objectMapper.readValue(response, BrandDayDates.class);
        assertNotNull(result.getStartDay());
        assertNotNull(result.getEndDay());
        assertEquals(DateUtils.fromDate(result.getSaleStartDay()).truncatedTo(ChronoUnit.MINUTES),
                now.minusDays(1).truncatedTo(ChronoUnit.MINUTES));
        assertEquals(result.getSaleEndDay(), DateUtils.toDate(now.plusDays(1).truncatedTo(ChronoUnit.DAYS)));
    }

}
