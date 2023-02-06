package ru.yandex.market.loyalty.back.controller;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.loyalty.api.model.promocode.referal.ReferralPromocodeResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.ydb.UserAccrualsCacheDao;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.model.ydb.UserAccrualsCacheEntry;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoStatusWithBudgetCacheService;
import ru.yandex.market.loyalty.core.service.promocode.ReferralProgramService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.OneTimeSupplier;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.*;

@TestFor(PromocodesController.class)
public class ReferralPromocodeEndpointTest extends MarketLoyaltyBackMockedDbTestBase {

    private final static String DEFAULT_ACCRUAL_PROMO_KEY = "accrual_promo";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    private ReferralProgramService referralProgramService;
    @Autowired
    private UserAccrualsCacheDao userAccrualsCacheDao;
    @Autowired
    private ReferralProgramService.ReferralPromosCache referralPromosCache;
    private boolean init = false;

    @Before
    public void init() {
        if (init) {
            return;
        }
        createPromosForReferralProgram();
        turnOnReferralProgram();
        this.init = true;
    }

    @Test
    public void shouldReturnNewPromocodeForNewUser() {
        ReferralPromocodeResponse response = sendRequest(12L);
        assertThat(response, allOf(
                hasProperty("refererPromoCode", notNullValue()),
                hasProperty("refererPromoCodeExpiredDate", notNullValue()),
                hasProperty("alreadyGot", equalTo(0L)),
                hasProperty("friendsOrdered", equalTo(0L)),
                hasProperty("expectedCashback", equalTo(0L)),
                hasProperty("partnerProgramInfo", notNullValue())
        ));
        assertThat(response.getPartnerProgramInfo(), allOf(
                hasProperty("partnerProgramMaxReward", equalTo(50_000L)),
                hasProperty("partnerProgramLink", equalTo("https://aff.market.yandex.ru/influencers"))
        ));
    }

    @Test
    public void shouldReturnSamePromocodeForSameUser() {
        ReferralPromocodeResponse response1 = sendRequest(13L);
        assertThat(response1, allOf(
                hasProperty("refererPromoCode", notNullValue()),
                hasProperty("refererPromoCodeExpiredDate", notNullValue()),
                hasProperty("alreadyGot", equalTo(0L)),
                hasProperty("friendsOrdered", equalTo(0L)),
                hasProperty("expectedCashback", equalTo(0L)),
                hasProperty("partnerProgramInfo", notNullValue())
        ));
        assertThat(response1.getPartnerProgramInfo(), allOf(
                hasProperty("partnerProgramMaxReward", equalTo(50_000L)),
                hasProperty("partnerProgramLink", equalTo("https://aff.market.yandex.ru/influencers"))
        ));
        ReferralPromocodeResponse response2 = sendRequest(13L);
        assertThat(response2, allOf(
                hasProperty("refererPromoCode", equalTo(response1.getRefererPromoCode())),
                hasProperty("refererPromoCodeExpiredDate", equalTo(response1.getRefererPromoCodeExpiredDate())),
                hasProperty("alreadyGot", equalTo(response1.getAlreadyGot())),
                hasProperty("friendsOrdered", equalTo(response1.getFriendsOrdered())),
                hasProperty("expectedCashback", equalTo(response1.getExpectedCashback())),
                hasProperty("partnerProgramInfo", notNullValue())
        ));
        assertThat(response1.getPartnerProgramInfo(), allOf(
                hasProperty("partnerProgramMaxReward",
                        equalTo(response1.getPartnerProgramInfo().getPartnerProgramMaxReward())),
                hasProperty("partnerProgramLink", equalTo(response1.getPartnerProgramInfo().getPartnerProgramLink())
                )));
    }

    @Test
    public void shouldGenerateNewPromocodeAfterCurrentExpiring() {
        ReferralPromocodeResponse response1 = sendRequest(14L);
        assertThat(response1, allOf(
                hasProperty("refererPromoCode", notNullValue()),
                hasProperty("refererPromoCodeExpiredDate", notNullValue()),
                hasProperty("alreadyGot", equalTo(0L)),
                hasProperty("friendsOrdered", equalTo(0L)),
                hasProperty("expectedCashback", equalTo(0L)),
                hasProperty("partnerProgramInfo", notNullValue())
        ));
        clock.spendTime(8, ChronoUnit.DAYS);
        ReferralPromocodeResponse response2 = sendRequest(14L);
        assertThat(response2, allOf(
                hasProperty("alreadyGot", equalTo(response1.getAlreadyGot())),
                hasProperty("friendsOrdered", equalTo(response1.getFriendsOrdered())),
                hasProperty("expectedCashback", equalTo(response1.getExpectedCashback())),
                hasProperty("partnerProgramInfo", notNullValue())
        ));
        assertThat(response1.getPartnerProgramInfo(), allOf(
                hasProperty("partnerProgramMaxReward",
                        equalTo(response1.getPartnerProgramInfo().getPartnerProgramMaxReward())),
                hasProperty("partnerProgramLink", equalTo(response1.getPartnerProgramInfo().getPartnerProgramLink())
                )));
        assertThat(response2, allOf(
                hasProperty("refererPromoCode", not(equalTo(response1.getRefererPromoCode())))
                //TODO: включить после запуска в прод. В эксперименте даты окончания будут совпадать
                //hasProperty("refererPromoCodeExpiredDate", not(equalTo(response1.getRefererPromoCodeExpiredDate())))
        ));
    }

