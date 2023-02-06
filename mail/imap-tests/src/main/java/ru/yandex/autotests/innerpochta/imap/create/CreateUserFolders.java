package ru.yandex.autotests.innerpochta.imap.create;

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
import ru.yandex.autotests.innerpochta.imap.consts.flags.FolderFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.sun.mail.imap.protocol.BASE64MailboxEncoder.encode;
import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.matchers.ListItemMatcher.listItem;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.ListRequest.list;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.cyrillic;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateName;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:02
 */
@Aqua.Test
@Title("Команда CREATE. Создание папок: рандомной, русской, папки со спец. символами")
@Features({ImapCmd.CREATE})
@Stories(MyStories.USER_FOLDERS)
@Description("Создаем различные папки, подпапки. Проверяем, что корректно создаются. " +
        "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class CreateUserFolders extends BaseTest {
    private static Class<?> currentClass = CreateUserFolders.class;

    private static final int LEVEL_OF_HIERARCHY = 3;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String folder;


    public CreateUserFolders(String folder) {
        this.folder = folder;
    }

    @Parameterized.Parameters(name = "folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("139")
    public void createFirstLevelFolder() throws Exception {
        imap.request(create(folder)).shouldBeOk();
        imap.list().shouldSeeFolder(folder);
        imap.request(list("\"\"", "*")).shouldBeOk()
                .withItem(listItem("|", folder, FolderFlags.UNMARKED.value(), FolderFlags.HAS_NO_CHILDREN.value()));
        imap.list().shouldSeeSystemFolders();
        imap.request(select(folder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(examine(folder)).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @Stories(MyStories.JIRA)
    @Description("[DARIA-5641]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("138")
    public void createSecondLevelFolder() throws Exception {
        String parent = generateName();
        String child = newFolder(parent, folder).fullName();
        imap.request(create(child)).shouldBeOk();
        imap.list().shouldSeeFolder(child);
        imap.list().shouldSeeSystemFolders();
        imap.request(select(child)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(examine(child)).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @Stories(MyStories.JIRA)
    @Description("[MAILPROTO-2109] Создаем папку вида: папка|любая_другая папка")
    @ru.yandex.qatools.allure.annotations.TestCaseId("141")
    public void createSecondLevelFolderWithAllKindsOfParent() throws Exception {
        String child = generateName();
        String full = newFolder(folder, child).fullName();
        imap.list().show();
        imap.request(create(full)).shouldBeOk();
        imap.list().shouldSeeFolder(full);
        imap.list().shouldSeeSystemFolders();
        imap.request(select(full)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(examine(full)).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }


    @Test
    @Description("Создаем папку на 3 уровне, с кириллицей посередине и сразу селектим ее, ожидая ОК")
    @ru.yandex.qatools.allure.annotations.TestCaseId("142")
    public void createThirdLevelFolderWithAllKindsOfParent() throws Exception {
        String child = encode(cyrillic());
        String full = newFolder(folder, child, folder).fullName();
        imap.request(create(full)).shouldBeOk();
        imap.request(select(full)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(examine(full)).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @Description("Создаем подпапку с именем папки\n" + "Ожидаемый результат: OK")
    @ru.yandex.qatools.allure.annotations.TestCaseId("143")
    public void createFolderWithSubfolderOfTheSameName() {
        imap.request(create(folder)).shouldBeOk();
        imap.list().shouldSeeFolder(folder);
        String child = newFolder(folder, folder).fullName();
        imap.request(create(child)).shouldBeOk();
        imap.list().shouldSeeFolder(folder);
        imap.request(select(child)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(examine(child)).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

// Next tests are commented out because they are broken
// We can't simple @Ignore them, because ya make exits with non-zero code
// Tests counts as "NOT_LAUNCHED" instead of "IGNORED" in ya make because they are parametrized
/*
    @Test
    @Description("Дважды создаем одну и ту же папку\n Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("144")
    public void doubleCreateTestOnFirstLevel() {
        imap.request(create(folder)).shouldBeOk();
        imap.request(create(folder)).shouldBeNo();
    }

    @Test
    @Description("Дважды создаем одну и ту же подпапку\n Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("145")
    public void doubleCreateUserFolderOnSecondLevel() {
        imap.request(create(folder)).shouldBeOk();
        imap.list().shouldSeeFolder(folder);
        String child = newFolder(folder, folder).fullName();
        imap.request(create(child)).shouldBeOk();
        imap.request(create(child)).shouldBeNo();
    }
*/

    @Test
    @Description("Создаем папку вида: папко|папко")
    @ru.yandex.qatools.allure.annotations.TestCaseId("146")
    public void createUserSubfolderInTheSameFolderOnSecondLevel() {
        FolderContainer container = newFolder(folder, folder);
        imap.request(create(container.fullName())).shouldBeOk();
        imap.list().shouldSeeFolder(container.parent());
        imap.list().shouldSeeFolder(container.fullName());
    }

    @Test
    @Description("Пробуем создать одну и ту же подпапку на одном уровнем разных папок")
    @ru.yandex.qatools.allure.annotations.TestCaseId("147")
    public void createSubfolderOnFolders() {
        FolderContainer folder1 = newFolder(Utils.generateName(), folder);
        FolderContainer folder2 = newFolder(Utils.generateName(), folder);

        imap.request(create(folder1.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folder1.foldersTreeAsList());

        imap.request(create(folder2.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(folder2.foldersTreeAsList());

        imap.list().shouldSeeSystemFolders();
    }

    @Test
    @Stories(MyStories.JIRA)
    @Description("Создаем иеархию папок [MAILPROTO-634], [MAILPROTO-1961]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("148")
    public void createHierarchy() {
        FolderContainer hierarchy = newFolder(LEVEL_OF_HIERARCHY).add(folder);
        imap.request(create(hierarchy.fullName())).shouldBeOk();
        imap.list().shouldSeeFolders(hierarchy.foldersTreeAsList());
    }
}
