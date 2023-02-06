package ru.yandex.autotests.innerpochta.imap.append;

import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.SelectRequest;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getFilledMessageWithAttachFromEML;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getMessage;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.02.14
 * Time: 18:33
 */
@Aqua.Test
@Title("Команда APPEND. Системные папки")
@Features({ImapCmd.APPEND})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Аппендим письма в системные папки")
@RunWith(Parameterized.class)
public class AppendInSystemFolder extends BaseTest {
    private static Class<?> currentClass = AppendInSystemFolder.class;

    public static final String PATH_TO_EML = "/messages/complicated_message.eml";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static TestMessage expected;
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass)); //TODO без прода
    private String systemFolder;

    public AppendInSystemFolder(String systemFolder) {
        this.systemFolder = systemFolder;
    }

    @BeforeClass
    public static void prepareEml() throws Exception {
        expected = getFilledMessageWithAttachFromEML
                (AppendMessages.class.getResource(PATH_TO_EML).toURI());
    }

    @Parameterized.Parameters(name = "system folder - {0}")
    public static Collection<Object[]> data() {
        return allSystemFolders();

    }

    @Test
    @Description("Аппендим тестовое сообщение в системные папки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("34")
    public void testAppendInSystemFolders() throws Exception {
        imap.request(append(systemFolder,
                literal(getMessage(expected)))).shouldBeOk();
        prodImap.select().waitMsgs(systemFolder, 1);
        //todo: добавить проверку соответствия отправленного и пришедшего письма
    }

    @Test
    @Issue("MPROTO-1627")
    @Title("Аппенд сообщения в системную папку после SELECT-а")
    @Description("Аппендим письмо в системные папки, должны увидеть EXIST и RECENT\n" +
            "(ответ команды NOOP)")
    //TODO не проверял
    @ru.yandex.qatools.allure.annotations.TestCaseId("36")
    public void appendInSystemFolderWithSelectShouldSeeExist() throws Exception {
        imap.request(SelectRequest.select(systemFolder)).shouldBeOk();
        imap.request(append(systemFolder,
                literal(getMessage(expected)))).shouldBeOk()
                .existsShouldBe(1)
                .recentShouldBe(1);
    }

    @Test
    @Description("Аппендим тестовое сообщение дважды")
    @ru.yandex.qatools.allure.annotations.TestCaseId("681")
    public void testDoubleAppendResponses() throws Exception {
        String message = literal(getMessage(expected));
        imap.request(append(systemFolder, message)).shouldBeOk();
        imap.request(append(systemFolder, message)).shouldBeOk();
    }
}
