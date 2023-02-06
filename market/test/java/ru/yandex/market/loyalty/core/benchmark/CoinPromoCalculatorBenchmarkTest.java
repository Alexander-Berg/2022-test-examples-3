package ru.yandex.market.loyalty.core.benchmark;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.ImmutableList;
import com.jamonapi.MonitorFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import ru.yandex.market.loyalty.core.config.CoreTestConfig;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.accounting.Account;
import ru.yandex.market.loyalty.core.model.accounting.AccountMatter;
import ru.yandex.market.loyalty.core.model.accounting.AccountType;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.MinOrderTotalCuttingRule;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.applicability.ApplicabilityPredicate;
import ru.yandex.market.loyalty.core.service.coin.CoinPromoCalculator;
import ru.yandex.market.loyalty.core.service.discount.PromoCalculationList;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.RulePayloads;

import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.service.discount.DiscountService.indexCoinsByType;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_WAREHOUSE_ID;

/**
 * Before all optimizations:
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.038 ± 0.002   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.080 ± 0.002   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.178 ± 0.022   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.404 ± 0.045   s/op
 * <p>
 * With items prefiltering:
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.033 ± 0.003   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.072 ± 0.007   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.154 ± 0.016   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.363 ± 0.035   s/op
 * <p>
 * With switch ItemPromoCalculation.getItemPromos() to Stream instead of intermediate collection:
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.031 ± 0.005   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.061 ± 0.006   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.131 ± 0.013   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.300 ± 0.029   s/op
 * <p>
 * Remove lambda (doesn't help)
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.028 ± 0.004   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.063 ± 0.010   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.141 ± 0.015   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.302 ± 0.033   s/op
 * <p>
 * Use getDiscounts in getDiscountsForItems
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.031 ± 0.005   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.069 ± 0.017   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.120 ± 0.012   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.275 ± 0.027   s/op
 * <p>
 * After merge
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.026 ± 0.003   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.056 ± 0.005   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.125 ± 0.014   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.257 ± 0.022   s/op
 * <p>
 * After merging Money calculations
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.025 ± 0.003   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.054 ± 0.007   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.117 ± 0.010   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.253 ± 0.024   s/op
 * <p>
 * Enable dead code in FairCalculator
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.049 ± 0.004   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.102 ± 0.010   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.229 ± 0.020   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.493 ± 0.042   s/op
 * <p>
 * Enable preliminary money calculations
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.048 ± 0.004   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.106 ± 0.010   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.226 ± 0.021   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.471 ± 0.043   s/op
 * <p>
 * Enable throwing exceptions from FairCalculator
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.053 ± 0.001   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.120 ± 0.014   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.253 ± 0.015   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.619 ± 0.037   s/op
 * <p>
 * For batch estimation
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt   Score    Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             1  avgt   20  ≈ 10⁻⁴            s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             2  avgt   20  ≈ 10⁻⁴            s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             3  avgt   20  ≈ 10⁻³            s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             4  avgt   20   0.001 ±  0.001   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             5  avgt   20   0.002 ±  0.001   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             6  avgt   20   0.002 ±  0.001   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             7  avgt   20   0.005 ±  0.001   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             8  avgt   20   0.011 ±  0.001   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop             9  avgt   20   0.037 ±  0.003   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20   0.084 ±  0.005   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20   0.184 ±  0.009   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20   0.402 ±  0.017   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20   0.535 ±  0.044   s/op
 * <p>
 * With naive coins splitting by 10
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.052 ± 0.004   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.116 ± 0.010   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.127 ± 0.022   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.118 ± 0.010   s/op
 * <p>
 * Extended benchmarking
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.072 ± 0.013   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.172 ± 0.022   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.169 ± 0.023   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.164 ± 0.026   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            14  avgt   20  0.178 ± 0.032   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            15  avgt   20  0.172 ± 0.024   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            16  avgt   20  0.195 ± 0.051   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            17  avgt   20  0.132 ± 0.004   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            18  avgt   20  0.131 ± 0.003   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            19  avgt   20  0.142 ± 0.008   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            20  avgt   20  0.157 ± 0.012   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            25  avgt   20  0.265 ± 0.021   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            30  avgt   20  0.280 ± 0.026   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            35  avgt   20  0.392 ± 0.011   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            40  avgt   20  0.412 ± 0.039   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            50  avgt   20  0.544 ± 0.046   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            60  avgt   20  0.680 ± 0.062   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            75  avgt   20  0.902 ± 0.088   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop           100  avgt   20  1.191 ± 0.104   s/op
 * <p>
 * Dynamically batch decreasing 10 -> 5
 * <p>
 * Benchmark                                               (coinsCount)  Mode  Cnt  Score   Error  Units
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            10  avgt   20  0.066 ± 0.005   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            11  avgt   20  0.137 ± 0.010   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            12  avgt   20  0.148 ± 0.027   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            13  avgt   20  0.127 ± 0.012   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            14  avgt   20  0.130 ± 0.010   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            15  avgt   20  0.126 ± 0.003   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            16  avgt   20  0.125 ± 0.002   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            17  avgt   20  0.133 ± 0.004   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            18  avgt   20  0.133 ± 0.006   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            19  avgt   20  0.138 ± 0.005   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            20  avgt   20  0.151 ± 0.004   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            25  avgt   20  0.191 ± 0.010   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            30  avgt   20  0.230 ± 0.017   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            35  avgt   20  0.224 ± 0.022   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            40  avgt   20  0.230 ± 0.020   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            50  avgt   20  0.241 ± 0.022   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            60  avgt   20  0.252 ± 0.022   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop            75  avgt   20  0.259 ± 0.022   s/op
 * CoinPromoCalculatorBenchmark.findFlashPromoDataForShop           100  avgt   20  0.282 ± 0.027   s/op
 */
