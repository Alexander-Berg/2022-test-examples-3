package ru.yandex.autotests.innerpochta.imap.pg;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.ExamineResponse;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

@Aqua.Test
@Title("Команда EXAMINE. Выбор папки для чтения. Общие тесты")
@Features({ImapCmd.EXAMINE, "PG"})
@Stories(MyStories.COMMON)
@Description("Выбираем для чтения папки, подпапки. Проверяем флаги.\n" +
        "Позитивное и негативное тестирование")
public class ExamineCommonPgTest extends BaseTest {
    private static Class<?> currentClass = ExamineCommonPgTest.class;


    @ClassRule
    public static final ImapClient imap = (newLoginedClient(currentClass));

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Description("EXAMINE без параметров\n" +
            "Ожидаемый результат: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("370")
    public void examineWithoutParamShouldSeeBad() {
        imap.request(examine("")).shouldBeBad();
    }

    @Test
    @Stories(MyStories.JIRA)
    @Description("EXAMINE кириллической папки без энкодинга [MAILPROTO-2141]\n" +
            "Должны увидеть: BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("369")
    public void examineCyrillicFolderShouldSeeBad() {
        imap.request(examine(Utils.cyrillic())).shouldBeBad()
                .statusLineContains(ExamineResponse.EXAMINE_FOLDER_ENCODING_ERROR);
    }
}
