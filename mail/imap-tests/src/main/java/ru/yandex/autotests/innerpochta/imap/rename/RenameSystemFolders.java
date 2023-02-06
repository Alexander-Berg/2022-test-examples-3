package ru.yandex.autotests.innerpochta.imap.rename;

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
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.RenameRequest.rename;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 02.04.14
 * Time: 17:11
 */
@Aqua.Test
@Title("Команда RENAME. Переименование системных папок, а также пользовательских в системные")
@Features({ImapCmd.RENAME})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Переименовываем системные папки на различных уровнях")
@RunWith(Parameterized.class)
public class RenameSystemFolders extends BaseTest {
    private static Class<?> currentClass = RenameSystemFolders.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;
    private String folderForRename = Util.getRandomString();


    public RenameSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> sysFolders() {
        return allSystemFolders();
    }

    @Description("Переименовыем системную папку в системную папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("409")
    public void renameSystemFolderToSystemFolder() {
        imap.request(rename(sysFolder, sysFolder)).shouldBeOk();
        imap.list().shouldSeeSystemFolders();
    }

    @Description("Переименовыем пользовательскую папку в системную папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("410")
    public void renameFolderToSystemFolderShouldSeeBad() {
        prodImap.request(create(folderForRename)).shouldBeOk();
        imap.list().shouldSeeFolder(folderForRename);

        imap.request(rename(folderForRename, sysFolder)).shouldBeBad();
        imap.list().shouldSeeSystemFolders();
        imap.list().shouldSeeFolder(folderForRename);

    }

    @Description("Переименовыем подпапку в системную папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("411")
    public void renameSubfolderToSystemFolder() {
        FolderContainer folderContainer = FolderContainer.newFolder(folderForRename, Util.getRandomString());
        prodImap.request(create(folderContainer.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folderContainer.foldersTreeAsList());

        imap.request(rename(folderContainer.fullName(), sysFolder)).shouldBeBad();
    }


    @Description("Переименовыем подпапку в системную папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("412")
    public void renameNotExistToSystemFolder() {
        imap.request(rename(Util.getRandomString(), sysFolder)).shouldBeNo();
    }
}
