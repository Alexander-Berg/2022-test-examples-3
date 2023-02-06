package ru.yandex.market.loyalty.core.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.log4j.Log4j2;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.api.model.AbstractOrder;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.OrderItemResponse;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.config.CacheForTests;
import ru.yandex.market.loyalty.core.config.CoreTestConfig;
import ru.yandex.market.loyalty.core.config.caches.DefaultCache;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.mock.Mocks;
import ru.yandex.market.loyalty.core.mock.Stubs;
import ru.yandex.market.loyalty.core.service.AutoLoadingCache;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoStatusWithBudgetCacheService;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;
import ru.yandex.market.loyalty.core.service.Top500UserCacheService;
import ru.yandex.market.loyalty.core.service.cashback.CashbackCacheService;
import ru.yandex.market.loyalty.core.service.cashback.CashbackDetailsGroupService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerFastEvaluatorService;
import ru.yandex.market.loyalty.core.stub.StubDao;
import ru.yandex.market.loyalty.core.utils.NoExternalCallsInTransactionChecker;
import ru.yandex.market.loyalty.core.utils.RefreshCachesEvent;
import ru.yandex.market.loyalty.lightweight.ExceptionUtils;
import ru.yandex.market.loyalty.test.database.CompositeSQLValidator;
import ru.yandex.market.loyalty.test.database.ResultSetAccounter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.reset;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 05.04.17
 */
@Log4j2
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = CoreTestConfig.class)
public abstract class MarketLoyaltyCoreMockedDbTestBase {
    protected static final int CPU_COUNT = 5;

    @Autowired
    private CacheLoaderState cacheLoaderState;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private DbConsistenceChecker dbConsistenceChecker;
    @Autowired
    private DbCleaner dbCleaner;
    @Autowired
    protected SupplementaryDataLoader supplementaryDataLoader;
    @Autowired
    protected ClockForTests clock;
    @Autowired
    protected MonitorCleaner monitorCleaner;
    @Autowired
    @DefaultCache
    protected CacheForTests cache;
    @Autowired
    private CompositeSQLValidator validator;
    @Autowired
    protected NoExternalCallsInTransactionChecker noExternalCallsInTransactionChecker;
    @Autowired
    protected Top500UserCacheService top500UserCacheService;

    @Autowired
    private TriggerFastEvaluatorService triggerFastEvaluatorService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private CashbackDetailsGroupService cashbackDetailsGroupService;
    @Autowired
    private RegionSettingsService regionSettingsService;
    @Autowired
    private List<AutoLoadingCache> caches;

    @Autowired
    @Mocks
    private Map<Object, Runnable> mocks;

    @Autowired
    @Stubs
    private List<StubDao> stubs;

    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    protected CashbackCacheService cashbackCacheService;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    ResultSetAccounter resultSetAccounter;
    @Autowired
    @Blackbox
    protected RestTemplate blackboxRestTemplate;

    @Before
    public void initMocks() throws InterruptedException {
        if (!cacheLoaderState.isCacheLoaded()) {
            eventPublisher.publishEvent(new RefreshCachesEvent(new Object()));
            cacheLoaderState.cacheIsLoaded();
        }

        cache.clear();
        monitorCleaner.clean();
        clock.reset();
        mocks.forEach((mock, init) -> {
            reset(mock);
            if (init != null) {
                init.run();
            }
        });
        stubs.forEach(StubDao::clear);
        noExternalCallsInTransactionChecker.clean();
        regionSettingsService.reloadCache();
        top500UserCacheService.reloadTop500Uids();
        configurationService.clear();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        BlackboxUtils.mockUserInfoResponse(blackboxRestTemplate);
    }

    @Before
    public void enableConfigParams() {
        configurationService.set(ConfigurationService.DELIVERY_EXTRA_CHARGE_CALCULATION_ENABLED, true);
        configurationService.set(ConfigurationService.CONSIDER_EXTRA_CHARGE_IN_DELIVERY_ZERO_PRICE_CALCULATION, true);
    }