    @Test
    public void shouldReturnEmptyObjectForUnregisteredUser() {
        final long UNREGISTERED_UID = 1_152_921_504_606_846_980L;
        ReferralPromocodeResponse response = sendRequest(UNREGISTERED_UID);
        assertThat(response, allOf(
                hasProperty("refererPromoCode", nullValue()),
                hasProperty("refererPromoCodeExpiredDate", nullValue()),
                hasProperty("alreadyGot", nullValue()),
                hasProperty("friendsOrdered", nullValue()),
                hasProperty("expectedCashback", nullValue()),
                hasProperty("partnerProgramInfo", nullValue())
        ));
    }

    @Test
    public void shouldReturnCorrectInfoAboutReceivedReward() {
        final long uid = 15L;
        createUserAccruals(uid);
        ReferralPromocodeResponse response = sendRequest(uid);
        assertThat(response, allOf(
                hasProperty("refererPromoCode", notNullValue()),
                hasProperty("refererPromoCodeExpiredDate", notNullValue()),
                hasProperty("alreadyGot", equalTo(1600L)),
                hasProperty("friendsOrdered", equalTo(4L)),
                hasProperty("expectedCashback", equalTo(900L)),
                hasProperty("partnerProgramInfo", notNullValue())
        ));
    }

    @Test
    public void shouldReturnErrorOnConfigError() throws Exception {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_ACCRUAL, null);
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_PROMOCODE, nullValue());
        MockHttpServletRequestBuilder requestBuilder = get("/promocodes/referral/get/" + 17)
                .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(requestBuilder)
                .andExpect(status().is4xxClientError());
    }

    private void createUserAccruals(long uid) {
        Promo.PromoStatusWithBudget promoStatusWithBudget =
                referralProgramService.loadReferralPromo(configurationService.getReferralProgramAccrualPromoKey())
                .orElseThrow();
        var promoId = promoStatusWithBudget.getPromoId();
        List<UserAccrualsCacheEntry.AccrualStatusesWithAmount> accruals = new ArrayList<>();
        accruals.add(createUserAccrual(300, promoId, CONFIRMED));
        accruals.add(createUserAccrual(300, promoId, CONFIRMED));
        accruals.add(createUserAccrual(500, promoId, CONFIRMED));
        accruals.add(createUserAccrual(500, promoId, CONFIRMED));
        accruals.add(createUserAccrual(300, promoId, PENDING));
        accruals.add(createUserAccrual(300, promoId, IN_QUEUE));
        accruals.add(createUserAccrual(300, promoId, PROCESSED));
        accruals.add(createUserAccrual(300, promoId, CANCELLED));
        accruals.add(createUserAccrual(300, promoId, CONFIRMATION_ERROR));


        userAccrualsCacheDao.getOrInsert(uid, new OneTimeSupplier<>(() -> accruals));
    }

    private UserAccrualsCacheEntry.AccrualStatusesWithAmount createUserAccrual(long reward, long promoId,
                                                                               YandexWalletTransactionStatus status) {
        return new UserAccrualsCacheEntry.AccrualStatusesWithAmount(
                status, YandexWalletRefundTransactionStatus.NOT_QUEUED,
                BigDecimal.valueOf(reward), promoId, Timestamp.from(clock.instant())
        );
    }

    private ReferralPromocodeResponse sendRequest(long uid) {
        try {
            MockHttpServletRequestBuilder requestBuilder = get("/promocodes/referral/get/" + uid)
                    .contentType(MediaType.APPLICATION_JSON);
            String response = mockMvc.perform(requestBuilder)
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            return objectMapper.readValue(
                    response, ReferralPromocodeResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createPromosForReferralProgram() {
        Promo accrualPromo = promoManager.createAccrualPromo(PromoUtils.WalletAccrual.defaultModelAccrual()
                .setName(DEFAULT_ACCRUAL_PROMO_KEY)
                .setPromoKey(DEFAULT_ACCRUAL_PROMO_KEY)
                .setStartDate(Date.from(clock.instant()))
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("REFERRAL_CODE")
                        .setBudget(BigDecimal.valueOf(1000))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_ACCRUAL, accrualPromo.getPromoKey());
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_PROMOCODE, couponPromo.getPromoKey());
        promoStatusWithBudgetCacheService.reloadCache();
        referralPromosCache.loadPromos();
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_USE_PROMOCODES_POOL, true);
    }

    private void turnOnReferralProgram() {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_ENABLED, true);
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_MAX_REFERRER_REWARD, 3000);
        configurationService.set(ConfigurationService.PARTNER_PROGRAM_MAX_REWARD, 50000);
        configurationService.set(ConfigurationService.PARTNER_PROGRAM_LINK, "https://aff.market.yandex.ru/influencers");
    }
}
