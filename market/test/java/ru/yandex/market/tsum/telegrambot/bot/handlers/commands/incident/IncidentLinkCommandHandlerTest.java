package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.incident;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.telegrambot.bot.dao.entity.ChatTicketRelation;
import ru.yandex.startrek.client.error.EntityNotFoundException;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.Mockito.*;

/**
 * @author Alexey Sivolapov <a href="mailto:asivolapov@yandex-team.ru"></a>
 * @date 27/01/20
 */
@RunWith(MockitoJUnitRunner.class)
public class IncidentLinkCommandHandlerTest extends IncidentTestPreset {

    @InjectMocks
    protected IncidentLinkCommandHandler commandHandler;

    @Before
    public void setUp() {
        commandHandler.startrekIncidentQueueName = TEST_STARTREK_INC_QUEUE_NAME;
    }

    @Test
    public void executeCommand_emptyCommandBody() throws Exception {
        expectedException.expect(IncorrectUserInputException.class);
        expectedException.expectMessage("Необходимо указать номер тикета");
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentLinkEmptyBody.json");
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
    }

    @Test
    public void executeCommand_wrongIncidentQueueName() throws Exception {
        expectedException.expect(IncorrectUserInputException.class);
        expectedException.expectMessage("Допускаются только тикеты из очереди " + TEST_STARTREK_INC_QUEUE_NAME);
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentLinkWrongQueue.json");
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
    }

    @Test
    public void executeCommand_nonExistIssue() throws Exception {
        expectedException.expect(IncorrectUserInputException.class);
        expectedException.expectMessage("Прости, я не смог найти");
        when(mockStartrekSession.issues().get(any(String.class))).thenThrow(EntityNotFoundException.class);
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentLinkValid1.json");
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
    }

    @Test
    public void executeCommand_resolved_issue() throws Exception {
        expectedException.expect(IncorrectUserInputException.class);
        expectedException.expectMessage("Данный тикет уже");
        Issue issue = IssueBuilder.newBuilder(TEST_STARTREK_INC_ISSUE).setStatus("resolved").setDisplay("Закрыт").build();
        when(mockStartrekSession.issues().get(any(String.class))).thenReturn(issue);
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentLinkValid1.json");
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
    }

    @Test
    public void executeCommand_valid_nonAdmin() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentLinkValid1.json");
        Issue issue = IssueBuilder.newBuilder(TEST_STARTREK_INC_ISSUE).setStatus("opened").build();
        when(mockStartrekSession.issues().get(any(String.class))).thenReturn(issue);
        Long chatId = context.getUpdate().getMessage().getChatId();
        ChatTicketRelation relation = new ChatTicketRelation(chatId, TEST_STARTREK_INC_ISSUE, context.getPerson().getLogin());
        when(mockChatTicketRelationDao.getRelationByChatId(chatId)).thenReturn(relation);

        Message testMessage = new Message();
        doReturn(testMessage).when(context.getBot()).replyOnMessage(eq(context.getUpdate().getMessage()),
            anyString());
        doReturn(false).when(context.getBot()).isChatAdministrator(any(Chat.class));

        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());

        Mockito.verify(context.getBot(), times(1)).replyOnMessage(eq(context.getUpdate().getMessage()),
            contains("/" + TEST_STARTREK_INC_ISSUE));
        Mockito.verify(context.getBot(), times(1)).replyOnMessageAndSetReplyTo(eq(context.getUpdate().getMessage()),
            contains("я могу быть более полезным"));
    }

    @Test
    public void executeCommand_valid_admin() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentLinkValid1.json");
        Issue issue = mock(Issue.class);

        when(issue.getStatus()).thenReturn(IssueBuilder.newBuilder("TEST_STARTREK_INC_ISSUE").setStatus("opened").build().getStatus());

        when(mockStartrekSession.issues().get(any(String.class))).thenReturn(issue);

        Long chatId = context.getUpdate().getMessage().getChatId();
        ChatTicketRelation relation = new ChatTicketRelation(chatId, TEST_STARTREK_INC_ISSUE, context.getPerson().getLogin());
        when(mockChatTicketRelationDao.getRelationByChatId(chatId)).thenReturn(relation);

        Message botTelegramMessage = mock(Message.class);
        doReturn(botTelegramMessage).when(context.getBot()).replyOnMessage(eq(context.getUpdate().getMessage()),
            anyString());
        doReturn(true).when(context.getBot()).isChatAdministrator(any(Chat.class));
        when(context.getBot().getInviteLink(any())).thenReturn(TEST_INVITE_LINK);

        doReturn(null).when(botTelegramMessage).getPinnedMessage();
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());

        Mockito.verify(context.getBot(), times(1)).replyOnMessage(eq(context.getUpdate().getMessage()),
            contains("/" + TEST_STARTREK_INC_ISSUE));
        Mockito.verify(context.getBot(), times(1)).pinMessage(eq(botTelegramMessage));
        Mockito.verify(issue, times(1)).comment(contains(TEST_INVITE_LINK));
    }
}
