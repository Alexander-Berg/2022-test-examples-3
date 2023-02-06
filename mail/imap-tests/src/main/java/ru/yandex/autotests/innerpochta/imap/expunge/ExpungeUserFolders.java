package ru.yandex.autotests.innerpochta.imap.expunge;

import java.util.Collection;

import org.junit.Before;
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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.expunge;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:12
 */
@Aqua.Test
@Title("Команда EXPUNGE. Удаление писем в пользовательских папках")
@Features({ImapCmd.EXPUNGE})
@Stories(MyStories.USER_FOLDERS)
@Description("Удаляем письма в пользовательских папках, помеченные флагом /Deleted\n" +
        "Позитивное и негативное тестироывние")
@RunWith(Parameterized.class)
public class ExpungeUserFolders extends BaseTest {
    private static Class<?> currentClass = ExpungeUserFolders.class;

    private static final int LEVEL_OF_HIERARCHY = 5;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String userFolder;


    public ExpungeUserFolders(String userFolder) {
        this.userFolder = userFolder;
    }

    @Parameterized.Parameters(name = "folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Before
    public void prepareFolder() {
        prodImap.request(create(userFolder));
        imap.list().shouldSeeFolder(userFolder);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("184")
    public void simpleExpungeAfterSelectUserFolder() {
        imap.request(select(userFolder)).repeatUntilOk(imap);
        imap.request(expunge()).shouldBeOk();
        imap.request(expunge()).shouldBeOk();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("185")
    public void simpleExpungeAfterExamineUserFolder() {
        imap.request(examine(userFolder)).repeatUntilOk(imap);
        imap.request(expunge()).shouldBeOk();
        imap.request(expunge()).shouldBeOk();
    }

    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. Выполняем EXPUNGE\n" +
            "Письмо должно удалиться\n" +
            "[MAILPROTO-2137] - list говорит ошибка сервера (корп)")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("186")
    public void expungeWithDeletedOneMessageAfterSelect() throws Exception {
        prodImap.append().appendRandomMessage(userFolder);
        prodImap.select().waitMsgs(userFolder, 1);

        imap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.noop().pullChanges();
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(expunge()).shouldBeOk().expungeShouldBe(1);

        prodImap.status().numberOfMessagesShouldBe(userFolder, 0);
    }

    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. " +
            "Открываем папку для чтения. Закрываем папку.\n" +
            "Письмо НЕ должно удалиться")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("183")
    public void closeWithDeletedOneMessageAfterExamine() throws Exception {
        prodImap.append().appendRandomMessage(userFolder);
        prodImap.select().waitMsgs(userFolder, 1);

        prodImap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.store().deletedOnMessages(prodImap.search().allMessages());
        prodImap.request(unselect()).shouldBeOk();

        imap.request(examine(userFolder)).shouldBeOk();
        imap.request(expunge()).shouldBeOk().shouldBeEmpty();

        prodImap.status().numberOfMessagesShouldBe(userFolder, 1);
    }
}