    @After
    public void invalidateCaches() {
        triggerFastEvaluatorService.invalidateCaches();
        coinService.search.invalidateCaches();
        caches.forEach(AutoLoadingCache::clear);
    }

    protected static Set<String> getDiscountTokens(AbstractOrder<? extends OrderItemResponse> orderResponse) {
        return orderResponse.getItems().stream()
                .map(OrderItemResponse::getPromos)
                .flatMap(Collection::stream)
                .map(ItemPromoResponse::getDiscountToken)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Before
    public void prepareDatabase() {
        if (shouldClearDb()) {
            dbCleaner.clearDb();
        }
        if (shouldLoadTechnicalAccounts()) {
            supplementaryDataLoader.createTechnicalIfNotExists();
        }
        if (shouldCreateEmptyContext()) {
            supplementaryDataLoader.createEmptyOperationContext();
        }
        supplementaryDataLoader.populateCategoryTree();
        supplementaryDataLoader.populateFreeDeliveryAddresses();
        supplementaryDataLoader.createDefaultCashbackDetailsGroup();
        validator.startTest();
        resultSetAccounter.start();
    }

    @After
    public void clearDbAndCheckConsistency() {
        resultSetAccounter.stopAndCheck();

        try {
            if (shouldCheckConsistence()) {
                dbConsistenceChecker.checkMonitor();
            }
        } finally {
            if (shouldClearDb()) {
                dbCleaner.clearDb();
            }
        }
        assertThat(validator.finishTest(), is(empty()));
        noExternalCallsInTransactionChecker.check();
        ensureValidatorEnabled();
    }

    private void ensureValidatorEnabled() {
        validator.setEnabled(true);
    }

    protected boolean shouldCheckConsistence() {
        return true;
    }

    protected boolean shouldLoadTechnicalAccounts() {
        return true;
    }

    protected boolean shouldClearDb() {
        return true;
    }

    protected boolean shouldCreateEmptyContext() {
        return true;
    }

    public static AssertionError revertTokenNotFound() {
        return new AssertionError("Revert token is not found");
    }

    public static <E extends Throwable> void testConcurrency(
            Supplier<ExceptionUtils.RunnableWithException<E>> jobGenerator
    ) throws InterruptedException {
        testConcurrency(cpus -> Stream.generate(jobGenerator)
                .limit(cpus)
                .collect(Collectors.toList())
        );
    }

    public static <E extends Throwable> void testConcurrency(
            Function<Integer, List<ExceptionUtils.RunnableWithException<E>>> jobGenerator
    ) throws InterruptedException {
        testConcurrency(jobGenerator.apply(CPU_COUNT));
    }

    public static <E extends Throwable> void testConcurrency(
            List<ExceptionUtils.RunnableWithException<E>> jobs
    ) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CPU_COUNT);
        AtomicReference<Throwable> exception = new AtomicReference<>(null);
        assertThat(jobs.size(), lessThanOrEqualTo(CPU_COUNT));
        CyclicBarrier barrier = new CyclicBarrier(jobs.size());
        log.info("testConcurrency {} jobs on {} cpus", jobs.size(), CPU_COUNT);
        for (ExceptionUtils.RunnableWithException<E> job : jobs) {
            executor.submit(makeExceptionsUnchecked(() -> {
                barrier.await();
                try {
                    job.run();
                } catch (Throwable e) {
                    log.error("exception in job", e);
                    exception.set(e);
                }
            }));
        }
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
            executor.shutdownNow();
            fail();
        }
        if (exception.get() != null) {
            log.error("", exception.get());
            throw new AssertionError(
                    "jobsCount: " + jobs.size() + " errorMessage: " + exception.get().getMessage(),
                    exception.get()
            );
        }
    }

    protected void reloadPromoCache() {
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        promoStatusWithBudgetCacheService.reloadCache();
    }
}
