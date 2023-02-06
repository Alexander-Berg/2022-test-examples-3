package ru.yandex.market.failover;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.support.AbstractApplicationContext;

import ru.yandex.market.failover.selfcheck.SelfcheckHeartbeatService;

import static org.mockito.Mockito.mock;

public class PeriodicalSelfcheckTest {
    private static final int ALARM_DELAY = 5_000;
    private AtomicBoolean result = new AtomicBoolean(false);
    private Periodical periodical;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        periodical = new PeriodicalForTestWitchSelfcheck();

        periodical.selfcheckHeartbeatService = mock(SelfcheckHeartbeatService.class);
        periodical.setStartDelay(1_000);
        periodical.setRunPeriod(1_000);
        periodical.setAlarmDelay(ALARM_DELAY);
        FailoverTestUtils.setPrivate(periodical, "failoverThreadPriority", 8);
        FailoverTestUtils.setPrivate(periodical, "applicationContext", mock(AbstractApplicationContext.class));

        periodical.start();
    }

    @After
    public void tearDown() {
        periodical.stop();
        Mockito.verifyNoMoreInteractions(periodical.selfcheckHeartbeatService);
    }

    @Test
    public void testNoSelfcheckActionsBeforeRun() throws InterruptedException {
        // ничего не делаем. не стартовали - не должно быть проверок
    }

    @Test
    public void testRegisterAndBeatOnStart() {
        periodical.startTimer();
        Class<PeriodicalForTestWitchSelfcheck> taskClass = PeriodicalForTestWitchSelfcheck.class;

        Mockito.verify(periodical.selfcheckHeartbeatService).register(taskClass, ALARM_DELAY);
        Mockito.verify(periodical.selfcheckHeartbeatService).beat(taskClass);
    }

    private class PeriodicalForTestWitchSelfcheck extends Periodical {
        @Override
        protected boolean runTask() {
            return result.get();
        }

        @Override
        protected boolean autoHeartbeatMonitor() {
            return true;
        }
    }

}
