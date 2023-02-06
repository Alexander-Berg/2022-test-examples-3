package ru.yandex.autotests.innerpochta.imap.move;

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
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.MoveResponse;
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.MoveRequest.move;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.08.14
 * Time: 19:46
 * <p/>
 * [MAILPROTO-2323]
 */
@Aqua.Test
@Title("Команда MOVE. Переносим письма в пользовательские папки")
@Features({ImapCmd.MOVE, "UID MOVE"})
@Stories(MyStories.USER_FOLDERS)
@Description("Перенос писем в пользовательские папки различного уровня")
@RunWith(Parameterized.class)
public class MoveUserFolders extends BaseTest {
    private static Class<?> currentClass = MoveUserFolders.class;

    private static final int NUMBER_OF_MESSAGE = 2;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String toFolder;

    public MoveUserFolders(String toFolder) {
        this.toFolder = toFolder;
    }

    @Parameterized.Parameters(name = "userFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Test
    @Description("Переносим письма по UID из INBOX в пользовательскую папку [MAILPROTO-2318]")
    @Stories({MyStories.JIRA, "UID CMD"})
    @ru.yandex.qatools.allure.annotations.TestCaseId("313")
    public void uidMoveToUserFolderTest() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGE);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGE);

        prodImap.request(create(toFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(toFolder);

        imap.select().inbox();
        imap.request(move(imap.search().uidAllMessages(), toFolder).uid(true)).shouldBeOk();

        imap.select().waitNoMessagesInInbox();
        imap.select().waitMsgs(toFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Пытаемся перенести письма из пустой папки\n" +
            "Ожидаемый результат: NO")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("309")
    public void moveWithEmptyMailboxShouldSeeNo() {
        prodImap.request(create(toFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(toFolder);

        prodImap.select().waitMsgs(toFolder, 0);

        imap.request(select(toFolder)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, systemFolders().getInbox())).shouldBeNo().statusLineContains(MoveResponse.NO_MESSAGES);
    }

    @Test
    @Description("Переносим письма в несуществующую папку\n" +
            "[MAILPROTO-2137] Ожидаемый результат: NO")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("310")
    public void moveInNonExistFolderShouldSeeNo() throws Exception {
        String folderName = Utils.generateName();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);
        prodImap.append().appendRandomMessages(folderName, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(folderName, NUMBER_OF_MESSAGE);

        imap.request(select(folderName)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, toFolder)).shouldBeNo()
                .statusLineContains(MoveResponse.NO_SUCH_FOLDER);
        imap.select().waitMsgs(folderName, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Переносим письма из папки в эту же папку")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("314")
    public void moveToThisSameUserFolder() throws Exception {
        prodImap.request(create(toFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(toFolder);
        prodImap.append().appendRandomMessages(toFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(toFolder, NUMBER_OF_MESSAGE);

        imap.request(select(toFolder)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, toFolder)).shouldBeOk();
        prodImap.select().waitMsgs(toFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Копируем письма из папки в другую пустую папку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("311")
    public void moveFromUserFolderToEmptyFolder() throws Exception {
        String folderFrom = Utils.generateName();
        prodImap.request(create(folderFrom)).shouldBeOk();
        imap.list().shouldSeeFolder(folderFrom);
        prodImap.append().appendRandomMessages(folderFrom, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(folderFrom, NUMBER_OF_MESSAGE);

        prodImap.request(create(toFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(toFolder);

        imap.request(select(folderFrom)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, toFolder)).shouldBeOk();

        imap.select().waitMsgs(toFolder, NUMBER_OF_MESSAGE);
        imap.select().waitNoMessages(folderFrom);
    }

    @Test
    @Description("Переносим несколько сообщений из папки в подпапку\n[MAILPROTO-2137]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("312")
    public void copyFromUserFolderToEmptySubfolder() throws Exception {
        String folderFrom = Utils.generateName();
        prodImap.request(create(folderFrom)).shouldBeOk();
        imap.list().shouldSeeFolder(folderFrom);
        prodImap.append().appendRandomMessages(folderFrom, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(folderFrom, NUMBER_OF_MESSAGE);

        FolderContainer folderContainer = FolderContainer.newFolder(Utils.generateName(), toFolder);
        prodImap.request(create(folderContainer.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folderContainer.foldersTreeAsList());

        imap.request(select(folderFrom)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, folderContainer.fullName())).shouldBeOk();

        imap.select().waitMsgs(folderContainer.fullName(), NUMBER_OF_MESSAGE);
        imap.select().waitNoMessages(folderFrom);
    }
}
