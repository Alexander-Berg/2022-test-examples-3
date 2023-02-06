package ru.yandex.market.loyalty.back.controller.perk;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.common.xml.MimeResourceFormHttpMessageConverter;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.perk.BuyPerkRequest;
import ru.yandex.market.loyalty.api.model.perk.BuyPerkResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkStat;
import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.api.model.perk.UserPerksResponse;
import ru.yandex.market.loyalty.api.model.perk.UserThresholdStat;
import ru.yandex.market.loyalty.api.model.programs.AddUserLoyaltyProgramResponse;
import ru.yandex.market.loyalty.api.model.web.LoyaltyTag;
import ru.yandex.market.loyalty.back.controller.PerkController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.back.util.AntifraudUtils;
import ru.yandex.market.loyalty.back.util.CashbackUtils;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.config.Default;
import ru.yandex.market.loyalty.core.config.Recommendations;
import ru.yandex.market.loyalty.core.config.TrustApi;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.dao.ydb.AllUserOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.CashbackOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.PerkAcquisitionDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserBlockPromoDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.perk.PerkAcquisition;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoStatusWithBudgetCacheService;
import ru.yandex.market.loyalty.core.service.blackbox.UserInfoResponse;
import ru.yandex.market.loyalty.core.service.discount.ThresholdId;
import ru.yandex.market.loyalty.core.service.report.PersonalPerksRecommendationResponse;
import ru.yandex.market.loyalty.core.trust.PaymentMethods;
import ru.yandex.market.loyalty.core.trust.RequestStatus;
import ru.yandex.market.loyalty.core.trust.TrustBalanceResponse;
import ru.yandex.market.loyalty.core.trust.TrustBalanceResponseBalanceEntity;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.ORDER_YEAR_DELIVERED;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.PAYMENT_SYSTEM_EXTRA_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.PERSONAL_PROMO;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.PRIME;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.SBER_PRIME;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.WELCOME_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_EMPLOYEE;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_EXTRA_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_EXTRA_PHARMA_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.CANCELLED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.CONFIRMED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.IN_QUEUE;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.PENDING;
import static ru.yandex.market.loyalty.core.service.perks.PerkService.GET_PERKS_TIMEOUT;
import static ru.yandex.market.loyalty.core.test.BlackboxUtils.mockBlackbox;
import static ru.yandex.market.loyalty.core.test.BlackboxUtils.mockBlackboxResponse;
import static ru.yandex.market.loyalty.core.test.BlackboxUtils.returnWithDelay;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@TestFor(PerkController.class)
public class PerkControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final String ORDER_ID = "123";
    private static final String OTHER_ORDER_ID = "345";
    private static final long UID = 234L;
    private static final long UID_335 = 335L;
    private static final long OTHER_UID = 456L;
    private static final long MSK_REGION_ID = 213L;
    public static final int CRIMEA_AGRARNOE_REGION_ID = 27064;
    private static final int LONGER_BLACKBOX_TIMEOUT_MILLIS = GET_PERKS_TIMEOUT + 50;

    @Autowired
    private ClockForTests clock;

    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PerkAcquisitionDao perkAcquisitionDao;
    @Autowired
    Environment env;
    @Autowired
    @Default
    Tvm2 tvm2;
    @Autowired
    private CashbackUtils cashbackUtils;
    @Autowired
    @TrustApi
    private RestTemplate trustRestTemplate;
    @Autowired
    @Recommendations
    private RestTemplate recommendationRestTemplate;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private UserOrdersDao userOrdersDao;
    @Autowired
    private AllUserOrdersDao allUserOrdersDao;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    public CashbackOrdersDao cashbackOrdersDao;
    @Autowired
    public AntifraudUtils antifraudUtils;
    @Autowired
    private UserBlockPromoDao userBlockPromoDao;

    @Before
    public void setEnablePrimeFlag() {
        configurationService.enable(ConfigurationService.PRIME_ENABLE);
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.enable(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED);
    }

    @Test
    public void shouldBuyNonExistentUid() {
        marketLoyaltyClient.buyPerk(createBuyPrimeRequest(OTHER_ORDER_ID, OTHER_UID))
                .apply(ALL_OK_VISITOR);

        marketLoyaltyClient.buyPerk(createBuyPrimeRequest(ORDER_ID, UID))
                .apply(ALL_OK_VISITOR);
    }

    @Test
    public void shouldNotGivePrimeIfNotEnabled() {
        configurationService.disable(ConfigurationService.PRIME_ENABLE);

        marketLoyaltyClient.buyPerk(createBuyPrimeRequest(ORDER_ID, UID))
                .apply(ALL_OK_VISITOR);

        clock.spendTime(1, ChronoUnit.SECONDS);

        assertFalse(perkStatus(PRIME, UID, MSK_REGION_ID, true).isPurchased());
    }

    @Test
    public void shouldAllowGetTwoPerks() {
        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, PRIME, YANDEX_PLUS);
        assertFalse(statusMap.get(PRIME).isPurchased());
        assertFalse(statusMap.get(YANDEX_PLUS).isPurchased());
    }

    @Test
    public void shouldAllowGetThreePerks() {
        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, PRIME, YANDEX_PLUS, YANDEX_EMPLOYEE);
        assertFalse(statusMap.get(PRIME).isPurchased());
        assertFalse(statusMap.get(YANDEX_PLUS).isPurchased());
        assertFalse(statusMap.get(YANDEX_EMPLOYEE).isPurchased());
    }

    @Test
    public void shouldAllowGetPrimeOnly() {
        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, PRIME);
        assertFalse(statusMap.get(PRIME).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldAllowGetYandexPlusOnly() {
        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_PLUS);
        assertFalse(statusMap.get(YANDEX_PLUS).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldAllowGetDisabledYandexCashback() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_CASHBACK);
        assertFalse(statusMap.get(YANDEX_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldAllowYandexExtraCashback() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertTrue(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldNotReturnPersonalPerks() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockRecommendationResponse("perk1");

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, PERSONAL_PROMO, YANDEX_EXTRA_CASHBACK);
        assertTrue(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        PerkStat personalPerks = statusMap.get(PERSONAL_PROMO);
        assertTrue(personalPerks.isPurchased());
        assertTrue(personalPerks.getPerks().isEmpty());
        verify(recommendationRestTemplate, times(0))
                .getForEntity(any(), eq(PersonalPerksRecommendationResponse.class));
    }

    @Test
    public void shouldReturnPersonalPerks() throws Exception {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockRecommendationResponse("perk1");

        PerkStatResponse response = perkStatusWithPersonalPromoExp(
                PERSONAL_PROMO, UID, MSK_REGION_ID, true);
        assertThat(response, notNullValue());
        assertEquals(response.getStatuses().get(0).getType(), PERSONAL_PROMO);
        assertTrue(response.getStatuses().get(0).getPerks().contains("perk1"));

        PerkStatResponse responseRandom = perkStatusWithPersonalPromoExpRandom(
                PERSONAL_PROMO, UID, MSK_REGION_ID, true);
        assertThat(responseRandom, notNullValue());
        assertEquals(responseRandom.getStatuses().get(0).getType(), PERSONAL_PROMO);
        assertTrue(responseRandom.getStatuses().get(0).getPerks().contains("perk1"));

        PerkStatResponse responseYalogin = perkStatusWithPersonalPromoExpYalogin(
                PERSONAL_PROMO, UID, MSK_REGION_ID, true);
        assertThat(responseYalogin, notNullValue());
        assertEquals(responseYalogin.getStatuses().get(0).getType(), PERSONAL_PROMO);
        assertTrue(responseYalogin.getStatuses().get(0).getPerks().contains("perk1"));
    }

    @Test
    public void shouldReturnPersonalPerksUrlRearr() throws Exception {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockRecommendationResponse("perk1");

        PerkStatResponse response = perkStatusWithPersonalPromoExpUrl(
                PERSONAL_PROMO, UID, MSK_REGION_ID, true);
        assertThat(response, notNullValue());
        assertEquals(response.getStatuses().get(0).getType(), PERSONAL_PROMO);
        assertTrue(response.getStatuses().get(0).getPerks().contains("perk1"));

        PerkStatResponse responseRandom = perkStatusWithPersonalPromoExpRandomUrl(
                PERSONAL_PROMO, UID, MSK_REGION_ID, true);
        assertThat(responseRandom, notNullValue());
        assertEquals(responseRandom.getStatuses().get(0).getType(), PERSONAL_PROMO);
        assertTrue(responseRandom.getStatuses().get(0).getPerks().contains("perk1"));

        PerkStatResponse responseYalogin = perkStatusWithPersonalPromoExpYaloginUrl(
                PERSONAL_PROMO, UID, MSK_REGION_ID, true);
        assertThat(responseYalogin, notNullValue());
        assertEquals(responseYalogin.getStatuses().get(0).getType(), PERSONAL_PROMO);
        assertTrue(responseYalogin.getStatuses().get(0).getPerks().contains("perk1"));
    }

    @Test
    public void shouldGoAllCheck() {
        String rearrFactoRandom = "personal_promo_dj_program=random_promos";
        assertEquals(configurationService.getValuePersonalPromo(rearrFactoRandom), Sets.newHashSet("random_promos"));

        String rearrFactorPersonal = "personal_promo_dj_program=personal_promos";
        assertEquals(configurationService.getValuePersonalPromo(rearrFactorPersonal),
                Sets.newHashSet("personal_promos"));

        String rearrFactorYalogin = "personal_promo_dj_program=yalogin_promos";
        assertEquals(configurationService.getValuePersonalPromo(rearrFactorYalogin), Sets.newHashSet("yalogin_promos"));

        String rearrFactorAny = "personal_promo_dj_program=qwertyuiop";
        assertEquals(configurationService.getValuePersonalPromo(rearrFactorAny), Sets.newHashSet("qwertyuiop"));

        String rearrFactorSome = "personal_promo_dj_program=example1;personal_promo_dj_program=example2";
        assertEquals(configurationService.getValuePersonalPromo(rearrFactorSome),
                Sets.newHashSet("example1", "example2"));

        String rearrFactorOutside = "random_flag";
        assertTrue(configurationService.getValuePersonalPromo(rearrFactorOutside).isEmpty());

        String rearrFactorSomeOutside = "random_flag;next_random_flag";
        assertTrue(configurationService.getValuePersonalPromo(rearrFactorSomeOutside).isEmpty());

        String rearrFactorExample = "personal_promo_dj_program=example;random_flag;next_random_flag";
        assertEquals(configurationService.getValuePersonalPromo(rearrFactorExample), Sets.newHashSet("example"));

        String rearrFactorEmpty = "";
        assertTrue(configurationService.getValuePersonalPromo(rearrFactorEmpty).isEmpty());

        String rearrFactorThree = "personal_promo_dj_program=personal_promos;" +
                "personal_promo_dj_program_2=yalogin_promos2;personal_promo_dj_program_3=yetanotherexp_promo;";
        assertEquals(configurationService.getValuePersonalPromo(rearrFactorThree),
                Sets.newHashSet("personal_promos", "yalogin_promos2", "yetanotherexp_promo"));
    }

    private void mockRecommendationResponse(String... perks) {
        PersonalPerksRecommendationResponse response = new PersonalPerksRecommendationResponse();
        response.setPerks(Set.of(perks));

        when(recommendationRestTemplate.getForEntity(
                any(),
                eq(PersonalPerksRecommendationResponse.class)
        )).thenReturn(ResponseEntity.ok(response));
    }

    @Test
    public void shouldDisableYandexExtraCashbackWhenCashbackIsDisabled() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.disable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertFalse(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldEnableYandexCashbackForUnAuth() {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_FOR_UNAUTH_ENABLED);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_CASHBACK);
        assertTrue(statusMap.get(YANDEX_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldDisableYandexCashbackForUnAuth() {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_CASHBACK);
        assertFalse(statusMap.get(YANDEX_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldEnableYandexCashbackForUnAuthWithRearr() throws Exception {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_FOR_UNAUTH_REARR, "YANDEX_CASHBACK_FOR_UNAUTH_REARR");

        PerkStatResponse response = perkStatusWithRearrFlags(YANDEX_CASHBACK, UID,
                MSK_REGION_ID, true, "YANDEX_CASHBACK_FOR_UNAUTH_REARR");
        assertEquals(1, response.getStatuses().size());
        assertTrue(response.getStatuses().get(0).isPurchased());
    }

    @Test
    public void shouldDisableYandexExtraPharmaCashbackWhenCashbackIsDisabled() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_PHARMA_CASHBACK_ENABLED, false);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EXTRA_PHARMA_CASHBACK);
        assertFalse(statusMap.get(YANDEX_EXTRA_PHARMA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldDisableYandexExtraCashbackWhenExtraCashbackIsDisabled() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, false);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertFalse(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldEnableYandexExtraCashbackOnlyForSpecifiedUids() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockBlackbox(OTHER_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_UIDS_SET, UID);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertTrue(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());

        statusMap = perkStatus(OTHER_UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertFalse(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldEnableYandexExtraPharmaCashbackOnlyForSpecifiedUids() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockBlackbox(OTHER_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_PHARMA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_PHARMA_CASHBACK_UIDS_SET, UID);

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EXTRA_PHARMA_CASHBACK);
        assertTrue(statusMap.get(YANDEX_EXTRA_PHARMA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());

        statusMap = perkStatus(OTHER_UID, MSK_REGION_ID, true, YANDEX_EXTRA_PHARMA_CASHBACK);
        assertFalse(statusMap.get(YANDEX_EXTRA_PHARMA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldDisableYandexExtraCashbackIfUserHasOrderWithExtraCashback() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockBlackbox(OTHER_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        UserOrder testUserOrder = new UserOrder(OTHER_UID, "test_status", Instant.now(), "test_binding_key",
                Platform.DESKTOP);
        when(userOrdersDao.selectByUidWithBFCondition(OTHER_UID)).thenReturn(Collections.singletonList(testUserOrder));

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertTrue(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());

        statusMap = perkStatus(OTHER_UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertFalse(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldDisableYandexExtraCashbackIfUserHasOrderWithExtraCashbackForConcreteTime() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockBlackbox(OTHER_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_COUNT, 2);
        configurationService.set(ConfigurationService.USE_SPECIFIED_TIME_FOR_SEARCH_ORDERS_COUNT, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_DATE_FROM_COUNT, "2021-09-13T00:00:00");
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_DATE_TO_COUNT, "2021-09-14T00:00:00");
        Instant from = Instant.from(LocalDateTime.parse("2021-09-13T00:00:00").atZone(ZoneId.of("Europe/Moscow")));
        Instant to = Instant.from(LocalDateTime.parse("2021-09-14T00:00:00").atZone(ZoneId.of("Europe/Moscow")));
        UserOrder testUserOrder1 = new UserOrder(OTHER_UID, "test_status",
                Instant.from(LocalDateTime.parse("2021-09-13T01:00:00").atZone(ZoneId.of("Europe/Moscow"))),
                "test_binding_key", Platform.DESKTOP);
        UserOrder testUserOrder2 = new UserOrder(OTHER_UID, "test_status",
                Instant.from(LocalDateTime.parse("2021-09-13T02:00:00").atZone(ZoneId.of("Europe/Moscow"))),
                "test_binding_key2", Platform.DESKTOP);
        when(userOrdersDao.findInTimeByUid(OTHER_UID, from, to)).thenReturn(List.of(testUserOrder1, testUserOrder2));

        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertTrue(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());

        statusMap = perkStatus(OTHER_UID, MSK_REGION_ID, true, YANDEX_EXTRA_CASHBACK);
        assertFalse(statusMap.get(YANDEX_EXTRA_CASHBACK).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldGetDisabledYandexCashbackForNonExpUsers() throws Exception {
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_REARR, "yandex_cashback_enabled=1");

        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam(LoyaltyTag.UID, Long.toString(UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(213))
                .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(
                perkStatResponse,
                hasProperty(
                        "statuses",
                        hasItem(
                                allOf(
                                        hasProperty("type", equalTo(YANDEX_CASHBACK)),
                                        hasProperty("purchased", equalTo(false)),
                                        hasProperty("emitAllowed", equalTo(false)),
                                        hasProperty("spendAllowed", equalTo(false))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldGetEnabledYandexCashbackForExpUsers() throws Exception {
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_REARR, "yandex_cashback_enabled=1");

        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", "yandex_cashback_enabled=1")
                .queryParam(LoyaltyTag.UID, Long.toString(UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(213))
                .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(
                perkStatResponse,
                hasProperty(
                        "statuses",
                        hasItem(
                                allOf(
                                        hasProperty("type", equalTo(YANDEX_CASHBACK)),
                                        hasProperty("purchased", equalTo(true)),
                                        hasProperty("emitAllowed", equalTo(true)),
                                        hasProperty("spendAllowed", equalTo(true))
                                )
                        )
                )
        );
    }


    @Test
    public void shouldGetEnabledYandexCashbackIfRearrIsEmptyString() throws Exception {
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_REARR, "");

        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam(LoyaltyTag.UID, Long.toString(UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(213))
                .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(
                perkStatResponse,
                hasProperty(
                        "statuses",
                        hasItem(
                                allOf(
                                        hasProperty("type", equalTo(YANDEX_CASHBACK)),
                                        hasProperty("purchased", equalTo(true)),
                                        hasProperty("emitAllowed", equalTo(true)),
                                        hasProperty("spendAllowed", equalTo(true))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldGetBalanceForYandexCashback() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockYandexAccountBalance(BigDecimal.valueOf(100));

        when(tvm2.getServiceTicket(anyInt())).thenReturn(Option.of("test_tvm_ticket"));
        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_CASHBACK);
        assertTrue(statusMap.get(YANDEX_CASHBACK).isPurchased());
        assertThat(statusMap.get(YANDEX_CASHBACK).getBalance(), comparesEqualTo(BigDecimal.valueOf(100)));
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldGetBalanceForYandexCashbackWithDecimalPart() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockYandexAccountBalance(BigDecimal.valueOf(100.9999));

        when(tvm2.getServiceTicket(anyInt())).thenReturn(Option.of("test_tvm_ticket"));
        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_CASHBACK);
        assertTrue(statusMap.get(YANDEX_CASHBACK).isPurchased());
        assertThat(statusMap.get(YANDEX_CASHBACK).getBalance(), comparesEqualTo(BigDecimal.valueOf(100)));
        assertEquals(1, statusMap.size());
    }

    @Test()
    public void shouldReturnEmptyPerkMapWhenTrustAnswerIsTooLong() {
        mockBlackbox(UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockYandexAccountBalanceWithLongAnswer(BigDecimal.valueOf(299));

        when(tvm2.getServiceTicket(anyInt())).thenReturn(Option.of("test_tvm_ticket"));
        Map<PerkType, PerkStat> perkResponse = perkStatus(UID, MSK_REGION_ID, true, YANDEX_CASHBACK);
        assertFalse(perkResponse.get(YANDEX_CASHBACK).isPurchased());
    }

    @Test
    public void shouldGetEnabledYandexCashbackForAnyUserIfRearrEnabled() throws Exception {
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_REARR,
                "yandex_cashback_any_user_enabled=1");
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ANY_USER_ENABLED, "true");

        mockBlackbox(UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam(LoyaltyTag.UID, Long.toString(UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(213))
                .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_CASHBACK.getCode())
                .header("X-Market-Rearrfactors", "yandex_cashback_any_user_enabled=1"))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(
                perkStatResponse,
                hasProperty(
                        "statuses",
                        hasItem(
                                allOf(
                                        hasProperty("type", equalTo(YANDEX_CASHBACK)),
                                        hasProperty("purchased", equalTo(true)),
                                        hasProperty("emitAllowed", equalTo(true)),
                                        hasProperty("spendAllowed", equalTo(false))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldAllowGetYandexStaffOnly() {
        Map<PerkType, PerkStat> statusMap = perkStatus(UID, MSK_REGION_ID, true, YANDEX_EMPLOYEE);
        assertFalse(statusMap.get(YANDEX_EMPLOYEE).isPurchased());
        assertEquals(1, statusMap.size());
    }

    @Test
    public void shouldGetPurchasedYPlusFromBlackbox() {
        mockBlackbox(UID, YANDEX_PLUS, true, blackboxRestTemplate);
        assertTrue(perkStatus(UID, MSK_REGION_ID, true, YANDEX_PLUS).get(YANDEX_PLUS).isPurchased());
    }

    @Test
    public void shouldGetPurchasedYaStaffFromBlackbox() {
        mockBlackbox(UID, YANDEX_EMPLOYEE, true, blackboxRestTemplate);
        assertTrue(perkStatus(UID, MSK_REGION_ID, true, YANDEX_EMPLOYEE).get(YANDEX_EMPLOYEE).isPurchased());
    }

    @Test
    public void shouldGetNotPurchasedYaPlusFromBlackbox() {
        mockBlackbox(UID, YANDEX_PLUS, false, blackboxRestTemplate);
        assertFalse(perkStatus(UID, MSK_REGION_ID, true, YANDEX_PLUS).get(YANDEX_PLUS).isPurchased());
    }

    @Test
    public void shouldGetNotPurchasedYaStaffFromBlackbox() {
        mockBlackbox(UID, YANDEX_EMPLOYEE, false, blackboxRestTemplate);
        assertFalse(perkStatus(UID, MSK_REGION_ID, true, YANDEX_EMPLOYEE).get(YANDEX_EMPLOYEE).isPurchased());
    }

    @Test
    public void shouldNotGiveFreeDeliveryForYaPlus() {
        mockBlackbox(UID, YANDEX_PLUS, true, blackboxRestTemplate);
        PerkStat perkStatForMsk = perkStatus(UID, MSK_REGION_ID, true, YANDEX_PLUS).get(YANDEX_PLUS);
        assertTrue(perkStatForMsk.isPurchased());
        assertFalse(perkStatForMsk.isFreeDelivery());
    }

    @Test
    public void shouldFallbackToNotPurchasedOnUserNotFoundInBlackbox() {
        assertFalse(perkStatus(UID, MSK_REGION_ID, true, YANDEX_PLUS).get(YANDEX_PLUS).isPurchased());
    }

    @Test
    public void shouldFailYaPlusOnBlackboxTimeout() {
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("uid=" + UID))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).then(invocation -> returnWithDelay(
                mockBlackboxResponse(true, YANDEX_EMPLOYEE),
                LONGER_BLACKBOX_TIMEOUT_MILLIS
        ));

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                perkStatus(UID, MSK_REGION_ID, true, PRIME, YANDEX_PLUS)
        );
        assertEquals(MarketLoyaltyErrorCode.BLACKBOX_EXCEPTION, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldFailOnBlackboxIoException() {
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("uid=" + UID))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).thenThrow(new ResourceAccessException("network is unreachable", new SocketTimeoutException()));

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                perkStatus(UID, MSK_REGION_ID, true, PRIME, YANDEX_PLUS)
        );
        assertEquals(MarketLoyaltyErrorCode.BLACKBOX_EXCEPTION, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldFailYaStaffOnBlackboxTimeout() {
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("uid=" + UID))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).then(invocation -> returnWithDelay(
                mockBlackboxResponse(true, YANDEX_EMPLOYEE),
                LONGER_BLACKBOX_TIMEOUT_MILLIS
        ));

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                perkStatus(UID, MSK_REGION_ID, true, PRIME, YANDEX_EMPLOYEE)
        );
        assertEquals(MarketLoyaltyErrorCode.BLACKBOX_EXCEPTION, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldNotReturnSberPrimePerkStatus() {
        final long TEST_UID = 4443331;
        Instant now = clock.instant();
        Instant startDate = now.minus(1, ChronoUnit.DAYS);
        Instant endDate = now.plus(1, ChronoUnit.DAYS);
        PerkAcquisition basePerk = new PerkAcquisition(123L, SBER_PRIME, TEST_UID, null, startDate, endDate,
                startDate, null);

        perkAcquisitionDao.upsertPerkAcquisitionWithEmptyOrderId(basePerk);
        PerkStatResponse response = marketLoyaltyClient.perkStatus(SBER_PRIME, TEST_UID, MSK_REGION_ID, false);
        assertThat(response.getStatuses(), hasSize(1));
        PerkStat perkStatus = response.getStatuses().iterator().next();
        assertThat(perkStatus.getType(), equalTo(SBER_PRIME));
        assertThat(perkStatus.isPurchased(), equalTo(false));
    }

    @Test
    public void shouldReturnSberPrimePerkStatusIfPerkNotFound() {
        final long TEST_UID = 44435552;

        PerkStatResponse response = marketLoyaltyClient.perkStatus(SBER_PRIME, TEST_UID, MSK_REGION_ID, false);
        assertThat(response.getStatuses(), hasSize(1));
        PerkStat perkStatus = response.getStatuses().iterator().next();
        assertThat(perkStatus.getType(), equalTo(SBER_PRIME));
        assertThat(perkStatus.isPurchased(), equalTo(false));
    }

    @Test
    public void shouldFailPerkStatus() {
        final long TEST_UID = 44435552;

        configurationService.set(ConfigurationService.FAIL_PERK_STATUS_TESTING, "true");
        assertThrows(MarketLoyaltyException.class, () -> marketLoyaltyClient.perkStatus(SBER_PRIME, TEST_UID,
                MSK_REGION_ID, false));

        configurationService.set(ConfigurationService.FAIL_PERK_STATUS_TESTING, "false");
        PerkStatResponse response = marketLoyaltyClient.perkStatus(SBER_PRIME, TEST_UID, MSK_REGION_ID, false);
        assertNotNull(response);
    }

    @Test
    public void shouldReturnWelcomeCashbackPerk() throws Exception {
        final long TEST_UID = 1111;
        final BigDecimal THRESHOLD = BigDecimal.valueOf(3500);
        final BigDecimal CASHBACK = BigDecimal.valueOf(500);
        final String PROMO_KEY = "PROMO_" + TEST_UID;
        Instant promoStart = clock.instant()
                .minus(7, ChronoUnit.DAYS);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY, PROMO_KEY);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
                promoStart.toString());
        configurationService.set(ConfigurationService.MARKET_PROMO_ADVERTISING_CAMPAIGN_500_REARR,
                "market_promo_advertising_campaign_500=1");
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_CASHBACK_AMOUNT, CASHBACK);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_THRESHOLD, THRESHOLD);
        String multiOrderId = UUID.randomUUID().toString();
        createPromoForWelcomeCashbackPerk(PROMO_KEY, BigDecimal.valueOf(1000));

        PerkStatResponse perkStatResponse = perkStatusWithRearrFlags(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false
                , "market_promo_advertising_campaign_500=1");
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.TRUE))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("threshold", equalTo(THRESHOLD))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("cashback", equalTo(CASHBACK))));

        allUserOrdersDao.upsert(new UserOrder(TEST_UID, OrderStatus.DELIVERED.name(), promoStart.minus(7,
                ChronoUnit.DAYS),
                "1", Platform.DESKTOP));
        allUserOrdersDao.upsert(new UserOrder(TEST_UID, OrderStatus.CANCELLED.name(), promoStart.minus(7,
                ChronoUnit.DAYS),
                "2", Platform.DESKTOP));
        allUserOrdersDao.upsert(new UserOrder(TEST_UID, OrderStatus.DELIVERED.name(), promoStart.plus(7,
                ChronoUnit.DAYS),
                multiOrderId + ",3", Platform.IOS));
        allUserOrdersDao.upsert(new UserOrder(TEST_UID, OrderStatus.CANCELLED.name(), promoStart.plus(7,
                ChronoUnit.DAYS),
                multiOrderId + ",4", Platform.IOS));

        perkStatResponse = perkStatusWithRearrFlags(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false,
                "market_promo_advertising_campaign_500=1");
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.TRUE))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("threshold", equalTo(THRESHOLD))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("cashback", equalTo(CASHBACK))));

    }

    @Test
    public void shouldNotReturnWelcomeCashback() throws Exception {
        long TEST_UID = 1112;
        final String PROMO_KEY = "PROMO_" + TEST_UID;
        final BigDecimal THRESHOLD = BigDecimal.valueOf(3500);
        final BigDecimal CASHBACK = BigDecimal.valueOf(500);
        Instant promoStart = clock.instant()
                .minus(7, ChronoUnit.DAYS);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY, PROMO_KEY);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
                promoStart.toString());
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_CASHBACK_AMOUNT, CASHBACK);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_THRESHOLD, THRESHOLD);
        configurationService.set(ConfigurationService.MARKET_PROMO_ADVERTISING_CAMPAIGN_500_REARR,
                "market_promo_advertising_campaign_500=1");
        String multiOrderId = UUID.randomUUID().toString();
        createPromoForWelcomeCashbackPerk(PROMO_KEY, BigDecimal.valueOf(1000));

        PerkStatResponse perkStatResponse = perkStatusWithRearrFlags(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false
                , "market_promo_advertising_campaign_500=1");
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.TRUE))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("threshold", equalTo(THRESHOLD))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("cashback", equalTo(CASHBACK))));

        allUserOrdersDao.upsert(new UserOrder(TEST_UID, OrderStatus.DELIVERED.name(), promoStart.minus(7,
                ChronoUnit.DAYS),
                "5", Platform.IOS));
        perkStatResponse = perkStatusWithRearrFlags(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false,
                "market_promo_advertising_campaign_500=1");
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("threshold", equalTo(THRESHOLD))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("cashback", equalTo(CASHBACK))));

        TEST_UID = 1113;
        allUserOrdersDao.upsert(new UserOrder(TEST_UID, OrderStatus.DELIVERED.name(), promoStart.plus(7,
                ChronoUnit.DAYS),
                multiOrderId + ",6", Platform.IOS));
        allUserOrdersDao.upsert(new UserOrder(TEST_UID, OrderStatus.DELIVERED.name(), promoStart.plus(7,
                ChronoUnit.DAYS),
                multiOrderId + ",7", Platform.IOS));
        perkStatResponse = perkStatusWithRearrFlags(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false,
                "market_promo_advertising_campaign_500=1");
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("threshold", equalTo(THRESHOLD))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("cashback", equalTo(CASHBACK))));
    }

    @Test
    public void shouldNotReturnWelcomeCashbackRearrDisabled() throws Exception {
        long TEST_UID = 1112;
        Instant promoStart = clock.instant()
                .minus(7, ChronoUnit.DAYS);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY, "TEST_PROMO");
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
                promoStart.toString());

        PerkStatResponse perkStatResponse = perkStatusWithRearrFlags(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false
                , "market_promo_advertising_campaign_500=1");
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
    }

    @Test
    public void shouldDisableWelcomeCashback() throws Exception {
        long TEST_UID = 1113;
        final BigDecimal THRESHOLD = BigDecimal.valueOf(3500);
        final BigDecimal CASHBACK = BigDecimal.valueOf(500);
        final String PROMO_KEY = "PROMO_" + TEST_UID;
        Instant promoStart = clock.instant()
                .minus(7, ChronoUnit.DAYS);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY, PROMO_KEY);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
                promoStart.toString());
        configurationService.set(ConfigurationService.MARKET_PROMO_ADVERTISING_CAMPAIGN_500_REARR,
                "market_promo_advertising_campaign_500=1");
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_CASHBACK_AMOUNT, CASHBACK);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_THRESHOLD, THRESHOLD);
        Promo promo = createPromoForWelcomeCashbackPerk(PROMO_KEY, BigDecimal.valueOf(1000));

        PerkStatResponse perkStatResponse = perkStatusWithRearrFlags(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false
                , "market_promo_advertising_campaign_500=1");
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.TRUE))));

        accountDao.updateBalance(promo.getBudgetEmissionAccountId(), BigDecimal.valueOf(-1000));
        promoStatusWithBudgetCacheService.reloadCache();
        perkStatResponse = perkStatusWithMarket500PromoRearr(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));

        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY, PROMO_KEY + "_1");
        promo = createPromoForWelcomeCashbackPerk(PROMO_KEY + "_1", BigDecimal.valueOf(1000));

        perkStatResponse = perkStatusWithMarket500PromoRearr(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.TRUE))));

        promoDao.updatePromoStatus(promo, PromoStatus.INACTIVE);
        promoStatusWithBudgetCacheService.reloadCache();
        perkStatResponse = perkStatusWithMarket500PromoRearr(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID, false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
    }

    @Test
    public void shouldReturnWelcomeCashbackPerkToNotLoggedInUser() throws Exception {
        final long TEST_UID = 1_152_921_504_606_846_978L;
        final BigDecimal THRESHOLD = BigDecimal.valueOf(3500);
        final BigDecimal CASHBACK = BigDecimal.valueOf(500);
        Instant promoStart = clock.instant()
                .minus(7, ChronoUnit.DAYS);
        final String PROMO_KEY = "PROMO_" + TEST_UID;
        createPromoForWelcomeCashbackPerk(PROMO_KEY, BigDecimal.valueOf(1000));
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY, PROMO_KEY);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
                promoStart.toString());
        configurationService.set(ConfigurationService.MARKET_PROMO_ADVERTISING_CAMPAIGN_500_REARR,
                "market_promo_advertising_campaign_500=1");
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_CASHBACK_AMOUNT, CASHBACK);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_THRESHOLD, THRESHOLD);

        PerkStatResponse perkStatResponse = perkStatusWithMarket500PromoRearr(WELCOME_CASHBACK, TEST_UID,
                MSK_REGION_ID, false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.TRUE))));
    }

    @Test
    public void shouldReturnBlockedPromo() throws Exception {
        configurationService.set(ConfigurationService.BLOCK_PROMO_ENABLED, true);

        userBlockPromoDao.upsertUserBlockPromo(UID, "some_threshold", true);
        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(213))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertEquals(1, perkStatResponse.getDisabledPromoThresholds().size());
        assertEquals("some_threshold", perkStatResponse.getDisabledPromoThresholds().get(0).getName());

    }

    @Test
    public void shouldNotReturnWelcomeCashbackPerkToNotLoggedInUser() {
        final long TEST_UID = 1_152_921_504_606_846_979L;

        PerkStatResponse perkStatResponse = marketLoyaltyClient.perkStatus(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID,
                false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
    }

    @Test
    public void shouldDisableTotalCashbackThreshold() {
        antifraudUtils.mockTotalCashbackThresholdExhausted(DEFAULT_UID, Date.from(clock.instant().minus(5,
                ChronoUnit.DAYS)));

        final List<String> thresholds = disabledThresholds(DEFAULT_UID, MSK_REGION_ID, false);

        assertThat(thresholds, contains(equalTo(ThresholdId.TOTAL_CASHBACK.getId())));
    }

    private Promo createPromoForWelcomeCashbackPerk(String promoKey, BigDecimal budget) {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.ONE)
                .setPromoKey(promoKey)
                .setEmissionBudget(budget)
        );
        promoStatusWithBudgetCacheService.reloadCache();
        return promo;
    }

    @Test
    public void shouldReturnWelcomeCashbackFalseWithoutParams() {
        long TEST_UID = 1113;

        PerkStatResponse perkStatResponse = marketLoyaltyClient.perkStatus(WELCOME_CASHBACK, TEST_UID, MSK_REGION_ID,
                false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("threshold", is(nullValue()))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("cashback", is(nullValue()))));
    }

    @Test
    public void shouldPaymentSystemExtraCashbackBeActive() {
        long UID = 1113;

        Promo cashbackPromo = setupPaymentSystemPromo();

        configurePaymentSystemPerk(UID);

        cashbackUtils.createUserAccruals(UID, cashbackPromo.getPromoId().getId(), Pair.of(100L, CONFIRMED));

        PerkStatResponse perkStatResponse = marketLoyaltyClient.perkStatus(PAYMENT_SYSTEM_EXTRA_CASHBACK, UID,
                MSK_REGION_ID, false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.TRUE))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("cashbackPercentNominal",
                comparesEqualTo(BigDecimal.valueOf(10)))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("maxCashbackTotal",
                comparesEqualTo(BigDecimal.valueOf(3000)))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("paymentSystem",
                equalTo(PaymentSystem.MASTERCARD))));
    }

    @Test
    public void shouldPaymentSystemExtraCashbackBeInactiveIfMaxOrdersCountExceeded() {
        long UID = 1113;

        Promo cashbackPromo = setupPaymentSystemPromo();

        configurePaymentSystemPerk(UID, 3, true, true);

        cashbackUtils.createUserAccruals(UID,
                cashbackPromo.getPromoId().getId(),
                Pair.of(100L, CONFIRMED),
                Pair.of(100L, IN_QUEUE),
                Pair.of(100L, PENDING)
        );

        PerkStatResponse perkStatResponse = marketLoyaltyClient.perkStatus(PAYMENT_SYSTEM_EXTRA_CASHBACK, UID,
                MSK_REGION_ID, false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
    }

    @Test
    public void shouldPaymentSystemExtraCashbackBeInactiveIfPerkDisabled() {
        long UID = 1113;

        Promo cashbackPromo = setupPaymentSystemPromo();

        configurePaymentSystemPerk(UID, 3, true, false);

        cashbackUtils.createUserAccruals(UID,
                cashbackPromo.getPromoId().getId(),
                Pair.of(100L, CONFIRMED),
                Pair.of(100L, CANCELLED),
                Pair.of(100L, CANCELLED)
        );

        PerkStatResponse perkStatResponse = marketLoyaltyClient.perkStatus(PAYMENT_SYSTEM_EXTRA_CASHBACK, UID,
                MSK_REGION_ID, false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
    }

    @Test
    public void shouldPaymentSystemExtraCashbackBeInactiveIfUserNotYaPlusSubscriber() {
        long UID = 1113;

        Promo cashbackPromo = setupPaymentSystemPromo();

        configurePaymentSystemPerk(UID, 3, false, true);

        cashbackUtils.createUserAccruals(UID,
                cashbackPromo.getPromoId().getId(),
                Pair.of(100L, CONFIRMED)
        );

        PerkStatResponse perkStatResponse = marketLoyaltyClient.perkStatus(PAYMENT_SYSTEM_EXTRA_CASHBACK, UID,
                MSK_REGION_ID, false);
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
    }

    @Test
    @Ignore("Not actual")
    public void shouldPaymentSystemExtraCashbackBeInactiveIfUserInCrimea() {
        long UID = 1113;

        Promo cashbackPromo = setupPaymentSystemPromo();

        configurePaymentSystemPerk(UID);

        cashbackUtils.createUserAccruals(UID,
                cashbackPromo.getPromoId().getId(),
                Pair.of(100L, CONFIRMED)
        );

        PerkStatResponse perkStatResponse = marketLoyaltyClient.perkStatus(PAYMENT_SYSTEM_EXTRA_CASHBACK,
                UID,
                CRIMEA_AGRARNOE_REGION_ID,
                false
        );
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
    }

    @Test
    public void shouldGetOrderYearOver700() throws Exception {
        configurationService.set(ConfigurationService.PERK_ORDER_YEAR_DELIVERED_THRESHOLD, "70000");

        mockBlackbox(UID, YANDEX_EMPLOYEE, true, blackboxRestTemplate);
        allUserOrdersDao.upsert(new UserOrder(UID_335, "DELIVERED", Instant.now(), "binding-key", Platform.DESKTOP,
                800_00L));

        final PerkStatResponse perkStatResponse = objectMapper.readValue(
                mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(UID_335))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(213))
                        .queryParam(LoyaltyTag.PERK_TYPE, ORDER_YEAR_DELIVERED.getCode()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(), PerkStatResponse.class);

        assertThat(
                perkStatResponse,
                hasProperty(
                        "statuses",
                        hasItem(
                                allOf(
                                        hasProperty("type", equalTo(ORDER_YEAR_DELIVERED)),
                                        hasProperty("purchased", equalTo(true))
                                )
                        )
                )
        );
    }

    @Test
    @Ignore("Not actual")
    public void shouldPaymentSystemExtraCashbackBeInactiveIfUserInCrimeaAndLaasApiNotWorking() throws Exception {
        configurationService.enable(ConfigurationService.LAAS_SERVICE_API_ENABLED);
        long UID = 1113;

        Promo cashbackPromo = setupPaymentSystemPromo();

        configurePaymentSystemPerk(UID);

        cashbackUtils.createUserAccruals(UID,
                cashbackPromo.getPromoId().getId(),
                Pair.of(100L, CONFIRMED)
        );

        PerkStatResponse perkStatResponse = perkStatusWithRealIpHeader(PAYMENT_SYSTEM_EXTRA_CASHBACK,
                UID,
                CRIMEA_AGRARNOE_REGION_ID,
                false,
                "real-ip"
        );
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.FALSE))));
    }

    @Test
    public void shouldPaymentSystemExtraCashbackBeActiveLaasApiNotWorking() throws Exception {
        configurationService.enable(ConfigurationService.LAAS_SERVICE_API_ENABLED);
        long UID = 1113;

        Promo cashbackPromo = setupPaymentSystemPromo();
        configurePaymentSystemPerk(UID);

        cashbackUtils.createUserAccruals(UID, cashbackPromo.getPromoId().getId(), Pair.of(100L, CONFIRMED));

        PerkStatResponse perkStatResponse = perkStatusWithRealIpHeader(PAYMENT_SYSTEM_EXTRA_CASHBACK, UID,
                MSK_REGION_ID, false, "real-ip");
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("purchased", equalTo(Boolean.TRUE))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("cashbackPercentNominal",
                comparesEqualTo(BigDecimal.valueOf(10)))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("maxCashbackTotal",
                comparesEqualTo(BigDecimal.valueOf(3000)))));
        assertThat(perkStatResponse.getStatuses(), everyItem(hasProperty("paymentSystem",
                equalTo(PaymentSystem.MASTERCARD))));
    }

    @Test
    public void shouldReturnZeroAsMaxPromoDiscountPercent() throws Exception {
        PerkStatResponse response = perkStatusWithRealIpHeader(YANDEX_CASHBACK, DEFAULT_UID, MSK_REGION_ID,
                false, "0.0.0.0");
        assertThat(response.getStatuses(), hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_CASHBACK)),
                hasProperty("maxPromoDiscountPercent", equalTo(BigDecimal.valueOf(0)))
        )));
    }


    private PerkStatResponse perkStatusWithMarket500PromoRearr(PerkType perkType, long uid, long regionId,
                                                               boolean noCache) throws Exception {
        return perkStatusWithRearrFlags(perkType, uid, regionId, noCache, "market_promo_advertising_campaign_500=1");
    }

    private PerkStatResponse perkStatusWithPersonalPromoExp(PerkType perkType, long uid, long regionId,
                                                            boolean noCache) throws Exception {
        return perkStatusWithRearrFlags(perkType, uid, regionId, noCache, "personal_promo_dj_program=personal_promos");
    }

    private PerkStatResponse perkStatusWithPersonalPromoExpUrl(PerkType perkType, long uid, long regionId,
                                                               boolean noCache) throws Exception {
        return perkStatusWithExperiments(perkType, uid, regionId, noCache, "personal_promo_dj_program" +
                "=personal_promos");
    }

    private PerkStatResponse perkStatusWithPersonalPromoExpRandom(PerkType perkType, long uid, long regionId,
                                                                  boolean noCache) throws Exception {
        return perkStatusWithRearrFlags(perkType, uid, regionId, noCache, "personal_promo_dj_program=random_promos");
    }

    private PerkStatResponse perkStatusWithPersonalPromoExpRandomUrl(PerkType perkType, long uid, long regionId,
                                                                     boolean noCache) throws Exception {
        return perkStatusWithExperiments(perkType, uid, regionId, noCache, "personal_promo_dj_program" +
                "=random_promos");
    }

    private PerkStatResponse perkStatusWithPersonalPromoExpYalogin(PerkType perkType, long uid, long regionId,
                                                                   boolean noCache) throws Exception {
        return perkStatusWithRearrFlags(perkType, uid, regionId, noCache, "personal_promo_dj_program=yalogin_promos");
    }

    private PerkStatResponse perkStatusWithPersonalPromoExpYaloginUrl(PerkType perkType, long uid, long regionId,
                                                                      boolean noCache) throws Exception {
        return perkStatusWithExperiments(perkType, uid, regionId, noCache, "personal_promo_dj_program" +
                "=yalogin_promos");
    }

    private PerkStatResponse perkStatusWithRealIpHeader(PerkType perkType, long uid, long regionId, boolean noCache,
                                                        String realIp) throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Real-IP", realIp)
                        .queryParam(LoyaltyTag.UID, Long.toString(uid))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(noCache))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(regionId))
                        .queryParam(LoyaltyTag.PERK_TYPE, perkType.getCode()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(), PerkStatResponse.class);
    }

    private PerkStatResponse perkStatusWithRearrFlags(PerkType perkType, long uid, long regionId, boolean noCache,
                                                      String rearrFlag) throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Market-Rearrfactors", rearrFlag)
                        .queryParam(LoyaltyTag.UID, Long.toString(uid))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(noCache))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(regionId))
                        .queryParam(LoyaltyTag.PERK_TYPE, perkType.getCode()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(), PerkStatResponse.class);
    }

    private PerkStatResponse perkStatusWithExperiments(PerkType perkType, long uid, long regionId,
                                                       boolean noCache, String rearrFlag) throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(uid))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(noCache))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(regionId))
                        .queryParam(LoyaltyTag.PERK_TYPE, perkType.getCode())
                        .queryParam(LoyaltyTag.EXPERIMENTS, rearrFlag))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(), PerkStatResponse.class);
    }

    private AddUserLoyaltyProgramResponse setUserPromoPerk(long uid, String hash, String actionId) throws Exception {
        String response = mockMvc
                .perform(post("/perk/set-perk?uid={uid}&hash={hash}&actionId={actionId}", uid, hash, actionId))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, AddUserLoyaltyProgramResponse.class);
    }

    private UserPerksResponse getUserPerks(long uid) throws Exception {
        String response = mockMvc
                .perform(get("/perk/list/{uid}", uid))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, UserPerksResponse.class);
    }

    private static BuyPerkRequest createBuyPrimeRequest(String orderId, long uid) {
        BuyPerkRequest buyPerkRequest = new BuyPerkRequest();
        buyPerkRequest.setPerkType(PRIME);
        buyPerkRequest.setOrderId(orderId);
        buyPerkRequest.setUid(uid);
        return buyPerkRequest;
    }

    private static AssertionError createFailOnUnexpectedResponse(BuyPerkResponse buyPerkResponse) {
        return new AssertionError("Unexpected response: " + buyPerkResponse);
    }

    private Map<PerkType, PerkStat> perkStatus(long uid, long regionId, boolean noCache, PerkType... type) {
        return marketLoyaltyClient.perkStatus(Arrays.asList(type), uid, regionId, noCache).getStatuses().stream().collect(Collectors.toMap(PerkStat::getType, Function.identity()));
    }

    private List<String> disabledThresholds(long uid, long regionId, boolean noCache) {
        return marketLoyaltyClient.perkStatus(Collections.emptyList(), uid, regionId, noCache).getDisabledPromoThresholds().stream()
                .map(UserThresholdStat::getName)
                .collect(Collectors.toList());
    }

    private PerkStat perkStatus(PerkType type, long uid, long regionId, boolean noCache) {
        return perkStatus(uid, regionId, noCache, type).get(type);
    }

    private void assertPrimeStatus(long uid, boolean isPurchased) {
        Map<PerkType, PerkStat> stat;
        stat = marketLoyaltyClient.perkStatus(PRIME, uid, MSK_REGION_ID, false)
                .getStatuses()
                .stream()
                .collect(Collectors.toMap(PerkStat::getType, Function.identity()));

        assertThat(stat.get(PRIME).isPurchased(), equalTo(isPurchased));
    }

    private void mockYandexAccountBalance(BigDecimal balance) {
        when(trustRestTemplate.getMessageConverters())
                .thenReturn(Collections.singletonList(new MimeResourceFormHttpMessageConverter()));

        when(trustRestTemplate.acceptHeaderRequestCallback(eq(PaymentMethods.class)))
                .thenReturn(request -> {
                });

        doReturn(
                new TrustBalanceResponse(
                        RequestStatus.SUCCESS,
                        null,
                        Collections.singletonList(
                                new TrustBalanceResponseBalanceEntity(
                                        "1",
                                        balance,
                                        "RUB"
                                ))
                ))
                .when(trustRestTemplate)
                .execute(anyString(), eq(HttpMethod.GET), any(RequestCallback.class), any(ResponseExtractor.class));
    }

    private void mockYandexAccountBalanceWithLongAnswer(BigDecimal balance) {
        when(trustRestTemplate.getMessageConverters())
                .thenReturn(Collections.singletonList(new MimeResourceFormHttpMessageConverter()));

        when(trustRestTemplate.acceptHeaderRequestCallback(eq(PaymentMethods.class)))
                .thenReturn(request -> {
                });

        when(
                trustRestTemplate.execute(
                        anyString(),
                        eq(HttpMethod.GET),
                        any(RequestCallback.class),
                        any(ResponseExtractor.class))
        ).thenAnswer((Answer<TrustBalanceResponse>) invocation -> {
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return new TrustBalanceResponse(
                    RequestStatus.SUCCESS,
                    null,
                    Collections.singletonList(
                            new TrustBalanceResponseBalanceEntity(
                                    "1",
                                    balance,
                                    "RUB"
                            ))
            );
        });

    }

    private Promo setupPaymentSystemPromo() {
        Promo cashbackPromo =
                promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10),
                        CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MAX_CASHBACK_FILTER_RULE, RuleParameterName.MAX_CASHBACK,
                                BigDecimal.valueOf(3000))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, RuleParameterName.PERK_TYPE,
                                PAYMENT_SYSTEM_EXTRA_CASHBACK)
                );

        promoStatusWithBudgetCacheService.reloadCache();
        cashbackCacheService.reloadCashbackPromos();

        return cashbackPromo;
    }

    private void configurePaymentSystemPerk(long uid) {
        configurePaymentSystemPerk(uid, 3, true, true);
    }

    private void configurePaymentSystemPerk(long uid, int maxOrdersCount,
                                            boolean yaPlusSubscriber, boolean isPerkEnabled) {
        configurationService.set(ConfigurationService.PAYMENT_SYSTEM_EXTRA_CASHBACK_MAX_ORDERS_COUNT, maxOrdersCount);
        configurationService.set(ConfigurationService.PAYMENT_SYSTEM_EXTRA_CASHBACK_ENABLED, isPerkEnabled);
        mockBlackbox(uid, YANDEX_PLUS, yaPlusSubscriber, blackboxRestTemplate);
    }


    private static final BuyPerkResponse.Visitor<Void, AssertionError> ALL_OK_VISITOR =
            new BuyPerkResponse.Visitor<Void, AssertionError>() {
        @Override
        public Void visit(BuyPerkResponse.AllOk allOk) {
            return null;
        }

        @Override
        public Void visit(BuyPerkResponse.AlreadyPurchasedOnAnotherOrder alreadyPurchasedOnAnotherOrder) {
            throw createFailOnUnexpectedResponse(alreadyPurchasedOnAnotherOrder);
        }

        @Override
        public Void visit(BuyPerkResponse.AlreadyPurchasedOnSameOrder alreadyPurchasedOnSameOrder) {
            throw createFailOnUnexpectedResponse(alreadyPurchasedOnSameOrder);
        }

        @Override
        public Void visit(BuyPerkResponse.Unknown unknown) throws AssertionError {
            throw createFailOnUnexpectedResponse(unknown);
        }
    };

    private static final BuyPerkResponse.Visitor<Void, AssertionError> ALREADY_PURCHASED_ON_SAME_ORDER_VISITOR =
            new BuyPerkResponse.Visitor<Void, AssertionError>() {
        @Override
        public Void visit(BuyPerkResponse.AllOk allOk) {
            throw createFailOnUnexpectedResponse(allOk);
        }

        @Override
        public Void visit(BuyPerkResponse.AlreadyPurchasedOnAnotherOrder alreadyPurchasedOnAnotherOrder) {
            throw createFailOnUnexpectedResponse(alreadyPurchasedOnAnotherOrder);
        }

        @Override
        public Void visit(BuyPerkResponse.AlreadyPurchasedOnSameOrder alreadyPurchasedOnSameOrder) {
            return null;
        }

        @Override
        public Void visit(BuyPerkResponse.Unknown unknown) throws AssertionError {
            throw createFailOnUnexpectedResponse(unknown);
        }
    };

    private static final BuyPerkResponse.Visitor<String, AssertionError> ALREADY_PURCHASED_ON_ANOTHER_ORDER_VISITOR =
            new BuyPerkResponse.Visitor<String, AssertionError>() {
        @Override
        public String visit(BuyPerkResponse.AllOk allOk) {
            throw createFailOnUnexpectedResponse(allOk);
        }

        @Override
        public String visit(BuyPerkResponse.AlreadyPurchasedOnAnotherOrder alreadyPurchasedOnAnotherOrder) {
            return alreadyPurchasedOnAnotherOrder.getOrderId();
        }

        @Override
        public String visit(BuyPerkResponse.AlreadyPurchasedOnSameOrder alreadyPurchasedOnSameOrder) {
            throw createFailOnUnexpectedResponse(alreadyPurchasedOnSameOrder);
        }

        @Override
        public String visit(BuyPerkResponse.Unknown unknown) throws AssertionError {
            throw createFailOnUnexpectedResponse(unknown);
        }
    };

    private static class AggregatingAllOkVisitor implements BuyPerkResponse.Visitor<Void, AssertionError> {
        private final LongAdder allOkCounter = new LongAdder();
        private final boolean failOnAnotherOrder;
        private final boolean failOnSameOrder;

        AggregatingAllOkVisitor(boolean failOnAnotherOrder, boolean failOnSameOrder) {
            this.failOnAnotherOrder = failOnAnotherOrder;
            this.failOnSameOrder = failOnSameOrder;
        }

        int getAllOkCount() {
            return allOkCounter.intValue();
        }

        @Override
        public Void visit(BuyPerkResponse.AllOk allOk) {
            allOkCounter.increment();
            return null;
        }

        @Override
        public Void visit(BuyPerkResponse.AlreadyPurchasedOnAnotherOrder alreadyPurchasedOnAnotherOrder) {
            if (failOnAnotherOrder) {
                throw createFailOnUnexpectedResponse(alreadyPurchasedOnAnotherOrder);
            }
            return null;
        }

        @Override
        public Void visit(BuyPerkResponse.AlreadyPurchasedOnSameOrder alreadyPurchasedOnSameOrder) {
            if (failOnSameOrder) {
                throw createFailOnUnexpectedResponse(alreadyPurchasedOnSameOrder);
            }
            return null;
        }

        @Override
        public Void visit(BuyPerkResponse.Unknown unknown) throws AssertionError {
            throw createFailOnUnexpectedResponse(unknown);
        }
    }
}
