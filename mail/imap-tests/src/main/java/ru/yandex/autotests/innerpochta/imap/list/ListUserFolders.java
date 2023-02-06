package ru.yandex.autotests.innerpochta.imap.list;

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
import ru.yandex.autotests.innerpochta.imap.consts.flags.SystemFolderFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.imap.structures.ItemContainer;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.HAS_CHILDREN;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.HAS_NO_CHILDREN;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.MARKED;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.UNMARKED;
import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;
import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;
import static ru.yandex.autotests.innerpochta.imap.requests.RenameRequest.rename;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;
import static ru.yandex.autotests.innerpochta.imap.structures.ItemContainer.newItem;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:13
 */
@Aqua.Test
@Title("Команда LIST. Список существующих папок.")
@Features({ImapCmd.LIST})
@Stories(MyStories.USER_FOLDERS)
@Description("Проверяем выдачу LIST\n"
        + "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class ListUserFolders extends BaseTest {
    private static Class<?> currentClass = ListUserFolders.class;
    public static final int LEVEL_OF_HIERARCHY = 4;
    @ClassRule
    public static ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String userFolder;

    public ListUserFolders(String userFolder) {
        this.userFolder = userFolder;
    }

    @Parameterized.Parameters(name = "user_folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Test
    @Description("Дергаем list с несуществующей папкой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("235")
    public void listWithNotExistFolder() {
        imap.request(list("\"\"", userFolder)).shouldBeOk().withoutItems();
        imap.list().shouldSeeOnlySystemFoldersWithFlags();
    }

    @Description("Создаем папку, должна появиться в list")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("236")
    public void shouldSeeFolderInListAfterCreate() {
        ItemContainer item = newItem().setName(userFolder).setFlags(UNMARKED.value(), HAS_NO_CHILDREN.value());
        prodImap.request(create(userFolder));
        imap.list().shouldSeeFolder(userFolder);

        imap.request(list("\"\"", userFolder)).shouldBeOk()
                .withItems(item.getListItem());
        imap.request(list(userFolder + "|", "%")).shouldBeOk().withoutItems();
        imap.list().shouldSeeInListWithFlags(item.getListItem());
        imap.list().shouldSeeInListWithFlagsInWildcard(item.getListItem());
    }

    @Description("Создаем папку, должна появиться в list")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("237")
    public void shouldSeeSubfolderInListAfterCreate() {
        FolderContainer folderContainer = newFolder(userFolder, Util.getRandomString());

        ItemContainer parentItem = newItem().setName(folderContainer.parent()).setFlags(UNMARKED.value(), HAS_CHILDREN.value());
        ItemContainer childItem = newItem().setName(folderContainer.fullName()).setFlags(UNMARKED.value(), HAS_NO_CHILDREN.value());

        prodImap.request(create(folderContainer.fullName()));
        imap.list().shouldSeeFolders(folderContainer.foldersTreeAsList());

        imap.request(list("\"\"", folderContainer.parent())).shouldBeOk()
                .withItems(parentItem.getListItem());

        imap.request(list("\"\"", folderContainer.fullName())).shouldBeOk()
                .withItems(childItem.getListItem());
        //БЕЗ папок с |
        imap.request(list(folderContainer.parent() + "|", "%")).shouldBeOk().withItems(childItem.getListItem());
        imap.request(list(folderContainer.fullName() + "|", "%")).shouldBeOk().withoutItems();

        imap.request(list("\"\"", "%")).shouldBeOk().withItems(parentItem.getListItem(), SystemFolderFlags.getINBOXItem(), SystemFolderFlags.getSentItem(), SystemFolderFlags.getTrashItem(), SystemFolderFlags.getSpamItem(),
                SystemFolderFlags.getDraftsItem(), SystemFolderFlags.getOutgoingItem());

        imap.list().shouldSeeInListWithFlags(parentItem.getListItem(), childItem.getListItem());
    }

    @Description("Удаляем папку, смотрим, что не отображается в list")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("239")
    public void shouldNotSeeFolderInListAfterDelete() {
        prodImap.request(create(userFolder));
        prodImap.list().shouldSeeFolder(userFolder);
        prodImap.request(delete(userFolder));

        imap.list().shouldNotSeeFolder(userFolder);
        imap.list().shouldSeeOnlySystemFoldersWithFlags();
        imap.list().shouldSeeOnlySystemFoldersWithFlagsInWildcard();
    }

    @Description("Переименовываем простую папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("242")
    public void shouldSeeRenamedFolder() {
        String renamed = Util.getRandomString();
        ItemContainer item = newItem().setName(renamed).setFlags(UNMARKED.value(), HAS_NO_CHILDREN.value());
        prodImap.request(create(userFolder));
        prodImap.list().shouldSeeFolder(userFolder);
        prodImap.request(rename(userFolder, renamed));

        imap.list().shouldSeeInListWithFlags(item.getListItem());
        imap.list().shouldSeeInListWithFlagsInWildcard(item.getListItem());

        imap.request(list("\"\"", renamed)).shouldBeOk().withItems(item.getListItem());
        imap.request(list("\"\"", userFolder)).shouldBeOk().withoutItems();

        imap.request(list(renamed, "%")).shouldBeOk().withItems(item.getListItem());
        imap.request(list(renamed + "|", "%")).shouldBeOk().withoutItems();

        imap.request(list(userFolder + "|", "%")).shouldBeOk().withoutItems();
        imap.request(list(userFolder, "%")).shouldBeOk().withoutItems();
    }

    @Test
    @Description("Создаем папку, добавляем письмо. Unmarked -> Marked")
    @ru.yandex.qatools.allure.annotations.TestCaseId("245")
    public void shouldSeeFlagMarkedOnFolderAfterAppend() throws Exception {
        prodImap.request(create(userFolder));
        imap.list().shouldSeeFolder(userFolder);
        prodImap.append().appendRandomMessage(userFolder);
        ItemContainer folderItem = newItem().setName(userFolder).setFlags(MARKED.value(), HAS_NO_CHILDREN.value());

        imap.request(list("\"\"", userFolder)).shouldBeOk()
                .withItems(folderItem.getListItem());

        imap.list().shouldSeeInListWithFlags(folderItem.getListItem());
        imap.list().shouldSeeInListWithFlagsInWildcard(folderItem.getListItem());

        imap.request(list(userFolder, "%")).shouldBeOk().withItems(folderItem.getListItem());
        imap.request(list(userFolder + "|", "%")).shouldBeOk().withoutItems();
    }

    @Description("Создаем подпапку, добавляем письмо. Unmarked -> Marked")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("246")
    public void shouldSeeFlagMarkedOnSubfolderAfterAppend() throws Exception {
        FolderContainer subfolderContainer = newFolder(userFolder, Util.getRandomString());

        prodImap.request(create(subfolderContainer.fullName()));

        imap.list().shouldSeeFolders(subfolderContainer.foldersTreeAsList());
        prodImap.append().appendRandomMessage(subfolderContainer.fullName());

        ItemContainer parentItem = newItem().setName(subfolderContainer.parent()).setFlags(UNMARKED.value(), HAS_CHILDREN.value());
        ItemContainer childItem = newItem().setName(subfolderContainer.fullName()).setFlags(MARKED.value(), HAS_NO_CHILDREN.value());

        imap.request(list("\"\"", subfolderContainer.parent())).shouldBeOk()
                .withItems(parentItem.getListItem());

        imap.request(list("\"\"", subfolderContainer.fullName())).shouldBeOk()
                .withItems(childItem.getListItem());

        imap.list().shouldSeeInListWithFlags(parentItem.getListItem(), childItem.getListItem());
        imap.list().shouldSeeInListWithFlagsInWildcard(parentItem.getListItem());

        imap.request(list(subfolderContainer.parent(), "%")).shouldBeOk().withItems(parentItem.getListItem());
        imap.request(list(subfolderContainer.parent() + "|", "%")).shouldBeOk().withItems(childItem.getListItem());

        imap.request(list(subfolderContainer.fullName(), "%")).shouldBeOk().withItems(childItem.getListItem());
        imap.request(list(subfolderContainer.fullName() + "|", "%")).shouldBeOk().withoutItems();
    }
}
