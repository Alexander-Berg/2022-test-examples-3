package ru.yandex.autotests.innerpochta.imap.delete;

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
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 01.04.14
 * Time: 22:24
 */
@Aqua.Test
@Title("Команда DELETE. Удаляем системные папки, подпапки")
@Features({ImapCmd.DELETE})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Удаляем системные папки и подпапки системных папок на различных уровнях")
@RunWith(Parameterized.class)
public class DeleteSystemFolders extends BaseTest {
    private static Class<?> currentClass = DeleteSystemFolders.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;


    public DeleteSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> sysFolders() {
        return allSystemFolders();
    }

    @Test
    @Description("Удаляем системные папки\n" +
            "Проверяем статус (не ок) и что папки на месте")
    @ru.yandex.qatools.allure.annotations.TestCaseId("156")
    public void testDeleteSystemFolderShouldSeeNo() {
        imap.request(delete(sysFolder)).shouldBeNo();
        imap.list().shouldSeeSystemFolders();
    }
}
