package ru.yandex.direct.libs.curator.lock;

import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.state.ConnectionState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

public class CuratorLockTest {
    private CuratorLock lock;
    @Mock
    private InterProcessLock internalLock;
    private int callbackCallCount;

    private void addCallbackCall() {
        callbackCallCount++;
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        lock = new CuratorLock(internalLock, this::addCallbackCall);
        callbackCallCount = 0;
    }

    @Test
    public void testCallBackCalled() {
        doReturn(true)
                .when(internalLock).isAcquiredInThisProcess();
        lock.stateChanged(null, ConnectionState.LOST);

        assertThat("Вызвали колбек", callbackCallCount, equalTo(1));
    }

    @Test
    public void testCallBackNotCalledWhenLockIsNotAcquired() {
        doReturn(false)
                .when(internalLock).isAcquiredInThisProcess();
        lock.stateChanged(null, ConnectionState.LOST);

        assertThat("Вызвали колбек", callbackCallCount, equalTo(0));
    }

    @Test
    public void testCallBackCalledOnlyOnce() {
        doReturn(true)
                .when(internalLock).isAcquiredInThisProcess();
        lock.stateChanged(null, ConnectionState.LOST);
        lock.stateChanged(null, ConnectionState.LOST);

        assertThat("Вызвали колбек только раз", callbackCallCount, equalTo(1));
    }

    @Test
    public void testCallBackCalledOnlyOnLost() {
        doReturn(true)
                .when(internalLock).isAcquiredInThisProcess();
        lock.stateChanged(null, ConnectionState.SUSPENDED);

        assertThat("Не вызвали колбек ни разу", callbackCallCount, equalTo(0));
    }
}
