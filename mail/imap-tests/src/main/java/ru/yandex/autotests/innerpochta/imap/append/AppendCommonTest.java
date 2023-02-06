package ru.yandex.autotests.innerpochta.imap.append;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.utils.MessageUtils;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 27.02.14
 * Time: 16:38
 */
@Aqua.Test
@Title("Команда APPEND. Общий тест")
@Features({ImapCmd.APPEND})
@Stories(MyStories.COMMON)
@Description("Общие тесты на APPEND. Позитивное и негативное тестирование")
public class AppendCommonTest extends BaseTest {
    private static Class<?> currentClass = AppendCommonTest.class;


    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Description("Аппендим письмо без указания {}")
    @ru.yandex.qatools.allure.annotations.TestCaseId("31")
    public void appendWithoutOctetsShouldSeeBad() throws Exception {
        imap.request(append(Folders.INBOX, MessageUtils.getRandomMessage())).shouldBeBad();
    }

    @Test
    @Issue("MAILPROTO-2141")
    @Description("SELECT кириллической папки без энкодинга [MAILPROTO-2141]. Должны увидеть: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("32")
    public void appendCyrillicMessageShouldSeeBad() {
        imap.request(append(Folders.INBOX, literal(Utils.cyrillic()))).shouldBeBad()
                .statusLineContains(ImapResponse.COMMAND_SYNTAX_ERROR);
    }
}
