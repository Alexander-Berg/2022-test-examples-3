package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.incident;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.market.tsum.telegrambot.bot.dao.entity.ChatTicketRelation;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author Alexey Sivolapov <a href="mailto:asivolapov@yandex-team.ru"></a>
 * @date 27/01/20
 */
@RunWith(MockitoJUnitRunner.class)
public class IncidentUnlinkCommandHandlerTest extends IncidentTestPreset {

    @InjectMocks
    protected IncidentUnlinkCommandHandler commandHandler;

    @Before
    public void setUp() {
        commandHandler.startrekIncidentQueueName = TEST_STARTREK_INC_QUEUE_NAME;
    }

    @Test
    public void executeCommand_notickets() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentUnlinkCommand.json");
        expectedException.expect(IncorrectUserInputException.class);
        expectedException.expectMessage("У текущего чата нет привязки к тикету");
        when(mockChatTicketRelationDao.getRelationByChatId(context.getUpdate().getMessage().getChatId())).thenReturn(null);

        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
    }

    @Test
    public void executeCommand_valid() throws Exception {
        CommandHandlerContext context = prepareContext("UpdateObjectWithIncidentUnlinkCommand.json");
        Long chatId = context.getUpdate().getMessage().getChatId();
        ChatTicketRelation relation = new ChatTicketRelation(chatId, TEST_STARTREK_INC_ISSUE, context.getPerson().getLogin());
        when(mockChatTicketRelationDao.getRelationByChatId(chatId)).thenReturn(relation);

        commandHandler.executeCommand(context.getBot(), context.getUpdate().getMessage(), context.getPerson());
        Mockito.verify(mockChatTicketRelationDao, times(1)).removeRelationByChatId(eq(chatId));
        Mockito.verify(context.getBot(), times(1)).replyOnMessageAndSetReplyTo(eq(context.getUpdate().getMessage()),
            contains(TEST_STARTREK_INC_ISSUE));
    }

}