@Threads(1)
@SuppressFBWarnings
@Ignore
public class CoinPromoCalculatorBenchmarkTest {

    @Test
    @Ignore
    public void runBenchmark() throws RunnerException {
        Options options = new OptionsBuilder()
                .include(CoinPromoCalculatorBenchmarkTest.class.getSimpleName())
                .forks(1)
                .jvmArgs("-Xmx512m", "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"))
                .build();
        new Runner(options).run();
    }

    @BenchmarkMode(Mode.AverageTime)
    @Benchmark
    @Measurement(timeUnit = TimeUnit.MILLISECONDS)
    public PromoCalculationList findFlashPromoDataForShop(CoinPromoCalculatorWrapper coinPromoCalculatorWrapper) {
        return coinPromoCalculatorWrapper.coinPromoCalculator.calculateItemCoins(
                PromoCalculationList.empty(),
                coinPromoCalculatorWrapper.uid,
                coinPromoCalculatorWrapper.items,
                coinPromoCalculatorWrapper.indexCoins,
                CoreMarketPlatform.BLUE,
                OperationContextFactory.uidOperationContext(),
                null,
                ApplicabilityPredicate.Utils.acceptAll(),
                RulePayloads.builder(SpendMode.SPEND).build(),
                null,
                coinPromoCalculatorWrapper.budgetAccounts,
                null,
                false,
                false
        );
    }

    @State(Scope.Benchmark)
    public static class CoinPromoCalculatorWrapper {
        private Map<CoreCoinType, List<Coin>> indexCoins;
        private Map<Long, Account> budgetAccounts;
        private List<Item> items;
        private GenericApplicationContext context;
        private CoinPromoCalculator coinPromoCalculator;
        private static final AtomicLong id = new AtomicLong();
        private static final BigDecimal HUNDRED = BigDecimal.valueOf(100L);
        private static final BigDecimal FIVE_HUNDRED = BigDecimal.valueOf(500L);
        private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000L);
        private final long uid = 1L;
        @Param({"10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "25", "30", "35", "40", "50", "60",
                "75", "100"})
        public int coinsCount;

        private static Pair<Coin, Account> createCoin(
                BigDecimal nominal, CoreCoinType coinType, BigDecimal minOrderTotal
        ) {
            long accountId = id.incrementAndGet();
            Account account = Account.builder()
                    .setId(accountId)
                    .setType(AccountType.ACTIVE)
                    .setBalance(MILLION)
                    .setMatter(AccountMatter.MONEY)
                    .setBudgetThreshold(BigDecimal.ZERO)
                    .setCanBeRestoredFromReserveBudget(false)
                    .build();
            long promoId = id.incrementAndGet();
            Coin.Builder coinBuilder = Coin.builder(new CoinKey(id.incrementAndGet()))
                    .setStatus(ACTIVE)
                    .setStartDate(new Date())
                    .setEndDate(DateUtils.addDays(new Date(), 1))
                    .setCreationDate(new Date())
                    .setActivationToken("")
                    .setUid(1L)
                    .setPromoId(promoId)
                    .setPlatform(CoreMarketPlatform.BLUE)
                    .setType(coinType)
                    .setPromoSubType(PromoSubType.MARKET_BONUS)
                    .setNominal(nominal);
            RulesContainer rulesContainer = new RulesContainer();
            if (minOrderTotal != null) {
                RuleContainer<MinOrderTotalCuttingRule> ruleContainer = RuleContainer
                        .builder(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE)
                        .withSingleParam(RuleParameterName.MIN_ORDER_TOTAL, minOrderTotal)
                        .build();
                rulesContainer.add(ruleContainer);
            }
            return Pair.of(
                    coinBuilder.setRulesContainer(rulesContainer)
                            .setPromoKey("")
                            .setBudgetAccountId(accountId)
                            .setSpendingAccountId(id.incrementAndGet())
                            .build(),
                    account
            );
        }

        @Setup(Level.Trial)
        public void setUp() {
            context = new AnnotationConfigApplicationContext(CoreTestConfig.class);
            coinPromoCalculator = context.getBean(CoinPromoCalculator.class);
            setupCoins(coinsCount);
            setupItems();

            MonitorFactory.reset();
        }

        private void setupItems() {
            items = ImmutableList.of(
                    createItem(FIVE_HUNDRED, BigDecimal.valueOf(3)),
                    createItem(HUNDRED, BigDecimal.valueOf(7)),
                    createItem(BigDecimal.ONE, BigDecimal.ONE)
            );
        }

        private static Item createItem(BigDecimal price, BigDecimal quantity) {
            return Item.Builder.create()
                    .withKey(ItemKey.ofFeedOffer(id.incrementAndGet(), Long.toString(id.incrementAndGet())))
                    .withPrice(price)
                    .withQuantity(quantity)
                    .withDownloadable(false)
                    .withHyperCategoryId((int) id.incrementAndGet())
                    .withSku(Long.toString(id.incrementAndGet()))
                    .withSsku(UUID.randomUUID().toString())
                    .withOldMinPrice(price)
                    .withVendorId(id.incrementAndGet())
                    .withPromoKeys(Collections.emptySet())
                    .withSupplierId(id.incrementAndGet())
                    .withWarehouseId(DEFAULT_WAREHOUSE_ID)
                    .withPayByYaPlus(0)
                    .build();
        }

        private void setupCoins(int count) {
            List<Pair<Coin, Account>> coinAccountPairs = new ArrayList<>();
            for (int i = 0; i < count; ++i) {
                coinAccountPairs.add(createCoin(HUNDRED, CoreCoinType.FIXED, null));
            }
            budgetAccounts = new HashMap<>();
            List<Coin> coins = new ArrayList<>();
            for (Pair<Coin, Account> coinAccountPair : coinAccountPairs) {
                Coin coin = coinAccountPair.getLeft();
                Account account = coinAccountPair.getRight();
                budgetAccounts.put(account.getId(), account);
                coins.add(coin);
            }

            indexCoins = indexCoinsByType(coins);
        }

        @TearDown(Level.Trial)
        public void print() {
            try {
                context.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                context = null;
                indexCoins = null;
                items = null;
                budgetAccounts = null;
            }
            System.out.println(MonitorFactory.getReport());
        }
    }
}
