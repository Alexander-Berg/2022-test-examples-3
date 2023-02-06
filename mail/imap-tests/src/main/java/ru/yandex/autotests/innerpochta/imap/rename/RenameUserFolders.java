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
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.requests.RenameRequest.rename;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:15
 */
@Aqua.Test
@Title("Команда RENAME. Переименование папки.")
@Features({ImapCmd.RENAME})
@Stories(MyStories.USER_FOLDERS)
@Description("Переименовываем папки на различных уровнях")
@RunWith(Parameterized.class)
public class RenameUserFolders extends BaseTest {
    private static Class<?> currentClass = RenameUserFolders.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String folderForRename = Util.getRandomString();
    private String folderToRename;


    public RenameUserFolders(String folderToRename) {
        this.folderToRename = folderToRename;
    }

    @Parameterized.Parameters(name = "folder - {0}")
    public static Collection<Object[]> foldersToRename() {
        return allKindsOfFolders();
    }

    @Description("Просто переименовываем папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("413")
    public void simpleRenameSingleFolderToSingleFolder() {
        prodImap.request(create(folderForRename)).shouldBeOk();
        imap.list().shouldSeeFolder(folderForRename);
        imap.request(rename(folderForRename, folderToRename)).shouldBeOk();
        imap.list().shouldSeeFolder(folderToRename);
        imap.list().shouldNotSeeFolder(folderForRename);
    }

    @Description("Переименовываем папку в подпапку\n"
            + "Должна создасться иерархия  папок")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("414")
    public void renameFolderToSubfolder() {
        prodImap.request(create(folderForRename)).shouldBeOk();
        imap.list().shouldSeeFolder(folderForRename);

        FolderContainer folderContainer = newFolder(Utils.generateName(), Utils.generateName());

        imap.request(rename(folderForRename, folderContainer.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folderContainer.foldersTreeAsList());
        imap.list().shouldNotSeeFolder(folderForRename);
        //todo: нужно проверить флаги
    }

    @Description("Переименовываем подпапку в другую подпапку\n" +
            "Родительская папка должна остаться")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("415")
    public void renameSubfolderToSubfolder() {
        FolderContainer folderContainerOld = newFolder(folderForRename, folderToRename);
        FolderContainer folderContainerNew = newFolder(Utils.generateName(), Utils.generateName());

        prodImap.request(create(folderContainerOld.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folderContainerOld.foldersTreeAsList());

        imap.request(rename(folderContainerOld.fullName(), folderContainerNew.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folderContainerNew.foldersTreeAsList());
        imap.list().shouldNotSeeFolder(folderContainerOld.fullName());
        imap.list().shouldSeeFolder(folderContainerOld.parent());
    }

    @Description("Переименовываем подпапку в другую папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("416")
    public void renameSubfolderToSingleFolder() {
        FolderContainer folderContainerOld = newFolder(Utils.generateName(), Utils.generateName());
        prodImap.request(create(folderContainerOld.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folderContainerOld.foldersTreeAsList());

        imap.request(rename(folderContainerOld.fullName(), folderToRename)).shouldBeOk();
        imap.list().shouldSeeFolder(folderToRename);
        imap.list().shouldSeeFolder(folderContainerOld.parent());
        imap.list().shouldNotSeeFolder(folderContainerOld.fullName());
    }

    @Description("Пробуем переименовать папку в несуществующую папку\n")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("417")
    public void renameToNotExistFolderShouldSeeBad() {
        imap.request(rename(folderToRename, Utils.generateName())).shouldBeNo();
    }

    public String getFolderExistStatus(String folderName) {
        return String.format("[CLIENTBUG] RENAME Cannot rename folder \"%s\" to \"%s\": folder exists.",
                folderName, folderName);
    }


    @Test
    @Title("Переименоваем папку в себя. Синхронизация с помощью LIST")
    @Description("Пробуем переименовать папку в себя\n")
    @ru.yandex.qatools.allure.annotations.TestCaseId("418")
    public void renameToSelfFolderShouldSeeOk() {
        prodImap.request(create(folderToRename)).shouldBeOk();
        imap.list().shouldSeeFolder(folderToRename);
        imap.request(rename(folderToRename, folderToRename)).shouldBeOk();
    }

    @Test
    @Title("Переименоваем папку в себя. Без синхронизации с помощью LIST")
    @Description("Пробуем переименовать папку в себя\n Без синхронизации.")
    @ru.yandex.qatools.allure.annotations.TestCaseId("419")
    public void renameToSelfWithoutListFolderShouldSeeOk() {
        prodImap.request(create(folderToRename)).shouldBeOk();
        imap.request(rename(folderToRename, folderToRename)).shouldBeOk();
    }

    @Description("Пробуем переименовать папку в папку, которая уже существует")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("420")
    public void renameToFolderAlreadyExist() {
        prodImap.request(create(folderForRename)).shouldBeOk();
        prodImap.request(create(folderToRename)).shouldBeOk();

        imap.list().shouldSeeFolder(folderForRename);
        imap.list().shouldSeeFolder(folderToRename);

        imap.request(rename(folderToRename, folderForRename)).shouldBeBad();
        imap.request(rename(folderForRename, folderToRename)).shouldBeBad();
    }

    @Test
    @Issue("MAILDLV-421")
    @Description("Переименовываем папку в только что удаленную папку в одной сессии")
    @ru.yandex.qatools.allure.annotations.TestCaseId("421")
    public void renameToDeleteFolderInOneSessionShouldBeOk() {
        prodImap.request(create(folderForRename)).shouldBeOk();
        prodImap.request(create(folderToRename)).shouldBeOk();

        imap.list().shouldSeeFolder(folderForRename);
        imap.list().shouldSeeFolder(folderToRename);

        imap.request(delete(folderToRename)).shouldBeOk();

        imap.request(rename(folderForRename, folderToRename)).shouldBeOk();
        imap.list().shouldSeeFolder(folderToRename);
        imap.list().shouldNotSeeFolder(folderForRename);
    }

    @Test
    @Issue("MAILDLV-421")
    @Description("Переименовываем папку в только что удаленную папку в двух сессиях")
    @ru.yandex.qatools.allure.annotations.TestCaseId("422")
    public void renameToDeleteFolderInTwoSession() {
        prodImap.request(create(folderForRename)).shouldBeOk();
        prodImap.request(create(folderToRename)).shouldBeOk();

        imap.list().shouldSeeFolder(folderForRename);
        imap.list().shouldSeeFolder(folderToRename);

        imap.request(delete(folderToRename)).shouldBeOk();

        imap.request(rename(folderForRename, folderToRename)).shouldBeOk();
        imap.list().shouldSeeFolder(folderToRename);
        imap.list().shouldNotSeeFolder(folderForRename);
    }
}
