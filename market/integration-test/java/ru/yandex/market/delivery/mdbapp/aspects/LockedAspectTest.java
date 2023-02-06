package ru.yandex.market.delivery.mdbapp.aspects;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.curator.managers.LockManager;

@DirtiesContext
public class LockedAspectTest extends AllMockContextualTest {
    @MockBean
    private LockManager lockManager;

    @MockBean
    private SimpleService simpleService;

    @Autowired
    private SimpleClassWithLock simpleClassWithLock;

    @Before
    public void setup() {
        Mockito.when(lockManager.canProceed(LockManager.Lock.DEFAULT)).thenReturn(true);
        Mockito.when(lockManager.canProceed(LockManager.Lock.DELIVERY_TARIFF)).thenReturn(false);
    }

    @Test
    public void testOpenLockLockAnnotation() {
        simpleClassWithLock.openLock();
        Mockito.verify(simpleService).handle();
    }

    @Test
    public void testCloseLockLockAnnotation() {
        simpleClassWithLock.closedLock();
        Mockito.verify(simpleService, Mockito.never()).handle();
    }
}
