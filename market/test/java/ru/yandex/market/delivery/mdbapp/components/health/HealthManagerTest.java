package ru.yandex.market.delivery.mdbapp.components.health;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.delivery.mdbapp.components.health.components.AbstractServiceHealthChecker;
import ru.yandex.market.delivery.mdbapp.components.health.components.CheckouterChecker;
import ru.yandex.market.delivery.mdbapp.components.health.components.LogBrokerChecker;
import ru.yandex.market.delivery.mdbapp.components.health.components.MbiChecker;
import ru.yandex.market.delivery.mdbapp.components.health.components.MdbChecker;
import ru.yandex.market.delivery.mdbapp.components.health.components.QueueChecker;
import ru.yandex.market.delivery.mdbapp.components.health.components.ScheduledJobsCheckerWrapper;
import ru.yandex.market.delivery.mdbapp.components.health.components.ZooKeeperChecker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HealthManagerTest {

    private CheckouterChecker checkouterChecker = mock(CheckouterChecker.class);
    private ZooKeeperChecker zooKeeperChecker = mock(ZooKeeperChecker.class);
    private MbiChecker mbiChecker = mock(MbiChecker.class);
    private MdbChecker mdbChecker = mock(MdbChecker.class);
    private LogBrokerChecker logBrokerChecker = mock(LogBrokerChecker.class);
    private QueueChecker queueChecker = mock(QueueChecker.class);
    private ScheduledJobsCheckerWrapper scheduledJobsCheckerWrapper =
        mock(ScheduledJobsCheckerWrapper.class);
    private List<AbstractServiceHealthChecker> checkers = Arrays.asList(
        checkouterChecker,
        zooKeeperChecker,
        mbiChecker,
        logBrokerChecker,
        queueChecker,
        scheduledJobsCheckerWrapper
    );
    private ApplicationContext context = mock(ApplicationContext.class);
    private HealthManager healthManager;

    @Before
    public void setUp() {
        when(checkouterChecker.getCheckerId()).thenReturn(CheckouterChecker.CHECKER_ID);
        when(zooKeeperChecker.getCheckerId()).thenReturn(ZooKeeperChecker.CHECKER_ID);
        when(mbiChecker.getCheckerId()).thenReturn(MbiChecker.CHECKER_ID);
        when(logBrokerChecker.getCheckerId()).thenReturn(LogBrokerChecker.CHECKER_ID);
        when(queueChecker.getCheckerId()).thenReturn(QueueChecker.CHECKER_ID);
        when(scheduledJobsCheckerWrapper.getCheckerId()).thenReturn(ScheduledJobsCheckerWrapper.CHECKER_ID);
        healthManager = new HealthManager(mdbChecker, checkers, context);
    }

    @Test
    public void combineResultFromCheckListTest() {
        CheckResult expectedResult = new CheckResult(CheckResult.Level.CRITICAL, "Starting");
        Mockito.when(mdbChecker.getLastCheckResult()).thenReturn(expectedResult);
        CheckResult checkResult = healthManager.getLastCheckResult();

        assertEquals(
            "We assume that New created checkers has `Starting` message and CRITICAL (2) level.",
            expectedResult.toString(),
            checkResult.toString()
        );
    }

    @Test
    public void getCheckouterCheckResultTest() {
        CheckResult expectedResult = new CheckResult(CheckResult.Level.CRITICAL, "test message for " +
            CheckouterChecker.class.getSimpleName());
        Mockito.when(checkouterChecker.getLastCheckResult()).thenReturn(expectedResult);
        assertEquals(
            "We expect healthManager returns result of checkouterChecker.getLastCheckResult call here.",
            checkResultToString(expectedResult),
            healthManager.getCheckResult(CheckouterChecker.CHECKER_ID)
        );
    }

    @Test
    public void getMbiCheckerResultTest() {
        CheckResult expectedResult = new CheckResult(CheckResult.Level.CRITICAL, "test message for " +
            MbiChecker.class.getSimpleName());
        Mockito.when(mbiChecker.getLastCheckResult()).thenReturn(expectedResult);
        assertEquals(
            "We expect healthManager returns result of mbiChecker.getLastCheckResult call here.",
            checkResultToString(expectedResult),
            healthManager.getCheckResult(MbiChecker.CHECKER_ID)
        );
    }

    @Test
    public void getZooKeeperCheckerTest() {
        CheckResult expectedResult = new CheckResult(CheckResult.Level.CRITICAL, "test message for " +
            ZooKeeperChecker.class.getSimpleName());
        Mockito.when(zooKeeperChecker.getLastCheckResult()).thenReturn(expectedResult);
        assertEquals(
            "We expect healthManager returns result of zooKeeperChecker.getLastCheckResult call here.",
            checkResultToString(expectedResult),
            healthManager.getCheckResult(ZooKeeperChecker.CHECKER_ID)
        );
    }

    @Test
    public void getLogBrokerCheckerTest() {
        CheckResult expectedResult = new CheckResult(CheckResult.Level.CRITICAL, "test message for " +
            LogBrokerChecker.class.getSimpleName());
        Mockito.when(logBrokerChecker.getLastCheckResult()).thenReturn(expectedResult);
        assertEquals(
            "We expect healthManager returns result of LogBrokerChecker.getLastCheckResult call here.",
            checkResultToString(expectedResult),
            healthManager.getCheckResult(LogBrokerChecker.CHECKER_ID)
        );
    }

    @Test
    public void getScheduledJobLoggerCheckerTest() {
        CheckResult expectedResult = new CheckResult(CheckResult.Level.CRITICAL, "test message for " +
            ScheduledJobsCheckerWrapper.class.getSimpleName());
        Mockito.when(scheduledJobsCheckerWrapper.getLastCheckResult()).thenReturn(expectedResult);
        assertEquals(
            "We expect healthManager returns result of getLastCheckResult call here.",
            checkResultToString(expectedResult),
            healthManager.getCheckResult(ScheduledJobsCheckerWrapper.CHECKER_ID)
        );
    }

    private static String checkResultToString(CheckResult checkResult) {
        return checkResult.getLevel().ordinal() + ";" + checkResult.getMessage().trim();
    }
}
