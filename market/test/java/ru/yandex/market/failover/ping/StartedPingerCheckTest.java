package ru.yandex.market.failover.ping;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.failover.FailoverState;
import ru.yandex.market.failover.FailoverTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.ping.CheckResult.Level.CRITICAL;

public class StartedPingerCheckTest extends PingerCheckTestCase<StartedPingerCheck> {
    protected FailoverState failoverState;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        failoverState = Mockito.mock(FailoverState.class);
        pingerCheck = new StartedPingerCheck();
        FailoverTestUtils.setPrivate(pingerCheck, "failoverState", failoverState);
    }

    @After
    public void tearDown() {
        verify(failoverState).isRunning();
        verifyNoMoreInteractions(failoverState);
    }

    @Test
    @Override
    public void testOk() {
        when(failoverState.isRunning()).thenReturn(true);
        assertEquals(CheckResult.OK, pingerCheck.check());
    }

    @Test
    public void testFail() {
        when(failoverState.isRunning()).thenReturn(false);
        CheckResult checkResult = pingerCheck.check();
        assertEquals(CRITICAL, checkResult.getLevel());
        assertEquals("Not running", checkResult.getMessage());
    }
}
