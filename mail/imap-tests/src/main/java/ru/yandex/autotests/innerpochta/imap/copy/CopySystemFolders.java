package ru.yandex.autotests.innerpochta.imap.copy;

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
import ru.yandex.autotests.innerpochta.imap.responses.CopyResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CopyRequest.copy;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 06.05.14
 * Time: 19:17
 * <p/>
 * [MAILPROTO-2188]
 */
@Aqua.Test
@Title("Команда COPY. Системные папки")
@Features({ImapCmd.COPY, "UID COPY"})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Копируем письма в системные папки")
@RunWith(Parameterized.class)
public class CopySystemFolders extends BaseTest {
    private static Class<?> currentClass = CopySystemFolders.class;

    private static final int LEVEL_OF_HIERARCHY = 5;
    private static final int NUMBER_OF_MESSAGE = 2;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;

    public CopySystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allSystemFolders();
    }

    @Test
    @Description("Копируем по UID из INBOX в пользовательскую папку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("105")
    public void uidCopyToSystemFolderTest() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGE);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGE);

        imap.select().inbox();
        imap.request(copy(imap.search().uidAllMessages(), sysFolder).uid(true)).shouldBeOk();
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Пытаемся скопировать письма из пустой папки\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("108")
    public void copyWithEmptyMailboxShouldSeeNo() {
        imap.select().waitMsgs(sysFolder, 0);

        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(copy("1:" + NUMBER_OF_MESSAGE, sysFolder)).shouldBeNo().statusLineContains(CopyResponse.NO_MESSAGES);
    }

    @Test
    @Description("Копируем письма в несуществующую папку [MAILPROTO-2188]\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("107")
    public void copyInNonExistFolderShouldSeeNo() throws Exception {
        imap.append().appendRandomMessages(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(copy("1:" + NUMBER_OF_MESSAGE, Utils.generateName())).shouldBeNo()
                .statusLineContains(CopyResponse.NO_SUCH_FOLDER);
        //все письма остались на месте
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Копируем письма из папки в эту же папку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("106")
    public void copyToThisSameSystemFolder() throws Exception {
        imap.append().appendRandomMessages(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(copy("1:" + NUMBER_OF_MESSAGE, sysFolder)).shouldBeOk();
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE * 2);
    }

    @Test
    @Description("Копируем письма из папки в пустую системную папку")
    @ru.yandex.qatools.allure.annotations.TestCaseId("103")
    public void copyFromUserFolderToEmptyFolder() throws Exception {
        String folderFrom = Utils.generateName();
        prodImap.request(create(folderFrom)).shouldBeOk();
        imap.list().shouldSeeFolder(folderFrom);

        imap.append().appendRandomMessages(folderFrom, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(folderFrom, NUMBER_OF_MESSAGE);

        imap.request(select(folderFrom)).repeatUntilOk(imap);
        imap.request(copy("1:" + NUMBER_OF_MESSAGE, sysFolder)).shouldBeOk();
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        //todo: хорошо бы проверить что сообщения те
    }
}
