package ru.yandex.autotests.innerpochta.imap.copy;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.CopyResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CopyRequest.copy;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.StatusRequest.status;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.05.14
 * Time: 18:13
 */
@Aqua.Test
@Title("Команда COPY. Копируем письма")
@Features({ImapCmd.COPY, "UID COPY"})
@Stories(MyStories.COMMON)
@Description("Различные простые кейсы на копирование\n" +
        "Позитивное и негативное тестирование")
public class CopyCommonTest extends BaseTest {
    private static Class<?> currentClass = CopyCommonTest.class;

    private static final int NUMBER_OF_MESSAGE = 2;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Before
    public void prepareInbox() throws Exception {
        imap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGE);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGE);
    }

    @Description("COPY с кириллической папкой без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("97")
    public void copyCyrillicMessageShouldSeeBad() {
        imap.select().inbox();
        imap.request(copy("1:" + NUMBER_OF_MESSAGE, Utils.cyrillic())).shouldBeBad()
                .statusLineContains(CopyResponse.FOLDER_ENCODING_ERROR);
        //uid copy
        imap.request(copy(imap.search().uidAllMessages(), Utils.cyrillic()).uid(true)).shouldBeBad()
                .statusLineContains(CopyResponse.UID_COPY_FOLDER_ENCODING_ERROR);
    }

    @Description("COPY без SELECT\n" +
            "Должны увидеть: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("98")
    public void copyWithoutSelectedFolderShouldSeeBad() {
        imap.select().inbox();
        List<String> uids = imap.search().uidAllMessages();

        imap.request(unselect());
        imap.request(copy("1:" + NUMBER_OF_MESSAGE, Folders.INBOX)).shouldBeBad()
                .statusLineContains(CopyResponse.WRONG_SESSION_STATE);
        //uid copy
        imap.request(copy(uids, Folders.INBOX).uid(true)).shouldBeBad()
                .statusLineContains(CopyResponse.UID_COPY_WRONG_SESSION_STATE);
    }

    @Description("COPY с EXAMINE\n")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("99")
    public void copyWithExaminedFolder() {
        imap.request(examine(Folders.INBOX)).shouldBeOk();
        imap.request(copy("1:" + NUMBER_OF_MESSAGE, Folders.INBOX)).shouldBeOk();
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGE * 2);
    }

    @Test
    @Description("UID COPY с EXAMINE\n")
    @ru.yandex.qatools.allure.annotations.TestCaseId("100")
    public void uidCopyWithExaminedFolder() {
        imap.select().inbox();
        List<String> uids = imap.search().uidAllMessages();

        imap.request(unselect());

        imap.examine().inbox();
        imap.request(copy(uids, Folders.INBOX).uid(true)).shouldBeOk();
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGE * 2);
    }

    @Test
    @Issue("MPROTO-1618")
    @Description("UID COPY. Проверяем, что в статусе в ответа строки типа:\n" +
            "COPYUID 1404385194 2078:2079 2080:2081, 2078:2079  uid-s копируемых писем, " +
            "2080:2081 uid-ы новых писем в папке куда копировали")
    @ru.yandex.qatools.allure.annotations.TestCaseId("101")
    public void uidCopyShouldSeeCopyUidInStatusLine() {
        String format = "[COPYUID %d %s:%s %s:%s] UID COPY Completed.";
        String folderName = Utils.generateName();
        prodImap.request(create(folderName)).shouldBeOk();

        imap.select().inbox();
        List<String> uids = imap.search().uidAllMessages();

        //так как переносим в эту же папку новые uid должны быть старые + количество копируемых сообщений
        String additionalText = String.format(format,
                imap.request(status(Folders.INBOX).uidValidity()).shouldBeOk().uidValidity(), uids.get(0), uids.get(1),
                Integer.parseInt(uids.get(0)) + NUMBER_OF_MESSAGE, Integer.parseInt(uids.get(1)) + NUMBER_OF_MESSAGE);

        imap.select().inbox();
        imap.request(copy(uids, Folders.INBOX).uid(true)).shouldBeOk()
                .statusLineContains(additionalText);
        imap.select().waitMsgsInInbox(NUMBER_OF_MESSAGE * 2);

        //так как переносим в новую папку новые uid должны быть 1 и 2
        additionalText = String.format(format, imap.request(status(folderName).uidValidity()).shouldBeOk().uidValidity(),
                uids.get(0), uids.get(1), 1, 2);

        imap.request(copy(uids, folderName).uid(true)).shouldBeOk()
                .statusLineContains(additionalText);
        imap.select().waitMsgs(folderName, NUMBER_OF_MESSAGE);

    }

}
