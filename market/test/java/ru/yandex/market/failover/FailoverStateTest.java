package ru.yandex.market.failover;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Тестируем логику failoverState.
 */
public class FailoverStateTest extends FailoverTestCase {
    private FailoverState failoverState;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        failoverState = initFailoverState();
    }


    @Test
    public void testIsRunning() {
        assertFalse(failoverState.isRunning());
        failoverState.setContextRunning(true);
        failoverState.setHttpRunning(true);
        assertTrue(failoverState.isRunning());
    }

    @Test
    public void testIsNotRunning() {
        failoverState.setContextRunning(true);
        failoverState.setContextRunning(false);
        assertFalse(failoverState.isRunning());
    }

    @Test
    public void testAllowedAcquireLeaderLock() {
        assertTrue(failoverState.isElectionAware());
    }

    @Test
    public void testNotAllowedAcquireLeaderLockOnElectionManuallyStopped() {
        failoverState.setNoLeaderElectionTill(System.currentTimeMillis() + 1_000_000);
        assertFalse(failoverState.isElectionAware());
    }

    @Test
    public void testJustBecameLeaderButNotStartedYetOnNotLeader() {
        assertFalse(failoverState.isLeaderButNotStartedInTime());
    }

    @Test
    public void testJustBecameLeaderButNotStartedYetOnJustStarted() {
        failoverState.setLeader(true);
        assertFalse(failoverState.isLeaderButNotStartedInTime());
    }
}
