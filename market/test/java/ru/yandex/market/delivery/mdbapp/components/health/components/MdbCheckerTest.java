package ru.yandex.market.delivery.mdbapp.components.health.components;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.common.ping.ServiceInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MdbCheckerTest {
    private CheckouterChecker checkouterChecker = mock(CheckouterChecker.class);
    private ZooKeeperChecker zooKeeperChecker = mock(ZooKeeperChecker.class);
    private MbiChecker mbiChecker = mock(MbiChecker.class);
    private List<AbstractServiceHealthChecker> checkerList = Arrays.asList(
        checkouterChecker,
        zooKeeperChecker,
        mbiChecker
    );
    private MdbChecker mdbChecker;

    @Before
    public void setUp() {
        when(checkouterChecker.getCheckerId()).thenReturn(CheckouterChecker.CHECKER_ID);
        when(zooKeeperChecker.getCheckerId()).thenReturn(ZooKeeperChecker.CHECKER_ID);
        when(mbiChecker.getCheckerId()).thenReturn(MbiChecker.CHECKER_ID);
        mdbChecker = new MdbChecker(checkerList);
    }

    @Test
    public void checkAllChecksFailedTest() {
        addSubCheckers(CheckResult.Level.WARNING);
        CheckResult checkResult = mdbChecker.check();
        checkerList.forEach((AbstractServiceHealthChecker p) -> {
            verify(checkouterChecker).check();
            verify(checkouterChecker).getServiceInfo();
            verify(checkouterChecker).toShortString();
            verify(checkouterChecker, times(2)).getLastCheckResult();
            assertThat(
                "Checker name has expected to be in result message.",
                checkResult.getMessage(),
                CoreMatchers.containsString(checkouterChecker.getClass().getSimpleName())
            );
        });
    }

    @Test
    public void checkNoneChecksFailedTest() {
        addSubCheckers(CheckResult.Level.OK);
        CheckResult checkResult = mdbChecker.check();
        checkerList.forEach((AbstractServiceHealthChecker p) -> {
            verify(checkouterChecker).check();
            verify(checkouterChecker, never()).getServiceInfo();
            verify(checkouterChecker, never()).toShortString();
            verify(checkouterChecker, times(2)).getLastCheckResult();
        });
        assertEquals("We expected `0;OK` result here.", CheckResult.OK.toString(), checkResult.toString());
    }

    @Test
    public void checkMaxLevelChecksFailedTest() {
        addSubCheckers(CheckResult.Level.WARNING);
        reset(checkouterChecker);
        mockService(checkouterChecker, CheckResult.Level.CRITICAL, "test message");
        CheckResult checkResult = mdbChecker.check();
        assertEquals("We worse level in result.", CheckResult.Level.CRITICAL, checkResult.getLevel());
    }

    private void addSubCheckers(CheckResult.Level level) {
        checkerList.forEach((AbstractServiceHealthChecker p) -> {
            String message = "test message for " + p.getClass().getSimpleName();
            mockService(p, level, message);
        });
    }

    private void mockService(AbstractServiceHealthChecker checker, CheckResult.Level level, String message) {
        ServiceInfo info = new ServiceInfo(checker.getClass().getName(), "");
        Mockito.when(checker.check())
            .thenReturn(new CheckResult(level, message));
        Mockito.when(checker.getLastCheckResult())
            .thenReturn(new CheckResult(level, message));
        Mockito.when(checker.getServiceInfo())
            .thenReturn(info);
        Mockito.when(checker.toShortString())
            .thenReturn(level.ordinal() + ";" + message);
    }
}
