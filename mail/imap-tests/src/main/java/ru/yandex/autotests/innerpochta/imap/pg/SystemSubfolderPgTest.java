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
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.sun.mail.imap.protocol.BASE64MailboxEncoder.encode;
import static java.util.Arrays.asList;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.requests.CopyRequest.copy;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.MoveRequest.move;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getFilledMessageWithAttachFromEML;
import static ru.yandex.autotests.innerpochta.imap.utils.MessageUtils.getMessage;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateName;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

@Aqua.Test
@Title("Команда COPY. Системные папки")
@Features({ImapCmd.COPY, "PG"})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Копируем письма в системные подпапки")
@RunWith(Parameterized.class)
public class SystemSubfolderPgTest extends BaseTest {
    private static Class<?> currentClass = SystemSubfolderPgTest.class;

    public static final String PATH_TO_EML = "/messages/complicated_message.eml";
    private static final int LEVEL_OF_HIERARCHY = 5;
    private static final int NUMBER_OF_MESSAGE = 2;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    private static TestMessage expected;
    @Parameterized.Parameter
    public String sysFolder;
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return asList(new Object[][]{
                {systemFolders().getSent()},                  //todo: добавить inbox
                {systemFolders().getDeleted()},
                {systemFolders().getDrafts()},
        });
    }

    @BeforeClass
    public static void prepareEml() throws Exception {
        expected = getFilledMessageWithAttachFromEML
                (AppendMessages.class.getResource(PATH_TO_EML).toURI());
    }

    @Test
    @Stories(MyStories.JIRA)
    @Description("Копируем несколько сообщений из папки в подпапку системной папки [MAILPROTO-2132] ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("386")
    public void copyFromUserFolderToEmptySystemSubfolder() throws Exception {
        String folderFrom = Utils.generateName();
        prodImap.request(create(folderFrom)).shouldBeOk();
        imap.list().shouldSeeFolder(folderFrom);

        imap.append().appendRandomMessages(folderFrom, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(folderFrom, NUMBER_OF_MESSAGE);

        FolderContainer folderContainer = FolderContainer.newFolder(sysFolder, encode(Utils.cyrillic()));
        prodImap.request(create(folderContainer.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folderContainer.foldersTreeAsList());

        imap.request(select(folderFrom)).repeatUntilOk(imap);
        imap.request(copy("1:" + NUMBER_OF_MESSAGE, folderContainer.fullName())).shouldBeOk();
        prodImap.select().waitMsgs(folderContainer.fullName(), NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Переносим несколько сообщений из папки в подпапку системной папки\n" +
            "[MAILPROTO-2132][MAILPROTO-2321]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("387")
    public void moveFromUserFolderToEmptySystemSubfolder() throws Exception {
        String folderFrom = Utils.generateName();
        prodImap.request(create(folderFrom)).shouldBeOk();
        imap.list().shouldSeeFolder(folderFrom);

        imap.append().appendRandomMessages(folderFrom, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(folderFrom, NUMBER_OF_MESSAGE);

        FolderContainer folderContainer = FolderContainer.newFolder(sysFolder, encode(Utils.cyrillic()));
        prodImap.request(create(folderContainer.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folderContainer.foldersTreeAsList());

        imap.request(select(folderFrom)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, folderContainer.fullName())).shouldBeOk();

        imap.select().waitMsgs(folderContainer.fullName(), NUMBER_OF_MESSAGE);
        imap.select().waitNoMessages(folderFrom);
    }

    @Test
    @Description("Проверяем что можно удалить подпапку в системной папке")
    @ru.yandex.qatools.allure.annotations.TestCaseId("389")
    public void deleteSubfolderToSystemFolder() {
        FolderContainer folderContainer = FolderContainer.newFolder(sysFolder, Util.getRandomString());
        prodImap.request(create(folderContainer.fullName())).shouldBeOk();
        imap.list().shouldSeeFolder(folderContainer.fullName());

        imap.request(delete(folderContainer.fullName())).shouldBeOk();
        imap.list().shouldNotSeeFolder(folderContainer.fullName());
        imap.list().shouldSeeSystemFolders();
    }

    @Test
    @Description("Выбираем пустую подпапку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("390")
    public void examineSubfolderOfSystemFolder() {
        String subfolder = newFolder(sysFolder, Utils.generateName()).fullName();
        prodImap.request(create(subfolder)).shouldBeOk();
        imap.list().shouldSeeFolder(subfolder);
        imap.request(examine(subfolder)).repeatUntilOk(imap).shouldBeOk();
    }

    @Test
    @Description("Выбираем пустую подпапку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("391")
    public void selectSubfolderOfSystemFolder() {
        String subfolder = newFolder(sysFolder, Utils.generateName()).fullName();
        prodImap.request(create(subfolder)).shouldBeOk();
        imap.list().shouldSeeFolder(subfolder);
        imap.request(select(subfolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @Description("[MAILPROTO-1965] Аппендим в подпапки всех системных папок")
    @ru.yandex.qatools.allure.annotations.TestCaseId("388")
    public void appendInSystemSubfolder() throws Exception {
        String child = newFolder(sysFolder, generateName()).fullName();
        imap.request(create(child)).shouldBeOk();
        imap.list().shouldSeeFolder(child);
        imap.request(append(child,
                literal(getMessage(expected)))).shouldBeOk();

        imap.select().waitMsgs(child, 1);
        //todo: добавить проверку соответствия отправленного и пришедшего письма
    }
}
