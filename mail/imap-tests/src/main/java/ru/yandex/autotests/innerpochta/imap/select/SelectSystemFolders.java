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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.data.TestData.allSystemFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.responses.ExamineSelectResponse.Flags.values;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.03.14
 * Time: 19:35
 */
@Aqua.Test
@Title("Команда SELECT. Выбор системных папок")
@Features({ImapCmd.SELECT})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Селекстим системные папки. Проверяем флаги. " +
        "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class SelectSystemFolders extends BaseTest {
    private static Class<?> currentClass = SelectSystemFolders.class;

    @ClassRule
    public static final ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;

    public SelectSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "system_folder - {0}")
    public static Collection<Object[]> sysFolders() {
        return allSystemFolders();
    }

    @Description("Просто выбираем системные папки")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("574")
    public void simpleSelectSystemFolder() {
        imap.request(select(sysFolder)).shouldBeOk().shouldContainFlags(values()).existsShouldBe(0).recentShouldBe(0);
    }

    @Description("Дважды выбираем системную папку")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("573")
    public void doubleSelectSystemFoldersShouldSeeOk() {
        imap.request(select(sysFolder)).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
        imap.request(select(sysFolder)).shouldBeOk().existsShouldBe(0).recentShouldBe(0);
    }

    @Description("Выбираем папку. Закрываем папку. Выбираем папку.")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("575")
    public void testSelectWithClose() {
    }


}
