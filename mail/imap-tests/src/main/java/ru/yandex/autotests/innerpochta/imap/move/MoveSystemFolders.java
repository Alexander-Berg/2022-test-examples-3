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
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.MoveRequest.move;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.08.14
 * Time: 19:46
 */
@Aqua.Test
@Title("Команда MOVE. Переносим письма в системные папки")
@Features({ImapCmd.MOVE, "UID MOVE"})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Переносим писем в системные папки различного уровня")
@RunWith(Parameterized.class)
public class MoveSystemFolders extends BaseTest {
    private static Class<?> currentClass = MoveSystemFolders.class;

    private static final int LEVEL_OF_HIERARCHY = 5;
    private static final int NUMBER_OF_MESSAGE = 2;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;

    public MoveSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allSystemFolders();
    }

    @Test
    @Description("Переносим письма по UID из INBOX в пользовательскую папку [MAILPROTO-2318]")
    @Stories({MyStories.JIRA, "UID CMD"})
    @ru.yandex.qatools.allure.annotations.TestCaseId("303")
    public void uidMoveToSystemFolderTest() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGE);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGE);

        imap.select().inbox();
        imap.request(move(imap.search().uidAllMessages(), sysFolder).uid(true)).shouldBeOk();
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitNoMessagesInInbox();
    }

    @Test
    @Description("Пытаемся перенести письма из пустой папки\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("304")
    public void moveWithEmptyMailboxShouldSeeNo() {
        imap.select().waitMsgs(sysFolder, 0);

        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, systemFolders().getInbox())).shouldBeNo()
                .statusLineContains(MoveResponse.NO_MESSAGES);
    }

    @Test
    @Description("Копируем письма в несуществующую папку [MAILPROTO-2188]\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("305")
    public void moveInNonExistFolderShouldSeeNo() throws Exception {
        imap.append().appendRandomMessages(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, Utils.generateName())).shouldBeNo()
                .statusLineContains(MoveResponse.NO_SUCH_FOLDER);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Переносим письма из папки в эту же папку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("306")
    public void moveToThisSameSystemFolder() throws Exception {
        imap.append().appendRandomMessages(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, sysFolder)).shouldBeOk();
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Переносим письма из папки в пустую системную папку [MAILPROTO-2321]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("307")
    public void moveFromUserFolderToEmptyFolder() throws Exception {
        String folderFrom = Utils.generateName();
        prodImap.request(create(folderFrom)).shouldBeOk();
        imap.list().shouldSeeFolder(folderFrom);

        imap.append().appendRandomMessages(folderFrom, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(folderFrom, NUMBER_OF_MESSAGE);

        imap.request(select(folderFrom)).repeatUntilOk(imap);
        imap.request(move("1:" + NUMBER_OF_MESSAGE, sysFolder)).shouldBeOk();

        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitNoMessages(folderFrom);
    }
}
