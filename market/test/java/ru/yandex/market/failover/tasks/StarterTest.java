package ru.yandex.market.failover.tasks;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.failover.FailoverState;
import ru.yandex.market.failover.FailoverTestCase;
import ru.yandex.market.failover.FailoverTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StarterTest extends FailoverTestCase {
    private Starter starter;
    private FailoverState failoverState;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        starter = new Starter();
        failoverState = initFailoverState();
        FailoverTestUtils.setPrivate(starter, "failoverState", failoverState);
    }

    @Test
    public void testShouldStart() {
        failoverState.setLeader(true);
        failoverState.setContextRunning(false);
        failoverState.setHttpRunning(false);

        assertTrue(starter.shouldStart());
        assertFalse(starter.shouldStop());
    }

    @Test
    public void testShouldStopNonLeader() {
        failoverState.setLeader(false);
        failoverState.setContextRunning(true);
        failoverState.setHttpRunning(true);

        assertFalse(starter.shouldStart());
        assertTrue(starter.shouldStop());
    }

    @Test
    public void testShouldNotStopOrStartAlreadyRunning() {
        failoverState.setLeader(true);
        failoverState.setContextRunning(true);

        assertFalse(starter.shouldStart());
        assertFalse(starter.shouldStop());
    }

    @Test
    public void testShouldNotStopOrStartNonLeader() {
        failoverState.setLeader(false);
        failoverState.setContextRunning(false);

        assertFalse(starter.shouldStart());
        assertFalse(starter.shouldStop());
    }
}
