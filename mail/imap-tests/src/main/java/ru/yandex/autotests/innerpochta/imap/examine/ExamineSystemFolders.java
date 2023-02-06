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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.03.14
 * Time: 19:35
 */
@Aqua.Test
@Title("Команда EXAMINE. Выбор для чтения системных папок")
@Features({ImapCmd.EXAMINE})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Селектим для чтения системные папки. Проверяем флаги. " +
        "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class ExamineSystemFolders extends BaseTest {
    private static Class<?> currentClass = ExamineSystemFolders.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;


    public ExamineSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "system_folder - {0}")
    public static Collection<Object[]> sysFolders() {
        return allSystemFolders();
    }

    @Description("Просто выбираем системные папки")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("167")
    public void simpleExamineSystemFolder() {
        imap.request(examine(sysFolder)).repeatUntilOk(imap).shouldBeOk();
    }

    @Description("Дважды выбираем системную папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("168")
    public void doubleExamineSystemFoldersShouldSeeOk() {
        imap.request(examine(sysFolder)).repeatUntilOk(imap).shouldBeOk();
        imap.request(examine(sysFolder)).repeatUntilOk(imap).shouldBeOk();
    }

    @Description("Выбираем папку. Закрываем папку. Выбираем папку.")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("170")
    public void testExamineWithClose() {
    }
}
