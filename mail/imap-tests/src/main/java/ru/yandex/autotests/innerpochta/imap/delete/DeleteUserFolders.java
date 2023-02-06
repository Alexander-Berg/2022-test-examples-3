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
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:07
 */
@Aqua.Test
@Title("Команда DELETE. Удаляем папки")
@Features({ImapCmd.DELETE})
@Stories(MyStories.USER_FOLDERS)
@Description("Удаляем папки на различных уровнях")
//todo: добавить проверку флагов
@RunWith(Parameterized.class)
public class DeleteUserFolders extends BaseTest {
    private static Class<?> currentClass = DeleteUserFolders.class;

    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String folder;


    public DeleteUserFolders(String folder) {
        this.folder = folder;
    }

    @Parameterized.Parameters(name = "folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Description("Просто удаляем пользовательскую папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("158")
    public void testSimpleDelete() {
        prodImap.request(create(folder));
        imap.list().shouldSeeFolder(folder);

        imap.request(delete(folder)).shouldBeOk();
        imap.list().shouldNotSeeFolder(folder);
    }

    @Description("Дважды удаляем пользовательскую папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("160")
    public void doubleDeleteUserFolderShouldSeeNo() {
        prodImap.request(create(folder));
        imap.list().shouldSeeFolder(folder);

        imap.request(delete(folder)).shouldBeOk();
        imap.list().shouldNotSeeFolder(folder);
        imap.request(delete(folder)).shouldBeNo();
        imap.list().shouldNotSeeFolder(folder);
    }

    @Description("Проверяем что можно удалить подпапку в пользовательской папке\n" +
            "Проверяем что папка осталась на месте, а подпапка удалилась\n")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("161")
    public void deleteFolderOnSecondLevel() {
        FolderContainer folderContainer = newFolder(Utils.generateName(), folder);
        prodImap.request(create(folderContainer.fullName()));

        imap.list().shouldSeeFolders(folderContainer.foldersTreeAsList());
        imap.request(delete(folderContainer.fullName())).shouldBeOk();
        imap.list().shouldSeeFolder(folderContainer.parent());
        imap.list().shouldNotSeeFolder(folderContainer.fullName());

    }

    @Description("Пробуем дважды удалить подпапку\n" +
            "Проверяем наличие noselect у папки")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("162")
    public void doubleDeleteFolderOnSecondLevel() {
        FolderContainer folderContainer = newFolder(Utils.generateName(), folder);
        prodImap.request(create(folderContainer.fullName()));
        imap.list().shouldSeeFolders(folderContainer.foldersTreeAsList());

        imap.request(delete(folderContainer.fullName())).shouldBeOk();

        imap.list().shouldNotSeeFolder(folderContainer.fullName());
        imap.request(delete(folderContainer.fullName())).shouldBeNo();
        imap.list().shouldNotSeeFolder(folderContainer.fullName());
        imap.list().shouldSeeFolder(folderContainer.parent());

    }
}
