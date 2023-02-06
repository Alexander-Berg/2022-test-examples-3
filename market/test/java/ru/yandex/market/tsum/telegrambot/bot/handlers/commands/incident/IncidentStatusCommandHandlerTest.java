package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.incident;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.telegrambot.bot.dao.entity.ChatTicketRelation;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.Mockito.*;

/**
 * @author Alexey Sivolapov <a href="mailto:asivolapov@yandex-team.ru"></a>
 * @date 27/01/20
 */
@RunWith(MockitoJUnitRunner.class)
public class IncidentStatusCommandHandlerTest extends IncidentTestPreset {
    @InjectMocks
    protected IncidentStatusCommandHandler commandHandler;

    @Before
    public void setUp() {
        commandHandler.startrekIncidentQueueName = TEST_STARTREK_INC_QUEUE_NAME;
    }

    @Test
    public void executeCommand_notickets() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentStatusEmptyBody.json");
        expectedException.expect(IncorrectUserInputException.class);
        expectedException.expectMessage("К данному чату не привязано тикетов");
        when(mockChatTicketRelationDao.getRelationByChatId(context.getUpdate().getMessage().getChatId())).thenReturn(null);
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
    }

    @Test
    public void executeCommand_ticketResolved() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentStatusEmptyBody.json");
        expectedException.expect(IncorrectUserInputException.class);
        expectedException.expectMessage("Связанный тикет уже");
        Long chatId = context.getUpdate().getMessage().getChatId();
        ChatTicketRelation relation = new ChatTicketRelation(chatId, TEST_STARTREK_INC_ISSUE, context.getPerson().getLogin());
        when(mockChatTicketRelationDao.getRelationByChatId(chatId)).thenReturn(relation);
        Issue issue = IssueBuilder.newBuilder(TEST_STARTREK_INC_ISSUE).setStatus("resolved").setDisplay("Закрыт").build();
        when(mockStartrekSession.issues().get(any(String.class))).thenReturn(issue);
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
    }

    @Test
    public void executeCommand_emptyCommandBody() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentStatusEmptyBody.json");
        expectedException.expect(IncorrectUserInputException.class);
        expectedException.expectMessage("Кажется ты забыл указать текст");
        Long chatId = context.getUpdate().getMessage().getChatId();
        ChatTicketRelation relation = new ChatTicketRelation(chatId, TEST_STARTREK_INC_ISSUE, context.getPerson().getLogin());
        when(mockChatTicketRelationDao.getRelationByChatId(chatId)).thenReturn(relation);
        Issue issue = IssueBuilder.newBuilder(TEST_STARTREK_INC_ISSUE).setStatus("opened").build();
        when(mockStartrekSession.issues().get(any(String.class))).thenReturn(issue);
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
    }

    @Test
    public void executeCommand_valid_nonadmin() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentStatusValid.json");
        Long chatId = context.getUpdate().getMessage().getChatId();
        ChatTicketRelation relation = new ChatTicketRelation(chatId, TEST_STARTREK_INC_ISSUE, context.getPerson().getLogin());
        when(mockChatTicketRelationDao.getRelationByChatId(chatId)).thenReturn(relation);
        Issue issue = mock(Issue.class);
        when(issue.getStatus()).thenReturn(IssueBuilder.newBuilder("TEST_STARTREK_INC_ISSUE").setStatus("opened").build().getStatus());
        when(mockStartrekSession.issues().get(any(String.class))).thenReturn(issue);
        doReturn(false).when(context.getBot()).isChatAdministrator(any(Chat.class));

        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());

        Mockito.verify(context.getBot(), times(1)).replyOnMessageAndSetReplyTo(eq(context.getUpdate().getMessage()),
            contains("Статус тикета обновлен"));

        Mockito.verify(issue, times(1)).comment(contains(context.getPerson().getLogin()));
    }

    @Test
    public void executeCommand_valid_admin() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentStatusValid.json");
        Long chatId = context.getUpdate().getMessage().getChatId();
        ChatTicketRelation relation = new ChatTicketRelation(chatId, TEST_STARTREK_INC_ISSUE, context.getPerson().getLogin());
        when(mockChatTicketRelationDao.getRelationByChatId(chatId)).thenReturn(relation);
        Issue issue = mock(Issue.class);
        when(issue.getStatus()).thenReturn(IssueBuilder.newBuilder("TEST_STARTREK_INC_ISSUE").setStatus("opened").build().getStatus());
        when(mockStartrekSession.issues().get(any(String.class))).thenReturn(issue);
        doReturn(true).when(context.getBot()).isChatAdministrator(any(Chat.class));
        when(context.getBot().getInviteLink(any())).thenReturn(TEST_INVITE_LINK);
        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());

        Mockito.verify(context.getBot(), times(1)).replyOnMessageAndSetReplyTo(eq(context.getUpdate().getMessage()),
            contains("Статус тикета обновлен"));

        Mockito.verify(issue, times(1)).comment(contains(TEST_INVITE_LINK));
        Mockito.verify(context.getBot(), times(1)).pinMessage(eq(context.getUpdate().getMessage()));
    }

}