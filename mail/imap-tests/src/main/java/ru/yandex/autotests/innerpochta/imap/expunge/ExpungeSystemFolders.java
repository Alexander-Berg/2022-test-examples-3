package ru.yandex.autotests.innerpochta.imap.expunge;

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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.expunge;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 01.05.14
 * Time: 18:23
 */
@Aqua.Test
@Title("Команда EXPUNGE. Удаление писем в системных папках")
@Features({ImapCmd.EXPUNGE})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Удаляем письма в системных папках, помеченные флагом /Deleted\n" +
        "Позитивное и негативное тестироывние")
@RunWith(Parameterized.class)
public class ExpungeSystemFolders extends BaseTest {
    private static Class<?> currentClass = ExpungeSystemFolders.class;

    private static final int LEVEL_OF_HIERARCHY = 5;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;


    public ExpungeSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allSystemFolders();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("181")
    public void simpleExpungeAfterSelectEmptySystemFolder() {
        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(expunge()).shouldBeOk();
        imap.request(expunge()).shouldBeOk();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("182")
    public void simpleExpungeAfterExamineEmptySystemFolder() {
        prodImap.request(examine(sysFolder)).repeatUntilOk(imap);
        imap.request(expunge()).shouldBeOk();
        imap.request(expunge()).shouldBeOk();
    }
}
