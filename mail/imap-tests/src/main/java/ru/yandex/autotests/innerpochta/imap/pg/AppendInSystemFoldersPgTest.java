package ru.yandex.autotests.innerpochta.imap.pg;

import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.append.AppendMessages;
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
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getFilledMessageWithAttachFromEML;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getMessage;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;


@Aqua.Test
@Title("Команда APPEND. Системные папки")
@Features({ImapCmd.APPEND, "PG"})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Аппендим письма в системные папки")
@RunWith(Parameterized.class)
public class AppendInSystemFoldersPgTest extends BaseTest {
    private static Class<?> currentClass = AppendInSystemFoldersPgTest.class;


    public static final String PATH_TO_EML = "/messages/complicated_message.eml";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static TestMessage expected;
    @Parameterized.Parameter
    public String systemFolder;
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass)); //TODO без прода

    @Parameterized.Parameters(name = "system folder - {0}")
    public static Collection<Object[]> data() {
        return allSystemFolders();
    }

    @BeforeClass
    public static void prepareEml() throws Exception {
        expected = getFilledMessageWithAttachFromEML
                (AppendMessages.class.getResource(PATH_TO_EML).toURI());
    }

    @Test
    @Description("Аппендим тестовое сообщение в системные папки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("359")
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
    @ru.yandex.qatools.allure.annotations.TestCaseId("360")
    public void appendInSystemFolderWithSelectShouldSeeExist() throws Exception {
        imap.request(SelectRequest.select(systemFolder)).shouldBeOk();
        imap.request(append(systemFolder,
                literal(getMessage(expected)))).shouldBeOk()
                .existsShouldBe(1)
                .recentShouldBe(1);
    }

    @Test
    @Description("Аппендим тестовое сообщение дважды")
    public void testDoubleAppendResponses() throws Exception {
        String message = literal(getMessage(expected));

        Integer uidValidity = prodImap.request(select(systemFolder)).uidValidity();
        Integer uidNext = prodImap.request(select(systemFolder)).uidNext();
        imap.request(append(systemFolder, message)).shouldBeOk().uidvalidityShouldBe(uidValidity).uidShouldBe(uidNext);

        uidValidity = prodImap.request(select(systemFolder)).uidValidity();
        uidNext = prodImap.request(select(systemFolder)).uidNext();
        imap.request(append(systemFolder, message)).shouldBeOk().uidvalidityShouldBe(uidValidity).uidShouldBe(uidNext);
    }
}
