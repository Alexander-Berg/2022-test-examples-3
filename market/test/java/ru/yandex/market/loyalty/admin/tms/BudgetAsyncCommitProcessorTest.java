package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.dao.accounting.DeferredMetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.exception.BudgetExceededException;
import ru.yandex.market.loyalty.core.model.accounting.AccountMatter;
import ru.yandex.market.loyalty.core.model.accounting.AccountType;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.TechnicalBudgetMode;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@TestFor(BudgetAsyncCommitProcessor.class)
public class BudgetAsyncCommitProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private BudgetAsyncCommitProcessor budgetAsyncCommitProcessor;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private DeferredMetaTransactionDao deferredMetaTransactionDao;
    @Autowired
    private MetaTransactionDao metaTransactionDao;

    @Test
    public void shouldProcessSingleAccountDeferredTransactions() {
        Budget budget = createBudget(1000);
        deferredSpend(budget, 10);
        deferredSpend(budget, 10);
        deferredSpend(budget, 10);
        deferredSpend(budget, 10);
        deferredSpend(budget, 10);
        checkAccountBalanceNotChanged(budget);

        budgetAsyncCommitProcessor.processDeferredTransactions(Duration.ofSeconds(10), 1);
        checkAccountBalanceDecreasedBy(budget, 50);
        assertThat(deferredMetaTransactionDao.getTransactions(100), is(empty()));
    }

    @Test
    public void shouldProcessSeveralAccountsDeferredTransactions() {
        Budget budget1 = createBudget(1000);
        Budget budget2 = createBudget(1000);

        deferredSpend(budget1, 100);
        deferredSpend(budget2, 200);
        checkAccountBalanceNotChanged(budget1);
        checkAccountBalanceNotChanged(budget2);

        budgetAsyncCommitProcessor.processDeferredTransactions(Duration.ofSeconds(10), 1);
        checkAccountBalanceDecreasedBy(budget1, 100);
        checkAccountBalanceDecreasedBy(budget2, 200);
        assertThat(deferredMetaTransactionDao.getTransactions(100), is(empty()));
    }

    @Test
    public void shouldAllowNegativeBalance() {
        Budget budget = createBudget(1000);
        deferredSpend(budget, 500);
        deferredSpend(budget, 500);
        deferredSpend(budget, 500);
        checkAccountBalanceNotChanged(budget);

        budgetAsyncCommitProcessor.processDeferredTransactions(Duration.ofSeconds(10), 1);
        checkAccountBalanceDecreasedBy(budget, 1500);
        assertThat(deferredMetaTransactionDao.getTransactions(100), is(empty()));
    }

    @Test
    public void shouldForceSyncBudgetModeForBudgetThreshold() {
        Budget budget = createBudget(1000, 400);

        deferredSpend(budget, 600, false);

        checkAccountBalanceNotChanged(budget);

        budgetAsyncCommitProcessor.processDeferredTransactions(Duration.ofSeconds(10), 1);
        checkAccountBalanceDecreasedBy(budget, 600);
        assertThat(deferredMetaTransactionDao.getTransactions(100), is(empty()));


        deferredSpend(budget, 100, false);
        checkAccountBalanceDecreasedBy(budget, 700);
    }

    @Test
    public void shouldForceSyncBudgetModeForSyncFactor() {
        Budget budget = createBudget(1000);

        deferredSpend(budget, 900, false);
        checkAccountBalanceNotChanged(budget);

        budgetAsyncCommitProcessor.processDeferredTransactions(Duration.ofSeconds(10), 1);
        checkAccountBalanceDecreasedBy(budget, 900);
        assertThat(deferredMetaTransactionDao.getTransactions(100), is(empty()));

        deferredSpend(budget, 100, false);
        checkAccountBalanceDecreasedBy(budget, 1000);
    }

    @Test(expected = BudgetExceededException.class)
    public void shouldForceSyncBudgetModeForBudgetThresholdAndMakeError() {
        Budget budget = createBudget(1000);

        deferredSpend(budget, 900, false);
        checkAccountBalanceNotChanged(budget);

        budgetAsyncCommitProcessor.processDeferredTransactions(Duration.ofSeconds(10), 1);
        checkAccountBalanceDecreasedBy(budget, 900);
        assertThat(deferredMetaTransactionDao.getTransactions(100), is(empty()));

        deferredSpend(budget, 101, false);
    }

    private Budget createBudget(int initialBudget) {
        return createBudget(initialBudget, null);
    }

    private Budget createBudget(int initialBudget, Integer budgetThreshold) {
        final long spendingAccountId = accountDao.createAccount(AccountType.PASSIVE, AccountMatter.MONEY, null, false);
        final long budgetAccountId = accountDao.createAccount(AccountType.ACTIVE, AccountMatter.MONEY,
                budgetThreshold != null ? BigDecimal.valueOf(
                budgetThreshold) : null, false);
        Budget budget = new Budget(spendingAccountId, budgetAccountId, initialBudget);
        addBalance(budgetAccountId, BigDecimal.valueOf(initialBudget));
        return budget;
    }

    private void deferredSpend(Budget budget, int amount) {
        deferredSpend(budget, amount, true);
    }

    private void deferredSpend(Budget budget, int amount, boolean isNegativeBudgetAllowed) {
        deferredSpend(
                budget.getBudgetAccountId(),
                budget.getSpendingAccountId(),
                BigDecimal.valueOf(amount),
                isNegativeBudgetAllowed
        );
    }

    private void checkAccountBalanceDecreasedBy(Budget budget, int expectedSpendAmount) {
        checkAccountBalanceDecreasedBy(
                budget.getBudgetAccountId(),
                budget.getSpendingAccountId(),
                budget.getInitialBudget(),
                expectedSpendAmount
        );
    }

    private void checkAccountBalanceNotChanged(Budget budget) {
        checkAccountBalanceNotChanged(
                budget.getBudgetAccountId(),
                budget.getSpendingAccountId(),
                budget.getInitialBudget()
        );
    }

    private void checkAccountBalanceDecreasedBy(long budgetAccountId, long spendingAccountId, int initialBudget,
                                                int expectedSpendAmount) {
        checkAccountBalance(budgetAccountId, spendingAccountId, initialBudget - expectedSpendAmount,
                expectedSpendAmount);
    }

    private void checkAccountBalanceNotChanged(
            long budgetAccountId, long spendingAccountId, int initialBudget
    ) {
        checkAccountBalance(budgetAccountId, spendingAccountId, initialBudget, 0);
    }

    private void checkAccountBalance(
            long budgetAccountId, long spendingAccountId, int remainingBudget, int spendBudget
    ) {
        assertThat(
                accountDao.getAccount(budgetAccountId).getBalance(),
                comparesEqualTo(BigDecimal.valueOf(remainingBudget))
        );
        assertThat(
                accountDao.getAccount(spendingAccountId).getBalance(),
                comparesEqualTo(BigDecimal.valueOf(spendBudget))
        );
    }

    private void deferredSpend(
            long budgetAccountId, long spendingAccountId, BigDecimal amountToMove, boolean isNegativeBudgetAllowed
    ) {
        long transactionId = metaTransactionDao.createTransaction(null, null, null);

        budgetService.performTransactions(
                Set.of(transactionId),
                amountToMove,
                budgetAccountId,
                spendingAccountId,
                isNegativeBudgetAllowed ? TechnicalBudgetMode.ASYNC_WITH_NEGATIVE_ALLOWED : BudgetMode.ASYNC,
                MarketLoyaltyErrorCode.BUDGET_EXCEEDED
        );
    }

    private void addBalance(long budgetAccountId, BigDecimal amountToMove) {
        budgetService.performSingleTransaction(
                amountToMove,
                accountDao.getTechnicalAccountId(AccountMatter.MONEY),
                budgetAccountId,
                BudgetMode.SYNC,
                MarketLoyaltyErrorCode.BUDGET_EXCEEDED
        );
    }

    private static class Budget {
        final long spendingAccountId;
        final long budgetAccountId;
        final int initialBudget;

        public Budget(long spendingAccountId, long budgetAccountId, int initialBudget) {
            this.spendingAccountId = spendingAccountId;
            this.budgetAccountId = budgetAccountId;
            this.initialBudget = initialBudget;
        }

        public long getSpendingAccountId() {
            return spendingAccountId;
        }

        public long getBudgetAccountId() {
            return budgetAccountId;
        }

        public int getInitialBudget() {
            return initialBudget;
        }
    }
}
