package ru.yandex.market.tsum.tms.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import ru.yandex.market.tsum.tms.service.MonitoringSystemUpdateState.Condition;

import static org.junit.Assert.assertEquals;

public class MonitoringSystemUpdateStateTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private StaffGroupNotificationContext context = Mockito.mock(StaffGroupNotificationContext.class);
    private Exception exception = Mockito.mock(Exception.class);

    @Test
    public void getErrorMessage_failIfStateIsOk() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
            "Current state should have one of conditions TELEGRAPH_ERROR, JUGGLER_ERROR");
        MonitoringSystemUpdateState state = new MonitoringSystemUpdateState(context, Condition.OK, null);
        state.getErrorMessage();
    }

    @Test
    public void getErrorMessage_failOnErrorConditionButAbsentException() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
            "Monitoring system exception should be present");
        MonitoringSystemUpdateState state = new MonitoringSystemUpdateState(
            context,
            Condition.JUGGLER_ERROR, null
        );
        state.getErrorMessage();
    }

    @Test
    public void getErrorMessage_expectJugglerErrorMessage() {
        String jugglerError = "Some juggler error";
        Mockito.when(exception.getMessage()).thenReturn(jugglerError);
        MonitoringSystemUpdateState state = new MonitoringSystemUpdateState(
            context,
            Condition.JUGGLER_ERROR,
            exception
        );
        assertEquals(
            "Ошибка при переключении дежурного в Juggler: " + jugglerError,
            state.getErrorMessage()
        );
    }

    @Test
    public void getErrorMessage_expectTelegraphErrorMessage() {
        String telegraphError = "Some Telegraph error";
        Mockito.when(exception.getMessage()).thenReturn(telegraphError);
        MonitoringSystemUpdateState state = new MonitoringSystemUpdateState(
            context,
            Condition.TELEGRAPH_ERROR,
            exception
        );
        assertEquals(
            "Ошибка при переключении дежурного в Telegraph: " + telegraphError,
            state.getErrorMessage()
        );
    }
}