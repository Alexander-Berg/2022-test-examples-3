package ru.yandex.autotests.innerpochta.imap.select;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.ExamineSelectResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.03.14
 * Time: 19:35
 * <p/>
 * [MPROTO-1067]
 * [MAILPROTO-2141]
 */
@Aqua.Test
@Title("Команда SELECT. Общие тесты")
@Features({ImapCmd.SELECT})
@Stories(MyStories.COMMON)
@Description("Общие тесты на SELECT. SELECT без параметров")
public class SelectCommonTest extends BaseTest {
    private static Class<?> currentClass = SelectCommonTest.class;


    @ClassRule
    public static final ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Description("SELECT без параметров\n" +
            "Должны увидеть: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("569")
    public void selectWithoutParamShouldSeeBad() {
        imap.request(select("")).shouldBeBad();
    }

    @Test
    @Stories(MyStories.JIRA)
    @Description("SELECT кириллической папки без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("570")
    public void selectCyrillicFolderShouldSeeBad() {
        imap.request(select(Utils.cyrillic())).shouldBeBad()
                .statusLineContains(ExamineSelectResponse.SELECT_FOLDER_ENCODING_ERROR);
    }
}
