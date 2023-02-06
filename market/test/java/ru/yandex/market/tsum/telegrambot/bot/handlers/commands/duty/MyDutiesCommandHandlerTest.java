package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.duty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.telegrambot.TestObjectLoader;
import ru.yandex.market.tsum.telegrambot.bot.Bot;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MyDutiesCommandHandlerTest {
    private static final String UPDATE_OBJECT_FILE_NAME = "bot/handlers/commands/duty/UpdateObjectForMyDuties.json";

    private static final StringBuilder DEFAULT_DUTY_INFO = new StringBuilder("Вся неделя: support\n");

    @InjectMocks
    private MyDutiesCommandHandler commandHandler;

    @Mock
    private DutyInfoByPeriodGetter dutyInfoByPeriodGetter;

    @Test
    public void handle() throws Exception {
        Update update = TestObjectLoader.getTestUpdateObject(UPDATE_OBJECT_FILE_NAME, Update.class,
            TestObjectLoader.SerializerType.JACKSON);

        when(dutyInfoByPeriodGetter.getInfoAboutDutiesUntilEndOfWeek(any(LocalDateTime.class),
            any(String.class))).thenReturn(DEFAULT_DUTY_INFO);

        Bot mockBot = mock(Bot.class);

        doReturn(null).when(mockBot).replyOnMessage(eq(update.getMessage()), anyString());
        StaffPerson sender = new StaffPerson("", -1, null, Collections.emptyList(), null, null);
        commandHandler.handle(mockBot, update.getMessage(), sender);

        verify(mockBot, times(1)).replyOnMessage(eq(update.getMessage()),
            contains(DEFAULT_DUTY_INFO.toString()));
    }
}