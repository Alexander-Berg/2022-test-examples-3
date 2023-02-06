package ru.yandex.market.loyalty.core.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.TransactionDao;
import ru.yandex.market.loyalty.core.model.accounting.Account;
import ru.yandex.market.loyalty.core.model.accounting.AccountMatter;
import ru.yandex.market.loyalty.core.model.accounting.AccountType;
import ru.yandex.market.loyalty.core.model.accounting.TransactionEntry;
import ru.yandex.market.loyalty.core.model.budgeting.BudgetModeOverride;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.budgeting.BudgetModeService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredTransactionHooks;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.DiscountServiceTestingUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.ExceptionUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.core.utils.DiscountResponseUtil.hasNoErrors;
import static ru.yandex.market.loyalty.core.utils.DiscountServiceTestingUtils.discountRequest;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_CODE;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 14.06.17
 */
public class BudgetServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final BigDecimal INITIAL_BUDGET = BigDecimal.TEN;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private TransactionDao transactionDao;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private MetaTransactionDao metaTransactionDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private DiscountServiceTestingUtils discountServiceTestingUtils;
    @Autowired
    private DeferredTransactionHooks deferredTransactionHooks;
    @Autowired
    private BudgetModeService budgetModeService;

    private long technicalAccountId;

    @Before
    public void init() {
        technicalAccountId = accountDao.getTechnicalAccountId(AccountMatter.MONEY);
    }

    @Test
    public void checkBudget() {
        long accountId = createNewAccount(INITIAL_BUDGET);

        budgetService.checkBudget(INITIAL_BUDGET, accountId);
    }

    @Test
    public void checkBudgetExceeded() {
        long accountId = createNewAccount(INITIAL_BUDGET);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                budgetService.checkBudget(INITIAL_BUDGET.multiply(BigDecimal.valueOf(2)), accountId)
        );
        assertEquals(MarketLoyaltyErrorCode.BUDGET_EXCEEDED, exception.getMarketLoyaltyErrorCode());
    }

    @Repeat(5)
    @Test
    public void concurrentSpendWithHalfSuccess() throws Exception {
        List<Pair<Long, Long>> accountPairs = IntStream.range(0, CPU_COUNT / 2)
                .mapToObj(i -> Pair.of(createNewAccount(BigDecimal.ONE), createNewAccount(BigDecimal.ZERO)))
                .collect(Collectors.toList());
        testConcurrency(cpus -> accountPairs
                .stream()
                .flatMap(pair -> Stream.of(pair, pair))
                .<ExceptionUtils.RunnableWithException<Exception>>map(pair -> () -> {
                    try {
                        budgetService.performSingleTransaction(BigDecimal.ONE, pair.getLeft(), pair.getRight(),
                                BudgetMode.SYNC, MarketLoyaltyErrorCode.BUDGET_EXCEEDED);
                    } catch (MarketLoyaltyException e) {
                        assertEquals(MarketLoyaltyErrorCode.BUDGET_EXCEEDED, e.getMarketLoyaltyErrorCode());
                    }
                })
                .collect(Collectors.toList())
        );

        assertThat(accountPairs, not(empty()));
        for (Pair<Long, Long> accountPair : accountPairs) {
            assertThat(budgetService.getBalance(accountPair.getLeft()), comparesEqualTo(BigDecimal.ZERO));
            assertThat(budgetService.getBalance(accountPair.getRight()), comparesEqualTo(BigDecimal.ONE));
        }
    }

    @Repeat(5)
    @Test
    public void concurrentTransactionsInDifferentWays() throws Exception {
        List<Pair<Long, Long>> accountPairs = IntStream.range(0, CPU_COUNT / 2)
                .mapToObj(i -> Pair.of(createNewAccount(BigDecimal.valueOf(2)),
                        createNewAccount(BigDecimal.valueOf(4))))
                .collect(Collectors.toList());
        testConcurrency(cpus -> accountPairs
                .stream()
                .flatMap(pair -> Stream.<ExceptionUtils.RunnableWithException<Exception>>of(
                        () -> budgetService.performSingleTransaction(BigDecimal.ONE, pair.getLeft(), pair.getRight(),
                                BudgetMode.SYNC, MarketLoyaltyErrorCode.BUDGET_EXCEEDED),
                        () -> budgetService.performSingleTransaction(BigDecimal.valueOf(2), pair.getRight(),
                                pair.getLeft(), BudgetMode.SYNC, MarketLoyaltyErrorCode.BUDGET_EXCEEDED)
                ))
                .collect(Collectors.toList())
        );

        assertThat(accountPairs, not(empty()));
        for (Pair<Long, Long> accountPair : accountPairs) {
            assertThat(budgetService.getBalance(accountPair.getLeft()), comparesEqualTo(BigDecimal.valueOf(3)));
            assertThat(budgetService.getBalance(accountPair.getRight()), comparesEqualTo(BigDecimal.valueOf(3)));
        }
    }

    private long createNewAccount(BigDecimal budget) {
        long from = accountDao.createAccount(AccountType.ACTIVE, AccountMatter.MONEY, null, false);
        if (budget.compareTo(BigDecimal.ZERO) != 0) {
            budgetService.performSingleTransaction(budget, technicalAccountId, from, BudgetMode.SYNC,
                    MarketLoyaltyErrorCode.OTHER_ERROR);
        }
        return from;
    }

    @Test
    public void spend() {
        Pair<Long, Long> accounts = Pair.of(createNewAccount(INITIAL_BUDGET), createNewAccount(BigDecimal.ZERO));

        assertTrue(accountDao.isBalanceSufficient(INITIAL_BUDGET, accounts.getLeft()));

        long transactionId = metaTransactionDao.createEmptyTransaction();
        budgetService.spendReal(transactionId, INITIAL_BUDGET, accounts.getLeft(), accounts.getRight(), BudgetMode.SYNC,
                0L);

        TransactionEntry spendAccountTransaction = transactionDao.getTransactionRows(transactionId)
                .stream()
                .filter(tr -> tr.getAccountId() == accounts.getRight())
                .findFirst()
                .orElseThrow(BudgetServiceTest::transactionNotExists);
        assertThat(INITIAL_BUDGET, comparesEqualTo(spendAccountTransaction.getAmount()));
        assertFalse(accountDao.isBalanceSufficient(INITIAL_BUDGET, accounts.getLeft()));
    }

    @Test
    public void expiredBudgetOnTransaction() {
        long first = createNewAccount(BigDecimal.ZERO);
        long second = createNewAccount(BigDecimal.ZERO);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                budgetService.performSingleTransaction(BigDecimal.ONE, first, second, BudgetMode.SYNC,
                        MarketLoyaltyErrorCode.BUDGET_EXCEEDED)
        );
        assertEquals(MarketLoyaltyErrorCode.BUDGET_EXCEEDED, exception.getMarketLoyaltyErrorCode());

        exception = assertThrows(MarketLoyaltyException.class, () ->
                budgetService.performSingleTransaction(BigDecimal.ONE, second, first, BudgetMode.SYNC,
                        MarketLoyaltyErrorCode.BUDGET_EXCEEDED)
        );
        assertEquals(MarketLoyaltyErrorCode.BUDGET_EXCEEDED, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void checkSameMatter() {
        long from = accountDao.getTechnicalAccountId(AccountMatter.MONEY);
        long to = accountDao.createAccount(AccountType.ACTIVE, AccountMatter.PIECE, null, false);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                budgetService.performSingleTransaction(BigDecimal.ONE, from, to, BudgetMode.SYNC,
                        MarketLoyaltyErrorCode.BUDGET_EXCEEDED)
        );
        assertEquals("Нельзя сделать перевод с разными типами стока", exception.getMessage());
    }

    @Test
    public void revert() {
        Pair<Long, Long> accounts = Pair.of(createNewAccount(INITIAL_BUDGET), createNewAccount(BigDecimal.ZERO));

        long transactionId = metaTransactionDao.createEmptyTransaction();
        budgetService.spendReal(transactionId, INITIAL_BUDGET, accounts.getLeft(), accounts.getRight(), BudgetMode.SYNC,
                0L);

        TransactionEntry spendAccountTransaction = transactionDao.getTransactionRows(transactionId)
                .stream()
                .filter(tr -> tr.getAccountId() == accounts.getRight())
                .findFirst()
                .orElseThrow(BudgetServiceTest::transactionNotExists);
        assertThat(INITIAL_BUDGET, comparesEqualTo(spendAccountTransaction.getAmount()));

        assertFalse(accountDao.isBalanceSufficient(BigDecimal.ONE, accounts.getLeft()));

        long revertTransactionId = budgetService.revert(transactionId, Date.from(clock.instant()));

        TransactionEntry budgetAccountTransaction = transactionDao.getTransactionRows(revertTransactionId)
                .stream()
                .filter(tr -> tr.getAccountId() == accounts.getLeft())
                .findFirst()
                .orElseThrow(BudgetServiceTest::transactionNotExists);
        assertThat(INITIAL_BUDGET, comparesEqualTo(budgetAccountTransaction.getAmount()));

        spendAccountTransaction = transactionDao.getTransactionRows(revertTransactionId)
                .stream()
                .filter(tr -> tr.getAccountId() == accounts.getRight())
                .findFirst()
                .orElseThrow(BudgetServiceTest::transactionNotExists);
        assertThat(INITIAL_BUDGET.negate(), comparesEqualTo(spendAccountTransaction.getAmount()));

        assertTrue(accountDao.isBalanceSufficient(INITIAL_BUDGET, accounts.getLeft()));
    }

    @Repeat(5)
    @Test
    public void shouldNotUseRowsOrderOnRevert() {
        Pair<Long, Long> firstAccounts = Pair.of(createNewAccount(INITIAL_BUDGET), createNewAccount(BigDecimal.ZERO));
        Pair<Long, Long> secondAccounts = Pair.of(createNewAccount(INITIAL_BUDGET), createNewAccount(BigDecimal.ZERO));
        Pair<Long, Long> thirdAccounts = Pair.of(createNewAccount(INITIAL_BUDGET), createNewAccount(BigDecimal.ZERO));

        long transactionId = metaTransactionDao.createEmptyTransaction();
        budgetService.spendReal(transactionId, INITIAL_BUDGET, firstAccounts.getLeft(), firstAccounts.getRight(),
                BudgetMode.SYNC, 0L);
        budgetService.spendReal(transactionId, INITIAL_BUDGET, secondAccounts.getLeft(), secondAccounts.getRight(),
                BudgetMode.SYNC, 0L);
        budgetService.spendReal(transactionId, BigDecimal.valueOf(3), thirdAccounts.getLeft(),
                thirdAccounts.getRight(), BudgetMode.SYNC, 0L);

        shuffleTransactionIds(transactionId);

        budgetService.revert(transactionId, Date.from(clock.instant()));

        assertThat(accountDao.getAccount(firstAccounts.getLeft()).getBalance(), comparesEqualTo(INITIAL_BUDGET));
        assertThat(accountDao.getAccount(secondAccounts.getLeft()).getBalance(), comparesEqualTo(INITIAL_BUDGET));
        assertThat(accountDao.getAccount(thirdAccounts.getLeft()).getBalance(), comparesEqualTo(INITIAL_BUDGET));
    }

    @Repeat(5)
    @Test
    public void shouldRevertTransactionWithSameAccountAsActiveAndPassiveRoles() {
        long firstAccount = createNewAccount(INITIAL_BUDGET);
        long secondAccount = createNewAccount(INITIAL_BUDGET);
        long thirdAccount = createNewAccount(INITIAL_BUDGET);

        long transactionId = metaTransactionDao.createEmptyTransaction();
        budgetService.spendReal(transactionId, BigDecimal.ONE, firstAccount, secondAccount, BudgetMode.SYNC, 0L);
        budgetService.spendReal(transactionId, BigDecimal.ONE, secondAccount, thirdAccount, BudgetMode.SYNC, 0L);

        shuffleTransactionIds(transactionId);

        budgetService.revert(transactionId, Date.from(clock.instant()));

        assertThat(accountDao.getAccount(firstAccount).getBalance(), comparesEqualTo(INITIAL_BUDGET));
        assertThat(accountDao.getAccount(secondAccount).getBalance(), comparesEqualTo(INITIAL_BUDGET));
        assertThat(accountDao.getAccount(thirdAccount).getBalance(), comparesEqualTo(INITIAL_BUDGET));
    }


    @Test
    public void syncSpendIfAsyncBudgetModeIsForcedSync() {
        configurationService.set(ConfigurationService.BUDGET_MODE_OVERRIDE, BudgetModeOverride.FORCED_SYNC);

        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setStatus(PromoStatus.ACTIVE)
                .setBudgetMode(BudgetMode.ASYNC)
        );

        promoService.update(promo);
        budgetModeService.reloadCache();

        var discountResponse = discountServiceTestingUtils.spendDiscount(
                discountRequest().withCoupon(DEFAULT_COUPON_CODE).build()
        );

        assertThat(discountResponse, hasNoErrors());

        verify(deferredTransactionHooks, times(1))
                .shouldForceDeferredTransactions(
                        eq(false),
                        eq(promo.getBudgetAccountId()),
                        eq(promo.getSpendingAccountId())
                );
    }

    @Test
    public void asyncSpendIfAsyncBudgetModeIsForcedAsync() {
        configurationService.set(ConfigurationService.BUDGET_MODE_OVERRIDE, BudgetModeOverride.FORCED_ASYNC);

        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setStatus(PromoStatus.ACTIVE)
                .setBudgetMode(BudgetMode.SYNC)
        );

        promoService.update(promo);
        budgetModeService.reloadCache();

        var discountResponse = discountServiceTestingUtils.spendDiscount(
                discountRequest().withCoupon(DEFAULT_COUPON_CODE).build()
        );

        assertThat(discountResponse, hasNoErrors());

        verify(deferredTransactionHooks, times(1))
                .shouldForceDeferredTransactions(
                        eq(true),
                        eq(promo.getBudgetAccountId()),
                        eq(promo.getSpendingAccountId())
                );
    }

    @Test
    public void syncSpendIfAsyncBudgetModeIsAccordingToPromoAndPromoBudgetModeIsSync() {
        configurationService.set(ConfigurationService.BUDGET_MODE_OVERRIDE, BudgetModeOverride.ACCORDING_TO_PROMO);

        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setStatus(PromoStatus.ACTIVE)
                .setBudgetMode(BudgetMode.SYNC)
        );

        promoService.update(promo);
        budgetModeService.reloadCache();

        var discountResponse = discountServiceTestingUtils.spendDiscount(
                discountRequest().withCoupon(DEFAULT_COUPON_CODE).build()
        );

        assertThat(discountResponse, hasNoErrors());

        verify(deferredTransactionHooks, times(1))
                .shouldForceDeferredTransactions(
                        eq(false),
                        eq(promo.getBudgetAccountId()),
                        eq(promo.getSpendingAccountId())
                );
    }

    @Test
    public void asyncSpendIfAsyncBudgetModeIsAccordingToPromoAndPromoBudgetModeIsAsync() {
        configurationService.set(ConfigurationService.BUDGET_MODE_OVERRIDE, BudgetModeOverride.ACCORDING_TO_PROMO);

        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setStatus(PromoStatus.ACTIVE)
                .setBudgetMode(BudgetMode.ASYNC)
        );

        promoService.update(promo);
        budgetModeService.reloadCache();

        var discountResponse = discountServiceTestingUtils.spendDiscount(
                discountRequest().withCoupon(DEFAULT_COUPON_CODE).build()
        );

        assertThat(discountResponse, hasNoErrors());

        verify(deferredTransactionHooks, times(1))
                .shouldForceDeferredTransactions(
                        eq(true),
                        eq(promo.getBudgetAccountId()),
                        eq(promo.getSpendingAccountId())
                );
    }

    private void shuffleTransactionIds(long transactionId) {
        List<TransactionEntry> transactions = transactionDao.getTransactionRows(transactionId);
        Collections.shuffle(transactions);
        for (TransactionEntry transaction : transactions) {
            jdbcTemplate.update(
                    "INSERT INTO TRANSACTION(TRANSACTION_ID, ACCOUNT_ID, AMOUNT, CREATION_TIME) VALUES(?,?,?,?)",
                    transaction.getTransactionId(),
                    transaction.getAccountId(),
                    transaction.getAmount().movePointRight(2),
                    transaction.getCreationTime()
            );
            jdbcTemplate.update(
                    "DELETE from transaction where id = ?",
                    transaction.getId()
            );
        }
    }

    private static AssertionError transactionNotExists() {
        return new AssertionError("Transaction does not exist");
    }

    @Test
    public void shouldForceSyncBudgetOnInsufficientBalance() {
        Account from = new Account(0, AccountType.ACTIVE, BigDecimal.ZERO, AccountMatter.MONEY, BigDecimal.ONE, false);
        Account to = new Account(0, AccountType.ACTIVE, BigDecimal.TEN, AccountMatter.MONEY, BigDecimal.ONE, false);

        boolean res = BudgetService.shouldForceSyncBudget(from, to);

        assertTrue(res);
    }

    @Test
    public void shouldForceSyncBudgetOnSyncFactorReached() {
        Account from = new Account(0, AccountType.ACTIVE, BigDecimal.valueOf(100_000L), AccountMatter.MONEY,
                BigDecimal.valueOf(10_000L), false);
        Account to = new Account(0, AccountType.ACTIVE, BigDecimal.valueOf(10_000_000L), AccountMatter.MONEY,
                BigDecimal.ONE, false);

        boolean res = BudgetService.shouldForceSyncBudget(from, to);

        assertTrue(res);
    }

    @Test
    public void shouldForceSyncBudgetOnSyncFactorNotReached() {
        Account from = new Account(0, AccountType.ACTIVE, BigDecimal.valueOf(100_000L), AccountMatter.MONEY, BigDecimal.valueOf(10_000L), false);
        Account to = new Account(0, AccountType.ACTIVE, BigDecimal.valueOf(100_000L), AccountMatter.MONEY, BigDecimal.ONE, false);

        boolean res = BudgetService.shouldForceSyncBudget(from, to);

        assertFalse(res);
    }
}
