package ru.yandex.autotests.innerpochta.imap.append;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.data.TestData;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getFilledMessageWithAttachFromEML;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getMessage;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 21:45
 */
@Aqua.Test
@Title("Команда APPEND. Различные письма")
@Features({ImapCmd.APPEND})
@Stories({MyStories.COMMON, "#различные письма"})
@Description("Аппендим различные письма в inbox. Проверяем что корректно аппендятся. " +
        "Позитивные тесты")
@RunWith(Parameterized.class)
public class AppendMessages extends BaseTest {
    private static Class<?> currentClass = AppendMessages.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String file;

    public AppendMessages(String file) {
        this.file = file;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return TestData.testMessages();
    }

    @Test
    @Title("Аппенд сообщения в INBOX")
    @Description("Достаем из тестовых ресурсов eml-ку и аппендим во входящие. " +
            "После чего проверяем что в инбоксе есть ровно 1 письмо. (Перед каждым тестом - чистка ящика)\n" +
            "TODO: добавить проверку соответствия отправленного и пришедшего письма")
    @ru.yandex.qatools.allure.annotations.TestCaseId("43")
    public void appendInInboxMessagesTest() throws Exception {
        //аппендим письмо во входящие
        TestMessage expected = getFilledMessageWithAttachFromEML
                (AppendMessages.class.getResource(file).toURI());

        imap.request(append(Folders.INBOX,
                literal(getMessage(expected)))).shouldBeOk();
        //пока проверяем только, что сообщение дошло
        prodImap.select().waitMsgs(Folders.INBOX, 1);

//        imap.fetch().getSubject("1");

//        assertThat(imap, hasMessage("1", expected));
    }
}
