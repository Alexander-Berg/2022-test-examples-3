package ru.yandex.market.common.util;

import com.google.common.base.Throwables;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.collections.Either;
import ru.yandex.market.common.report.model.ReportException;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class CommandWithFallbackTest {
    private final Callable<Integer> throwsImmediately = () -> {
        throw new ReportException("fail");
    };

    private final Callable<Integer> waitFor3secondsThenFail = () -> waitAndFail(3000);

    public final Callable<Integer> waitFor300millisThenReturn = () -> waitAndReturn(300, 3);

    public final Callable<Integer> waitFor2minutesThenReturn = () -> waitAndReturn(2 * 60 * 1000, 5);

    public final Callable<Integer> waitFor3secondsThenReturn = () -> waitAndReturn(3 * 1000, 7);

    @Test
    public void shouldReturnFirstResult() throws Exception {
        Either<Integer, List<ExecutionException>> execute = CommandWithFallback.withDefaults(waitFor300millisThenReturn, waitFor300millisThenReturn).execute();

        Assert.assertEquals(3, execute.asLeft().intValue());
    }

    @Test
    public void shouldReturnFallbackIfMainIsTooLong() throws Exception {
        Either<Integer, List<ExecutionException>> execute = CommandWithFallback.withDefaults(waitFor3secondsThenReturn, waitFor300millisThenReturn).execute();

        Assert.assertEquals(3, execute.asLeft().intValue());
    }

    @Test
    public void shouldReturnFallbackIfMainHasFailed() throws Exception {
        Either<Integer, List<ExecutionException>> execute = CommandWithFallback.withDefaults(waitFor3secondsThenFail, waitFor3secondsThenReturn).execute();

        Assert.assertEquals(7, execute.asLeft().intValue());
    }

    @Test
    public void shouldReturnFallbackIfMainFailsImmediately() throws Exception {
        Either<Integer, List<ExecutionException>> execute = CommandWithFallback.withDefaults(throwsImmediately, waitFor3secondsThenReturn).execute();

        Assert.assertEquals(7, execute.asLeft().intValue());
    }

    @Test
    public void shouldReturnListOfExceptionsIfBothAreFailed() throws Exception {
        Either<Integer, List<ExecutionException>> execute = CommandWithFallback.withDefaults(waitFor3secondsThenFail, waitFor3secondsThenFail).execute();

        Assert.assertEquals(2, execute.asRight().size());
    }

    @Test(expected = TimeoutException.class)
    public void shouldThrowTimeoutExceptionIfBothAreTakingTooLong() throws Exception {
        CommandWithFallback.withFastTimeout(waitFor2minutesThenReturn, waitFor2minutesThenReturn).execute();
    }

    @Test
    public void shouldSetBudgetsAndThrowTimeoutExceptionIfBothAreTakingTooLong() throws Exception {
        class BudgetHolder {
            public long budget = -1;
        }

        final BudgetHolder mainBudget = new BudgetHolder();
        final BudgetHolder fallbackBudget = new BudgetHolder();

        Exception thrown = null;

        try {
            CommandWithFallback.withFastTimeout(
                    (long budgetMs) -> {
                        mainBudget.budget = budgetMs;
                        Thread.sleep(120000);
                        return null;
                    },
                    (long budgetMs) -> {
                        fallbackBudget.budget = budgetMs;
                        Thread.sleep(120000);
                        return null;
                    }
            ).execute();
        } catch (Exception e) {
            thrown = e;
        }

        Assert.assertThat(mainBudget.budget, Matchers.not(Matchers.equalTo(-1)));
        Assert.assertThat(fallbackBudget.budget, Matchers.not(Matchers.equalTo(-1)));
        Assert.assertThat(fallbackBudget.budget, Matchers.not(Matchers.equalTo(mainBudget.budget)));
        Assert.assertThat(thrown, Matchers.notNullValue());

    }


    @Nonnull
    private static Integer waitAndReturn(int millis, int value) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Throwables.propagate(e);
        }
        return value;
    }

    private static Integer waitAndFail(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Throwables.propagate(e);
        }
        throw new ReportException("FAIL");
    }


}
