package ru.yandex.autotests.innerpochta.imap.append;


import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;

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
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getFilledMessageWithAttachFromEML;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getMessage;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateName;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.02.14
 * Time: 20:35
 */
@Aqua.Test
@Title("Команда APPEND. Пользовательские папки")
@Features({ImapCmd.APPEND})
@Stories(MyStories.USER_FOLDERS)
@Description("Аппендим рандомное письмо в пользовательские папки")
@RunWith(Parameterized.class)
public class AppendInUserFolder extends BaseTest {
    private static Class<?> currentClass = AppendInUserFolder.class;

    private static final String FILE_PATH = "/messages/complicated_message.eml";
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static TestMessage expected;
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String userFolder;

    public AppendInUserFolder(String userFolder) {
        this.userFolder = userFolder;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        expected = getFilledMessageWithAttachFromEML
                (AppendMessages.class.getResource(FILE_PATH).toURI());
    }

    @Parameterized.Parameters(name = "folder - {0}")
    public static Collection<Object[]> foldersForAppend() {
        return allKindsOfFolders();
    }

    @Test
    @Description("Аппендим сообщения в пользовательскую папку первого уровня")
    @ru.yandex.qatools.allure.annotations.TestCaseId("37")
    public void appendInUserFolder() throws Exception {
        imap.request(create(userFolder)).shouldBeOk();

        imap.request(append(userFolder,
                literal(getMessage(expected)))).shouldBeOk();
        prodImap.select().waitMsgs(userFolder, 1);
        //todo: добавить проверку соответствия отправленного и пришедшего письма
    }

    @Test
    @Description("Аппендим сообщения в пользовательскую папку второго уровня")
    @ru.yandex.qatools.allure.annotations.TestCaseId("38")
    public void appendInUserSubfolder() throws Exception {
        String parent = generateName();
        String child = newFolder(parent, userFolder).fullName();
        imap.request(create(child)).shouldBeOk();

        imap.request(append(child,
                literal(getMessage(expected)))).shouldBeOk();
        prodImap.select().waitMsgs(child, 1);
    }

    @Test
    @Title("Аппендим в несуществующую папку")
    @Description("Аппендим письмо/письма в несуществующую папку\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("40")
    public void appendInNotExistFolderShouldSeeNo() throws IOException, MessagingException {
        imap.list().shouldNotSeeFolder(userFolder);
        imap.request(append(userFolder,
                literal(getMessage(expected)))).shouldBeNo();
    }

    @Test
    @Title("Аппендим в несуществующую папку. Без синхронизации LIST")
    @Description("Аппендим письмо/письма в несуществующую папку\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("41")
    public void appendInNotExistFolderWithoutListShouldSeeNo() throws IOException, MessagingException {
        imap.request(append(userFolder,
                literal(getMessage(expected)))).shouldBeNo();
    }

    @Test
    @Issue("MPROTO-1627")
    @Title("Аппенд сообщения в пользовательскую папку после SELECT-а")
    @Description("Аппендим письмо в пользовательскую папки, должны увидеть EXIST и RECENT\n" +
            "(ответ команды NOOP)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("42")
    public void appendInSystemFolderWithSelectShouldSeeExist() throws Exception {
        imap.request(create(userFolder)).shouldBeOk();
        imap.request(select(userFolder)).shouldBeOk();

        imap.request(append(userFolder,
                literal(getMessage(expected)))).shouldBeOk()
                .existsShouldBe(1)
                .recentShouldBe(1);
    }
}
