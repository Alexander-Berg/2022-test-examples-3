package ru.yandex.autotests.innerpochta.imap.pg;

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

@Aqua.Test
@Title("Команда EXAMINE. Выбор для чтения системных папок")
@Features({ImapCmd.EXAMINE, "PG"})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Селектим для чтения системные папки. Проверяем флаги. " +
        "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class ExamineSystemFoldersPgTest extends BaseTest {
    private static Class<?> currentClass = ExamineSystemFoldersPgTest.class;

    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));
    @Parameterized.Parameter(0)
    public String sysFolder;
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Parameterized.Parameters(name = "system_folder - {0}")
    public static Collection<Object[]> sysFolders() {
        return allSystemFolders();
    }

    @Description("Просто выбираем системные папки")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("371")
    public void simpleExamineSystemFolder() {
        imap.request(examine(sysFolder)).repeatUntilOk(imap).shouldBeOk();
    }

    @Description("Дважды выбираем системную папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("372")
    public void doubleExamineSystemFoldersShouldSeeOk() {
        imap.request(examine(sysFolder)).repeatUntilOk(imap).shouldBeOk();
        imap.request(examine(sysFolder)).repeatUntilOk(imap).shouldBeOk();
    }
}
