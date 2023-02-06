package ru.yandex.autotests.innerpochta.imap.create;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.ImapConsts;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.CreateRequest;
import ru.yandex.autotests.innerpochta.imap.responses.CreateResponse;
import ru.yandex.autotests.innerpochta.imap.structures.FolderContainer;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.structures.FolderContainer.newFolder;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 27.03.14
 * Time: 15:12
 */
@Aqua.Test
@Title("Команда CREATE. Общие тесты")
@Features({ImapCmd.CREATE})
@Stories(MyStories.COMMON)
@Description("CREATE без параметров, создание папки с длинным именем и др")
public class CreateCommonTest extends BaseTest {
    private static Class<?> currentClass = CreateCommonTest.class;

    private static final int LEVEL_OF_HIERARCHY = 10;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @ClassRule
    public static ImapClient imap2 = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Description("CREATE без параметров")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("117")
    public void shouldNotCreateWithoutParam() {
        imap.request(create("")).shouldBeBad();
        imap.list().shouldSeeOnlySystemFoldersWithFlags();
    }

    @Description("Создаем папку с русским именем без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: folder encoding error")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("118")
    public void shouldNotCreateCyrillicFolder() {
        imap.request(CreateRequest.create(Utils.cyrillic())).shouldBeBad()
                .statusLineContains(CreateResponse.FOLDER_ENCODING_ERROR);
        imap.list().shouldSeeOnlySystemFoldersWithFlags();
    }

    @Test
    @Description("Создаем папку с слишком большим именем\n" +
            "Ожидаемый результат: Bad")
    @ru.yandex.qatools.allure.annotations.TestCaseId("120")
    public void shouldNotCreateFoldersWithLongName() {
        imap.request(create(ImapConsts.LONG_NAME)).shouldBeBad();
        imap.list().shouldNotSeeFolder(ImapConsts.LONG_NAME);
        imap.list().shouldSeeOnlySystemFoldersWithFlags();
    }

    @Description("Создаем иеарархию папок, состающую из очень большого количества папок [MAILPROTO-634]\n" +
            "Ожидаемый результат: BAD")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("122")
    public void shouldNotCreateBigHierarchy() {
        FolderContainer hierarchy = newFolder(LEVEL_OF_HIERARCHY);
        imap.request(create(hierarchy.fullName())).shouldBeBad();
    }


    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("123")
    public void shouldCreateWithTwoSession() {
        String folderName = Utils.generateName();
        imap.request(create(folderName)).shouldBeOk();
        imap2.list().shouldSeeFolder(folderName);
    }
}
