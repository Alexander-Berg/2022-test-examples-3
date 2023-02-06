package ru.yandex.market.failover.tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.failover.FailoverState;
import ru.yandex.market.failover.FailoverTestUtils;
import ru.yandex.market.failover.InstanceManager;

/**
 * Проверка того, что starter в правильных случаях вызывает сервис запуска
 */
public class StarterInvocationsTest {
    private Starter starter;
    private InstanceManager instanceManager;
    private FailoverState failoverState;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        starter = Mockito.mock(Starter.class);
        instanceManager = Mockito.mock(InstanceManager.class);
        failoverState = Mockito.mock(FailoverState.class);
        FailoverTestUtils.setPrivate(starter, "instanceManager", instanceManager);
        FailoverTestUtils.setPrivate(starter, "failoverState", failoverState);
        Mockito.when(starter.runTask()).thenCallRealMethod();
    }

    @After
    public void tearDown() {
        Mockito.verify(starter).runTask();
        Mockito.verifyNoMoreInteractions(starter, instanceManager);
    }

    @Test
    public void shouldStartIfGoodUptime() {
        Mockito.when(starter.shouldStart()).thenReturn(true);
        Mockito.when(failoverState.isPoorUptime()).thenReturn(false);
        starter.runTask();

        Mockito.verify(starter).shouldStart();
        Mockito.verify(starter).logAction(Mockito.isA(String.class));
        Mockito.verify(instanceManager).start();
    }

    @Test
    public void shouldStopIfPoorUptime() {
        Mockito.when(starter.shouldStart()).thenReturn(true);
        Mockito.when(failoverState.isPoorUptime()).thenReturn(true);
        starter.runTask();

        Mockito.verify(starter).shouldStart();
        Mockito.verify(starter).logAction(Mockito.isA(String.class));
        Mockito.verify(failoverState).delayLeaderElection();
        Mockito.verify(instanceManager).stop();
    }

    @Test
    public void shouldStop() {
        Mockito.when(starter.shouldStart()).thenReturn(false);
        Mockito.when(starter.shouldStop()).thenReturn(true);
        starter.runTask();

        Mockito.verify(starter).shouldStart();
        Mockito.verify(starter).shouldStop();
        Mockito.verify(starter).logAction(Mockito.isA(String.class));
        Mockito.verify(instanceManager).stop();
    }

    @Test
    public void shouldDoNothing() {
        Mockito.when(starter.shouldStart()).thenReturn(false);
        Mockito.when(starter.shouldStop()).thenReturn(false);
        starter.runTask();

        Mockito.verify(starter).shouldStart();
        Mockito.verify(starter).shouldStop();
    }
}
