package ru.yandex.market.crm.core.schedule;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.mcrm.lock.LockService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ScheduleTestConfig.class, Tasks.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClusterSchedulingConfigurerTest {

    private static final String TASK_1_LOCK_KEY = "Tasks#task1";
    private static final String TASK_2_LOCK_KEY = "Tasks#task2";
    private static final String LONG_TASK_KEY = "Tasks#longTask";

    @Inject
    private LockService lockService;

    @Inject
    private ClusterSchedulingConfigurer clusterSchedulingConfigurer;

    @Inject
    private Tasks tasks;

    @Before
    public void before() {
        when(lockService.tryLock(TASK_1_LOCK_KEY)).thenReturn(true);
        when(lockService.tryLock(TASK_2_LOCK_KEY)).thenReturn(false);
        when(lockService.tryLock(LONG_TASK_KEY)).thenReturn(true);
    }

    @Test
    public void taskWasLockedAndUnlockedTest() throws InterruptedException {
        // ждем выполнения
        Thread.sleep(1000);

        verify(lockService, atLeastOnce()).tryLock(TASK_1_LOCK_KEY);
        verify(lockService, atLeastOnce()).freeLock(TASK_1_LOCK_KEY);

        assertTrue(tasks.task1Invoked);
    }

    @Test
    public void taskWasNotExecutedWithoutLockTest() {
        verify(lockService, atLeastOnce()).tryLock(TASK_2_LOCK_KEY);

        assertFalse(tasks.task2Invoked);
    }

    @Test
    public void longRunningTaskFreesLockAfterShutdownTest() throws InterruptedException {
        // ждем выполнения
        Thread.sleep(1000);

        verify(lockService, atLeastOnce()).tryLock(LONG_TASK_KEY);
        clusterSchedulingConfigurer.shutdown();
        verify(lockService, atLeastOnce()).freeLock(LONG_TASK_KEY);

        assertTrue(tasks.longTaskInvoked);
    }
}
