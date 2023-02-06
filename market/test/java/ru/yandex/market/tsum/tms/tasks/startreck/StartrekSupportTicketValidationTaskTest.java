package ru.yandex.market.tsum.tms.tasks.startreck;

import org.joda.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.commune.bazinga.scheduler.schedule.Schedule;
import ru.yandex.commune.bazinga.scheduler.schedule.ScheduleDelay;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class StartrekSupportTicketValidationTaskTest {

    @Mock
    SupportTicketMonitoring supportTicketMonitoring;

    @InjectMocks
    StartrekSupportTicketValidationTask validationTask;

    @Test
    public void verifySchedule() {
        ScheduleDelay expectedDelay = new ScheduleDelay(Duration.standardHours(1));
        Schedule schedule = validationTask.cronExpression();
        assertEquals(expectedDelay.getDelay(), ((ScheduleDelay) schedule).getDelay());
    }

    @Test
    public void testExecute_ensureMonitoringInvocation() throws Exception {
        ExecutionContext executionContextMock = Mockito.mock(ExecutionContext.class);
        validationTask.execute(executionContextMock);
        Mockito.verify(supportTicketMonitoring, Mockito.times(1))
            .fireMonitoringIfWeightlessSupportTicketsFound();
    }


}