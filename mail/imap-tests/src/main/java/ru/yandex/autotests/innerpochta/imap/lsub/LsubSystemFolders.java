package ru.yandex.autotests.innerpochta.imap.lsub;

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

import static java.util.Arrays.asList;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.HAS_CHILDREN;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.HAS_NO_CHILDREN;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.MARKED;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags.UNMARKED;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.SystemFolderFlags.getSystemListItem;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.LsubRequest.lsub;
import static ru.yandex.autotests.innerpochta.imap.requests.SubscribeRequest.subscribe;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.rules.UnsubscribeRule.withUnsubscribeBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;
import static ru.yandex.autotests.innerpochta.imap.structures.ItemContainer.newItem;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 20.04.14
 * Time: 22:57
 */
@Aqua.Test
@Title("Команда LSUB. Список подписанных папок. Системные папки")
@Features({ImapCmd.LSUB})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Проверяем выдачу LSUB для системных папок\n"
        + "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class LsubSystemFolders extends BaseTest {
    private static Class<?> currentClass = LsubSystemFolders.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withUnsubscribeBefore(withCleanBefore(newLoginedClient(currentClass)));
    private String systemFolder;

    public LsubSystemFolders(String systemFolder) {
        this.systemFolder = systemFolder;
    }

    @Parameterized.Parameters(name = "system_folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return asList(new Object[][]{
                {systemFolders().getSent()},
                {systemFolders().getDeleted()},
                {systemFolders().getDrafts()},
        });
    }

    @Description("Создаем в системной папке подпапку, подписываемся на нее\n" + "Проверяем флаги")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("271")
    public void shouldSeeSubfolderOfSystemFolderInLsubAfterSubscribe() throws InterruptedException {
        FolderContainer subfolderContainer = newFolder(systemFolder, Util.getRandomString());

        prodImap.request(create(subfolderContainer.fullName()));

        ItemContainer systemItem = newItem().setListItem(getSystemListItem(systemFolder))
                .replaceFlag(HAS_NO_CHILDREN.value(), HAS_CHILDREN.value());
        ItemContainer subfolderItem = newItem().setName(subfolderContainer.fullName())
                .setFlags(UNMARKED.value(), HAS_NO_CHILDREN.value());

        prodImap.request(subscribe(systemFolder));
        prodImap.request(subscribe(subfolderContainer.fullName()));

        imap.request(lsub("\"\"", "*")).shouldBeOk().withItems(systemItem.getListItem(),
                subfolderItem.getListItem());

        imap.request(lsub("\"\"", subfolderContainer.parent())).shouldBeOk().withItems(systemItem.getListItem());

        imap.request(lsub("\"\"", subfolderContainer.fullName())).shouldBeOk().withItems(subfolderItem.getListItem());
    }

    @Description("Подписываемся на системную папку\n" + "Проверяем наличие подписанной папки в lsub")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("272")
    public void shouldSeeSystemFolderInLsubAfterSubscribe() {
        prodImap.request(subscribe(systemFolder));
        imap.request(lsub("\"\"", "*")).shouldBeOk().withItems(SystemFolderFlags.getSystemItem(systemFolder));
        imap.request(lsub("\"\"", systemFolder)).shouldBeOk().withItems(SystemFolderFlags.getSystemItem(systemFolder));
    }

    @Description("Подписываемся на системную папку\n" + "Проверяем наличие подписанной папки в lsub")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("273")
    public void shouldSeeMarkedOnSystemFolderAfterAppend() throws Exception {
        ItemContainer item = newItem().setListItem(getSystemListItem(systemFolder));
        prodImap.request(subscribe(systemFolder));
        prodImap.append().appendRandomMessage(systemFolder);
        imap.request(lsub("\"\"", "*")).shouldBeOk()
                .withItems(item.replaceFlag(UNMARKED.value(), MARKED.value()).getListItem());
        imap.request(lsub("\"\"", systemFolder)).shouldBeOk()
                .withItems(item.replaceFlag(UNMARKED.value(), MARKED.value()).getListItem());
    }
}
