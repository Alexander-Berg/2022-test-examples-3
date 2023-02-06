package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.ReferralProgramPromocodesPoolDao;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoStatusWithBudgetCacheService;
import ru.yandex.market.loyalty.core.service.promocode.ReferralProgramService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.REFERRAL_PROGRAM_INIT_POOL_SIZE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.REFERRAL_PROGRAM_USE_PROMOCODES_POOL;

@TestFor(ReferralPoolWatcherProcessor.class)
public class ReferralPoolWatcherTest extends MarketLoyaltyAdminMockedDbTest {

    private final static String DEFAULT_ACCRUAL_PROMO_KEY = "accrual_promokey";
    @Autowired
    private ReferralProgramPromocodesPoolDao referralProgramPromocodesPoolDao;
    @Autowired
    private ReferralProgramService referralProgramService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    private ReferralPoolWatcherProcessor referralPoolWatcher;
    @Autowired
    private ReferralProgramService.ReferralPromosCache referralPromosCache;


    @Test
    public void shouldInitCorrect() {
        configurationService.set(REFERRAL_PROGRAM_INIT_POOL_SIZE, 100_000);
        createPromosForReferralProgram();
        referralPoolWatcher.checkReferralPoolSize();
        long notLockedCount = referralProgramPromocodesPoolDao.countByLockedUntilIsNull();
        assertThat(notLockedCount, equalTo(configurationService.getReferralProgramInitPoolSize()));
    }

    @Test
    public void shouldInitCorrectWithCornerCases1() {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_INIT_POOL_SIZE, 900);
        createPromosForReferralProgram();
        referralPoolWatcher.checkReferralPoolSize();
        long notLockedCount = referralProgramPromocodesPoolDao.countByLockedUntilIsNull();
        assertThat(notLockedCount, equalTo(configurationService.getReferralProgramInitPoolSize()));
    }

    @Test
    public void shouldInitCorrectWithCornerCases2() {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_INIT_POOL_SIZE, 1100);
        createPromosForReferralProgram();
        referralPoolWatcher.checkReferralPoolSize();
        long notLockedCount = referralProgramPromocodesPoolDao.countByLockedUntilIsNull();
        assertThat(notLockedCount, equalTo(configurationService.getReferralProgramInitPoolSize()));
    }

    @Test
    public void shouldIncreasePoolSizeOnTenPercentUsage() {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_INIT_POOL_SIZE, 100);

        createPromosForReferralProgram();
        referralPoolWatcher.checkReferralPoolSize();
        assertThat(referralProgramPromocodesPoolDao.countByLockedUntilIsNull(), equalTo(100L));
        assertThat(referralProgramPromocodesPoolDao.countByLockedUntilIsNotNull(), equalTo(0L));

        for (int i = 0; i < 15; i++) {
            referralProgramService.getOrCreateUserCurrentReferralPromocode((long) i);
        }
        assertThat(referralProgramPromocodesPoolDao.countByLockedUntilIsNotNull(), equalTo(15L));
        referralPoolWatcher.checkReferralPoolSize();
        assertThat(
                referralProgramPromocodesPoolDao.countByLockedUntilIsNull() +
                        referralProgramPromocodesPoolDao.countByLockedUntilIsNotNull(),
                greaterThanOrEqualTo(150L));

    }

    @Test
    public void shouldUnlockCodesAfterExpirationTime() {
        shouldIncreasePoolSizeOnTenPercentUsage();
        clock.spendTime(40, ChronoUnit.DAYS);
        referralPoolWatcher.unlockReferralPool();
        assertThat(referralProgramPromocodesPoolDao.countByLockedUntilIsNotNull(),
                equalTo(0L));
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
        configurationService.set(REFERRAL_PROGRAM_USE_PROMOCODES_POOL, true);
    }
}
