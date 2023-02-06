package ru.yandex.market.failover.ping;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.failover.FailoverTestUtils;
import ru.yandex.market.failover.selfcheck.SelfcheckHeartbeatService;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.ping.CheckResult.Level.CRITICAL;
import static ru.yandex.market.failover.ping.AllTasksRunningCheck.TASK_CLASSES;

public class AllTasksRunningCheckTest extends PingerCheckTestCase<AllTasksRunningCheck> {
    private SelfcheckHeartbeatService selfcheckHeartbeatService;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        selfcheckHeartbeatService = Mockito.mock(SelfcheckHeartbeatService.class);
        pingerCheck = new AllTasksRunningCheck();
        FailoverTestUtils.setPrivate(pingerCheck, "selfcheckHeartbeatService", selfcheckHeartbeatService);
    }

    @Test
    @Override
    public void testOk() {
        doAnswer(new TrueOnTaskClassAnswer()).
                when(selfcheckHeartbeatService).isAlive(any(Class.class));

        Assert.assertEquals(CheckResult.OK, pingerCheck.check());
    }

    @Test
    public void testFail() {
        for (Class taskClass : TASK_CLASSES) {
            assertFailIfTaskStopped(taskClass);
        }
    }

    @Test
    public void testFailOnAllTasksFailed() {
        when(selfcheckHeartbeatService.isAlive(any(Class.class))).thenReturn(false);
        CheckResult check = pingerCheck.check();
        Assert.assertEquals(CRITICAL, check.getLevel());
    }

    public void assertFailIfTaskStopped(final Class stoppedTaskClass) {
        doAnswer(new FalseOnSpecifiedClassAnswer(stoppedTaskClass)).
                when(selfcheckHeartbeatService).isAlive(any(Class.class));

        CheckResult check = pingerCheck.check();
        assertFailedResult(stoppedTaskClass, check);
    }

    public void assertFailedResult(Class stoppedTaskClass, CheckResult checkResult) {
        String msg = String.format("%s is not running", stoppedTaskClass.getSimpleName());
        Assert.assertEquals(CRITICAL, checkResult.getLevel());
        Assert.assertEquals(msg, checkResult.getMessage());
    }


    private static class FalseOnSpecifiedClassAnswer implements Answer<Boolean> {
        private final Class stoppedTaskClass;

        public FalseOnSpecifiedClassAnswer(Class stoppedTaskClass) {
            this.stoppedTaskClass = stoppedTaskClass;
        }

        @Override
        public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
            Object[] args = invocationOnMock.getArguments();
            return !args[0].equals(stoppedTaskClass);
        }
    }

    private class TrueOnTaskClassAnswer implements Answer<Boolean> {
        @Override
        public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
            Object[] args = invocationOnMock.getArguments();
            return FailoverTestUtils.in(args[0], TASK_CLASSES);
        }
    }
}
