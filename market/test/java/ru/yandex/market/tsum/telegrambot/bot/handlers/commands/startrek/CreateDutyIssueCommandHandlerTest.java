package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.startrek;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.telegrambot.TestObjectLoader;
import ru.yandex.market.tsum.telegrambot.bot.Bot;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CreateDutyIssueCommandHandlerTest {
    private static final String UPDATE_OBJECT_FILE_NAME =
        "bot/handlers/commands/startrek/UpdateObjectWithCreateDutyIssueCommand.json";

    private static final String WRONG_UPDATE_OBJECT_FILE_NAME =
        "bot/handlers/commands/startrek/UpdateObjectWithWrongCreateDutyIssueCommand.json";

    @InjectMocks
    private CreateDutyIssueCommandHandler commandHandler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Session mockStartrekSession;

    @Test
    public void handle() throws Exception {
        String testIssueKey = "TEST_ISSUE";

        Update update = TestObjectLoader.getTestUpdateObject(UPDATE_OBJECT_FILE_NAME, Update.class,
            TestObjectLoader.SerializerType.JACKSON);

        Bot mockBot = mock(Bot.class);

        StaffPerson person = new StaffPerson(null, -1, null, null, null, null);

        Issue issue = IssueBuilder.newBuilder(testIssueKey).setStatus("open").build();

        when(mockStartrekSession.issues().create(any(IssueCreate.class))).thenReturn(issue);
        doReturn(null).when(mockBot).replyOnMessageAndSetReplyTo(eq(update.getMessage()),
            anyString());

        commandHandler.handle(mockBot, update.getMessage(), person);

        Mockito.verify(mockBot, times(1)).replyOnMessageAndSetReplyTo(eq(update.getMessage()),
            contains("/" + testIssueKey));
    }

    @Test
    public void handle_commandWithoutIssueDescription_responseWithHelpMessage() throws Exception {
        Update update = TestObjectLoader.getTestUpdateObject(WRONG_UPDATE_OBJECT_FILE_NAME, Update.class,
            TestObjectLoader.SerializerType.JACKSON);

        Bot mockBot = mock(Bot.class);

        doReturn(null).when(mockBot).replyOnMessage(eq(update.getMessage()),
            anyString());

        commandHandler.handle(
            mockBot,
            update.getMessage(),
            new StaffPerson(null, -1, null, null, null, null)
        );

        Mockito.verify(mockBot, times(1)).replyOnMessage(update.getMessage(),
            commandHandler.help());
    }
}