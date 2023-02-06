package ru.yandex.autotests.innerpochta.imap.select;

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
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.responses.ExamineSelectResponse.Flags;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:16
 */
@Aqua.Test
@Title("Команда SELECT. Выбор папки")
@Features({ImapCmd.SELECT})
@Stories(MyStories.USER_FOLDERS)
@Description("Селектим папки, подпапки. Проверяем флаги. " +
        "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class SelectUserFolders extends BaseTest {
    private static Class<?> currentClass = SelectUserFolders.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String userFolder;


    public SelectUserFolders(String userFolder) {
        this.userFolder = userFolder;
    }

    @Parameterized.Parameters(name = "user_folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Test
    @Description("Селектим несуществующую папку (несколько раз)\n" +
            "Ожидаемый результат: NO")
    @ru.yandex.qatools.allure.annotations.TestCaseId("578")
    public void doubleSelectNonexistentFolder() {
        prodImap.list().shouldNotSeeFolder(userFolder);
        imap.request(select(userFolder)).shouldBeNo();
        imap.request(select(userFolder)).shouldBeNo();
    }

    @Test
    @Description("Селектим пользовательскую папку\n" +
            "Ожидаемый результат: OK\n" +
            "Проверяем наличие флагов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("580")
    public void simpleSelectUserFolderShouldSeeOk() {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);
        imap.request(select(userFolder)).repeatUntilOk(imap)
                .shouldContainFlags(Flags.values())
                .existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @Description("Выбираем сначала одну папку, затем другую")
    @ru.yandex.qatools.allure.annotations.TestCaseId("579")
    public void selectFolders() {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);

        String folderName = Utils.generateName();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);

        imap.request(select(folderName)).repeatUntilOk(imap).existsShouldBe(0).recentShouldBe(0);
        imap.request(select(userFolder)).repeatUntilOk(imap).existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @Description("Выбираем для чтения папку, затем селектим ее же")
    @ru.yandex.qatools.allure.annotations.TestCaseId("581")
    public void selectExaminedFolder() {
        prodImap.request(create(userFolder)).shouldBeOk();

        prodImap.list().shouldSeeFolder(userFolder);
        imap.list().shouldSeeFolder(userFolder);

        prodImap.request(examine(userFolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(select(userFolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @Description("Селектим пользовательскую подпапку\n" +
            "Ожидаемый результат: OK")
    @ru.yandex.qatools.allure.annotations.TestCaseId("582")
    public void selectUserSubfolderShouldSeeOk() {
        FolderContainer folder = newFolder(Utils.generateName(), userFolder);
        prodImap.request(create(folder.fullName())).shouldBeOk();
        imap.list().shouldSeeFolder(folder.fullName());
        imap.request(select(folder.fullName())).repeatUntilOk(imap).shouldBeOk()
                .existsShouldBe(0).recentShouldBe(0);
        //todo: добавить еще проверок флагов и сообщений
    }

    @Test
    @Description("Селектим папку. Делаем close. Снова селектим папку.\n" +
            "Ожидаемый результат: OK")
    @ru.yandex.qatools.allure.annotations.TestCaseId("577")
    public void testSelectWithClose() {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);
        imap.request(select(userFolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(unselect()).shouldBeOk();
        imap.request(select(userFolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Test
    @Description("Вставляем пару сообщений, проверяем что меняются EXIST и RECENT")
    @ru.yandex.qatools.allure.annotations.TestCaseId("583")
    public void selectFolderWithMessages() throws Exception {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);

        prodImap.append().appendRandomMessage(userFolder);
        prodImap.select().waitMsgs(userFolder, 1);

        imap.request(select(userFolder)).existsShouldBe(1).recentShouldBe(1);
    }
}
