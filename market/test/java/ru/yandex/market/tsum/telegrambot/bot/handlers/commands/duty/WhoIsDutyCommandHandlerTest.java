package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.duty;

import java.util.Collections;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.core.duty.DutyManager;
import ru.yandex.market.tsum.telegrambot.TestObjectLoader;
import ru.yandex.market.tsum.telegrambot.bot.Bot;

import static org.mockito.AdditionalMatchers.and;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WhoIsDutyCommandHandlerTest {
    private static final String UPDATE_OBJECT_FILE_NAME = "bot/handlers/commands/duty/UpdateObjectForWhoIsDuty.json";

    private static final String INCIDENT_PERSON_OBJECT_FILE_NAME =
        "bot/handlers/commands/duty/PersonObjectForIncidentDuty.json";

    private static final String OPERATIONS_PERSON_OBJECT_FILE_NAME =
        "bot/handlers/commands/duty/PersonObjectForOperationsDuty.json";

    @InjectMocks
    private WhoIsDutyCommandHandler commandHandler;

    @Mock
    private DutyManager dutyManager;

    @Test
    public void handle() throws Exception {
        StaffPerson incidentPerson = TestObjectLoader.getTestUpdateObject(
            INCIDENT_PERSON_OBJECT_FILE_NAME, StaffPerson.class, TestObjectLoader.SerializerType.GSON);
        StaffPerson operationsPerson = TestObjectLoader.getTestUpdateObject(
            OPERATIONS_PERSON_OBJECT_FILE_NAME, StaffPerson.class, TestObjectLoader.SerializerType.GSON);

        when(dutyManager.getDutyPersonFromCalendar(eq(WhoIsDutyCommandHandler.DEPARTMENT_NAME),
            eq(WhoIsDutyCommandHandler.INCIDENT_DUTY_NAME),
            any(Date.class))).thenReturn(incidentPerson);
        when(dutyManager.getDutyPersonFromCalendar(eq(WhoIsDutyCommandHandler.DEPARTMENT_NAME),
            eq(WhoIsDutyCommandHandler.OPERATIONS_DUTY_NAME),
            any(Date.class))).thenReturn(operationsPerson);

        Update update = TestObjectLoader.getTestUpdateObject(UPDATE_OBJECT_FILE_NAME, Update.class,
            TestObjectLoader.SerializerType.JACKSON);

        Bot mockBot = mock(Bot.class);

        doReturn(null).when(mockBot).replyOnMessage(eq(update.getMessage()), anyString());
        StaffPerson sender = new StaffPerson(null, -1, null, Collections.emptyList(), null, null);
        commandHandler.handle(mockBot, update.getMessage(), sender);

        verify(mockBot, times(1)).replyOnMessage(eq(update.getMessage()),
            and(contains(incidentPerson.getLogin()), contains(operationsPerson.getLogin())));
    }
}