package ru.yandex.market.loyalty.core.dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.ReferralProgramPromocodesPoolEntry;
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
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.LoyaltyWithCurrentDatasourceTypeExecutorService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.WrappingExecutorService;

@Log4j2
public class ReferralProgramPromocodesPoolDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

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
    private ReferralProgramService.ReferralPromosCache referralPromosCache;
    private final WrappingExecutorService executor =
            LoyaltyWithCurrentDatasourceTypeExecutorService.wrap(
                    new ThreadPoolExecutor(
                            2,
                            Math.max(Runtime.getRuntime().availableProcessors(), 2),
                            60L,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(),
                            r -> new Thread(r, "FutureNotifyListener")
                    )
            );


    @Test
    public void shouldGetDifferentPromos() throws InterruptedException, ExecutionException {
        createPromosForReferralProgram();
        referralProgramService.generatePromocodeToPool(2);
        List<Future<Optional<ReferralProgramPromocodesPoolEntry>>> futures = executor.invokeAll(List.of(
                () -> referralProgramPromocodesPoolDao.getAndLockPromocode(clock.instant().plus(5, ChronoUnit.DAYS)),
                () -> referralProgramPromocodesPoolDao.getAndLockPromocode(clock.instant().plus(5, ChronoUnit.DAYS))
        ));
        for (Future<Optional<ReferralProgramPromocodesPoolEntry>> future : futures) {
            future.get().orElseThrow();
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void shouldThrowError() throws InterruptedException, ExecutionException {
        createPromosForReferralProgram();
        referralProgramService.generatePromocodeToPool(1);
        List<Future<Optional<ReferralProgramPromocodesPoolEntry>>> futures = executor.invokeAll(List.of(
                () -> referralProgramPromocodesPoolDao.getAndLockPromocode(clock.instant().plus(5, ChronoUnit.DAYS)),
                () -> referralProgramPromocodesPoolDao.getAndLockPromocode(clock.instant().plus(5, ChronoUnit.DAYS))
        ));
        for (Future<Optional<ReferralProgramPromocodesPoolEntry>> future : futures) {
            future.get().orElseThrow();
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
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_USE_PROMOCODES_POOL, true);
    }
}
