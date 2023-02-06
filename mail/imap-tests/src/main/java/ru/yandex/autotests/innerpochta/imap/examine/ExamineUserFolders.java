package ru.yandex.autotests.innerpochta.imap.examine;

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
import static ru.yandex.autotests.innerpochta.imap.responses.ExamineSelectResponse.Flags.values;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:11
 */
@Aqua.Test
@Title("Команда EXAMINE. Выбор папки для чтения")
@Features({ImapCmd.EXAMINE})
@Stories(MyStories.USER_FOLDERS)
@Description("Выбираем для чтения папки, подпапки. Проверяем флаги. " +
        "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class ExamineUserFolders extends BaseTest {
    private static Class<?> currentClass = ExamineUserFolders.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String userFolder;


    public ExamineUserFolders(String userFolder) {
        this.userFolder = userFolder;
    }

    @Parameterized.Parameters(name = "user_folder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Description("Селектим несуществующую папку\nОжидаемый результат: NO")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("172")
    public void testExamineNonexistentFolder() {
        imap.request(examine(userFolder)).repeatUntilNo(imap).shouldBeNo();
    }

    @Description("Селектим несуществующую папку несколько раз\nОжидаемый результат: NO")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("173")
    public void doubleExamineNonexistentFolder() {
        imap.request(examine(userFolder)).repeatUntilNo(imap).shouldBeNo();
        imap.request(examine(userFolder)).repeatUntilNo(imap).shouldBeNo();
    }

    @Description("Селектим пользовательскую папку\nОжидаемый результат: OK")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("174")
    public void simpleExamineUserFolderShouldSeeOk() {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);
        imap.request(examine(userFolder)).repeatUntilOk(imap)
                .shouldBeOk().shouldContainFlags(values()).existsShouldBe(0).recentShouldBe(0);
    }

    @Description("Выбираем для чтения сначала одну папку, затем другую")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("175")
    public void examineFolders() {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);

        String folderName = Utils.generateName();
        prodImap.request(create(folderName)).shouldBeOk();
        imap.list().shouldSeeFolder(folderName);

        imap.request(examine(folderName)).repeatUntilOk(imap).existsShouldBe(0).recentShouldBe(0);
        imap.request(examine(userFolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Description("Селектим папку, затем выбираем ее для чтения")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("176")
    public void examineSelectedFolder() {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);

        imap.request(select(userFolder)).repeatUntilOk(imap).existsShouldBe(0).recentShouldBe(0);
        imap.request(examine(userFolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Description("Селектим пользовательскую подпапку\nОжидаемый результат: OK")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("177")
    public void examineUserSubfolderShouldSeeOk() {
        FolderContainer folder = newFolder(Utils.generateName(), userFolder);
        prodImap.request(create(folder.fullName())).shouldBeOk();
        imap.list().shouldSeeFolder(folder.fullName());
        imap.request(examine(folder.fullName())).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);

    }

    @Description("Селектим папку. Делаем close. Снова селектим папку.\nОжидаемый результат: OK")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("171")
    public void testExamineWithClose() {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);
        imap.request(examine(userFolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(unselect()).shouldBeOk();
        imap.request(examine(userFolder)).repeatUntilOk(imap).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Description("Вставляем пару сообщений, проверяем что меняются EXIST и RECENT")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("178")
    public void examineFolderWithMessages() throws Exception {
        prodImap.request(create(userFolder)).shouldBeOk();
        imap.list().shouldSeeFolder(userFolder);

        prodImap.append().appendRandomMessage(userFolder);
        prodImap.select().waitMsgs(userFolder, 1);

        imap.request(examine(userFolder)).repeatUntilOk(imap).existsShouldBe(1).recentShouldBe(1);
    }
}
