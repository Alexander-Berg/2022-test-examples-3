package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.loyalty.api.model.perk.PerkStat;
import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.web.LoyaltyTag;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.ydb.UserAccrualsCacheDao;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.model.ydb.UserAccrualsCacheEntry;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
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
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.REFERRAL_PROGRAM;

@TestFor(PerkController.class)
public class ReferralPerkTest extends MarketLoyaltyBackMockedDbTestBase {

    private final static long UNREGISTERED_UID = 1_152_921_504_606_846_978L;
    private final static long DEFAULT_UID = 553L;
    private final static long MSK_REGION = 213L;
    private final static String DEFAULT_ACCRUAL_PROMO_KEY = "accrual_promo";
    private final static String REFERRAL_REAR_FLAG = "rear";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    private UserAccrualsCacheDao userAccrualsCacheDao;
    @Autowired
    private ReferralProgramService referralProgramService;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    private ReferralProgramService.ReferralPromosCache referralPromosCache;


    @Test
    public void shouldReturnFalseToUnregisteredUser() {
        createPromosForReferralProgram();
        turnOnReferralProgram();
        PerkStatResponse perkStatResponse = sendReferralPerkRequest(UNREGISTERED_UID, false);
        assertThat(perkStatResponse.getStatuses().get(0).isPurchased(), equalTo(false));
    }

    @Test
    public void shouldEnableWithSwitcher() {
        createPromosForReferralProgram();
        turnOnReferralProgram();
        assertThat(sendReferralPerkRequest().getStatuses().get(0).isPurchased(), equalTo(true));
    }

    @Test
    public void shouldEnableWithRear() {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_REAR, REFERRAL_REAR_FLAG);
        createPromosForReferralProgram();
        PerkStatResponse perkStatResponse = sendReferralPerkRequest(DEFAULT_UID, true);
        assertThat(perkStatResponse.getStatuses().get(0).isPurchased(), equalTo(true));
    }

    @Test
    public void shouldDisableWithNoPromo() {
        turnOnReferralProgram();
        PerkStatResponse perkStatResponse = sendReferralPerkRequest();
        assertThat(perkStatResponse.getStatuses().get(0).isPurchased(), equalTo(false));
    }

    @Test
    public void shouldContainsRightParams() {
        turnOnReferralProgram();
        createPromosForReferralProgram();
        PerkStat perkStat = sendReferralPerkRequest().getStatuses().get(0);
        assertThat(perkStat, hasProperty("purchased", equalTo(true)));
        assertThat(perkStat, hasProperty("promocodeNominal", equalTo(500L)));
        assertThat(perkStat, hasProperty("refererReward", equalTo(300L)));
        assertThat(perkStat, hasProperty("maxRefererReward", equalTo(3000L)));
        assertThat(perkStat, hasProperty("isGotFullReward", equalTo(false)));
    }

    @Test
    public void shouldGotFullReward() {
        turnOnReferralProgram();
        createPromosForReferralProgram();
        createUserAccruals();
        PerkStat perkStat = sendReferralPerkRequest().getStatuses().get(0);
        assertThat(perkStat, hasProperty("purchased", equalTo(true)));
        assertThat(perkStat, hasProperty("promocodeNominal", equalTo(500L)));
        assertThat(perkStat, hasProperty("refererReward", equalTo(300L)));
        assertThat(perkStat, hasProperty("maxRefererReward", equalTo(3000L)));
        assertThat(perkStat, hasProperty("isGotFullReward", equalTo(true)));

        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_MAX_REFERRER_REWARD, 5000L);
        perkStat = sendReferralPerkRequest().getStatuses().get(0);
        assertThat(perkStat, hasProperty("purchased", equalTo(true)));
        assertThat(perkStat, hasProperty("promocodePercent", nullValue()));
        assertThat(perkStat, hasProperty("refererReward", equalTo(300L)));
        assertThat(perkStat, hasProperty("maxRefererReward", equalTo(5000L)));
        assertThat(perkStat, hasProperty("isGotFullReward", equalTo(false)));
        assertThat(perkStat, hasProperty("minOrderTotal", equalTo(5000L)));
    }

    @Test
    public void shouldDisabledWithExpiredPromo() {
        turnOnReferralProgram();
        createPromosForReferralProgram();
        PerkStat perkStat = sendReferralPerkRequest().getStatuses().get(0);
        assertThat(perkStat, hasProperty("purchased", equalTo(true)));

        clock.spendTime(8, ChronoUnit.DAYS);
        perkStat = sendReferralPerkRequest().getStatuses().get(0);
        assertThat(perkStat, hasProperty("purchased", equalTo(false)));
    }

    private void createUserAccruals() {
        BigDecimal reward = BigDecimal.valueOf(300);
        Promo.PromoStatusWithBudget promoStatusWithBudget =
                referralProgramService.loadReferralPromo(configurationService.getReferralProgramAccrualPromoKey())
                .orElseThrow();
        List<UserAccrualsCacheEntry.AccrualStatusesWithAmount> accruals = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            accruals.add(
                    new UserAccrualsCacheEntry.AccrualStatusesWithAmount(
                            YandexWalletTransactionStatus.CONFIRMED, YandexWalletRefundTransactionStatus.NOT_QUEUED,
                            reward, promoStatusWithBudget.getPromoId(), Timestamp.from(clock.instant())
                    )
            );
        }

        userAccrualsCacheDao.getOrInsert(DEFAULT_UID, new OneTimeSupplier<>(() -> accruals));
    }

    private PerkStatResponse sendReferralPerkRequest() {
        return sendReferralPerkRequest(DEFAULT_UID, false);
    }

    private PerkStatResponse sendReferralPerkRequest(long uid, boolean withRear) {
        try {
            MockHttpServletRequestBuilder requestBuilder = get("/perk/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .queryParam(LoyaltyTag.UID, Long.toString(uid))
                    .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                    .queryParam(LoyaltyTag.REGION_ID, Long.toString(MSK_REGION))
                    .queryParam(LoyaltyTag.PERK_TYPE, REFERRAL_PROGRAM.getCode());
            if (withRear) {
                requestBuilder = requestBuilder
                        .header("X-Market-Rearrfactors", REFERRAL_REAR_FLAG);
            }
            String response = mockMvc.perform(requestBuilder)
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            return objectMapper.readValue(
                    response, PerkStatResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createPromosForReferralProgram() {
        Promo accrualPromo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(300L))
                .setName(DEFAULT_ACCRUAL_PROMO_KEY)
                .setPromoKey(DEFAULT_ACCRUAL_PROMO_KEY)
                .setStartDate(Date.from(clock.instant()))
                .setEndDate(Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setBudget(BigDecimal.valueOf(100_000L))
        );
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setBudget(BigDecimal.valueOf(1000))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setCouponValue(BigDecimal.valueOf(500L), CoreCouponValueType.FIXED)
                        .addPromoRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                Set.of(BigDecimal.valueOf(5000L)))
        );
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_ACCRUAL, accrualPromo.getPromoKey());
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_PROMOCODE, couponPromo.getPromoKey());
        deferredMetaTransactionService.consumeBatchOfTransactions(10);
        promoStatusWithBudgetCacheService.reloadCache();
        referralPromosCache.loadPromos();
    }

    private void turnOnReferralProgram() {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_ENABLED, true);
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_MAX_REFERRER_REWARD, 3000);
    }
}
