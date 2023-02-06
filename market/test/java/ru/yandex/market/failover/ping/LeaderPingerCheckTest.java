package ru.yandex.market.failover.ping;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.failover.FailoverState;
import ru.yandex.market.failover.FailoverTestUtils;

import static org.mockito.Mockito.when;

public class LeaderPingerCheckTest extends PingerCheckTestCase<LeaderPingerCheck> {
    protected FailoverState failoverState;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        failoverState = Mockito.mock(FailoverState.class);
        pingerCheck = new LeaderPingerCheck();
        FailoverTestUtils.setPrivate(pingerCheck, "failoverState", failoverState);
    }

    @Test
    @Override
    public void testOk() {
        when(failoverState.isLeader()).thenReturn(true);
        Assert.assertEquals(CheckResult.OK, pingerCheck.check());
    }

    @Test
    public void testFail() {
        when(failoverState.isLeader()).thenReturn(false);
        CheckResult checkResult = pingerCheck.check();

        Assert.assertNotNull(checkResult);
        Assert.assertNotSame(CheckResult.OK, checkResult);
    }


}
