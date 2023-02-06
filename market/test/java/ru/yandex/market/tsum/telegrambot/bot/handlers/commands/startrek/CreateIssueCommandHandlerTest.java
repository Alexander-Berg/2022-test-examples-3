package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.startrek;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.telegrambot.TestObjectLoader;
import ru.yandex.market.tsum.telegrambot.bot.Bot;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Event;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import javax.validation.constraints.AssertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateIssueCommandHandlerTest {
    private static final String UPDATE_OBJECT_WITH_NEW_STARTRECK_ISSUE_COMMAND=
        "bot/handlers/commands/startrek/UpdateObjectWithNewStartreckIssueCommand.json";

    @InjectMocks
    public StartrekParseUtils startrekParseUtils;

    private Update update;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Session mockStartrekSession;

    @InjectMocks
    private CreateIssueCommandHandler commandHandler;

    @Before
    public void setUpTestObjects() throws Exception {
        update = TestObjectLoader.getTestUpdateObject(UPDATE_OBJECT_WITH_NEW_STARTRECK_ISSUE_COMMAND, Update.class,
            TestObjectLoader.SerializerType.JACKSON);
    }
    @Test
    public void test_getSummary() throws Exception {
        String summary =startrekParseUtils.getSummary(update.getMessage());
        String result = "TEST_ISSUE Issue header";
        Assert.assertEquals(summary,result);
    }

    @Test
    public void test_getQueueNameFromSummary() throws Exception {
        String summary =startrekParseUtils.getQueueNameFromSummary(update.getMessage());
        String result = "TEST_ISSUE";
        Assert.assertEquals(summary,result);
    }

    @Test
    public void test_getSummaryWithoutQueue() throws Exception {
        String summary =startrekParseUtils.getSummaryWithoutQueue(update.getMessage());
        String result = "Issue header";
        Assert.assertEquals(summary,result);
    }

    @Test
    public void test_getDescription() throws Exception {
        StaffPerson person = new StaffPerson(null, -1, null, null, null, null);
        String summary =startrekParseUtils.getDescription(update.getMessage(),person);
        String result = "issue body.\nmultiline body.";
        Assert.assertEquals(summary,result);
    }

    @Test
    public void handle_commandCreateIssue_WithQueueName() throws Exception {
        String testIssueKey = "TEST_ISSUE";

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


}
