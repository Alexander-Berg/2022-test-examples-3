package ru.yandex.autotests.innerpochta.imap.pg;

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
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse.Status;
import ru.yandex.autotests.innerpochta.imap.rules.CleanRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Arrays.asList;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateName;

@Aqua.Test
@Title("Команда CREATE. Создаем подпапки в системных папках")
@Features({ImapCmd.CREATE, "PG"})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Создаем папки, подпапки в системных папках")
@RunWith(Parameterized.class)
public class CreateSystemSubfolderTest extends BaseTest {
    private static Class<?> currentClass = CreateSystemSubfolderTest.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Parameterized.Parameter(0)
    public String sysFolder;
    @Parameterized.Parameter(1)
    public Status status;
    @Rule
    public ImapClient prodImap = CleanRule.withCleanBefore(newLoginedClient(currentClass));

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return asList(new Object[][]{
                {systemFolders().getSent(), Status.OK},
                {systemFolders().getDeleted(), Status.OK},
                {systemFolders().getDrafts(), Status.OK},

                {systemFolders().getOutgoing(), Status.NO},
                {systemFolders().getSpam(), Status.NO}
        });
    }

    @Test
    @Stories(MyStories.JIRA)
    @Description("[MAILPROTO-2133] Создаем подпапку в системной папке")
    @ru.yandex.qatools.allure.annotations.TestCaseId("367")
    public void testCreateFolderInSystemFolder() {
        String folder = newFolder(sysFolder, generateName()).fullName();
        imap.request(create(folder)).statusShouldBe(status);
        if (status.equals(Status.OK)) {
            imap.list().shouldSeeFolder(folder);
        }
    }

}
