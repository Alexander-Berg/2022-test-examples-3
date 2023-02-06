package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.incident;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mock;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.telegrambot.TestObjectLoader;
import ru.yandex.market.tsum.telegrambot.bot.Bot;
import ru.yandex.market.tsum.telegrambot.bot.dao.ChatTicketRelationDao;
import ru.yandex.startrek.client.Session;

import java.io.IOException;

import static org.mockito.Mockito.mock;

/**
 * @author Alexey Sivolapov <a href="mailto:asivolapov@yandex-team.ru"></a>
 * @date 27/01/20
 */
public class IncidentTestPreset {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected Session mockStartrekSession;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected ChatTicketRelationDao mockChatTicketRelationDao;

    protected static final String TEST_USER_STAFF_LOGIN = "test_login";
    protected static final String TEST_STARTREK_INC_QUEUE_NAME = "TESTMARKETINC";
    protected static final String TEST_STARTREK_INC_ISSUE = "TESTMARKETINC-1";
    protected static final String TEST_INVITE_LINK = "https://telegram.test.invite.link/123";
    protected static final String UPDATE_OBJECTS_FOLDER_PATH = "bot/handlers/commands/incident/";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected CommandHandlerContext prepareContext(String updateJsonFileName) throws IOException {
        CommandHandlerContext result = new CommandHandlerContext();
        result.setBot(mock(Bot.class));
        result.setPerson(new StaffPerson(TEST_USER_STAFF_LOGIN, -1, null, null, null, null));
        result.setUpdate(TestObjectLoader.getTestUpdateObject(UPDATE_OBJECTS_FOLDER_PATH + updateJsonFileName, Update.class,
            TestObjectLoader.SerializerType.JACKSON));
        return result;
    }

}
