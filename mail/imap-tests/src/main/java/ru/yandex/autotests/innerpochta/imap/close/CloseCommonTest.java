package ru.yandex.autotests.innerpochta.imap.close;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.CloseResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CloseRequest.close;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.03.14
 * Time: 1:01
 * <p/>
 * Фактически CLOSE = EXPUNGE + UNSELECT
 */
@Aqua.Test
@Title("Команда CLOSE. Общие тесты")
@Features({ImapCmd.CLOSE})
@Stories(MyStories.COMMON)
@Description("Проверяем реакцию на закрытие без параметров")
public class CloseCommonTest extends BaseTest {
    private static Class<?> currentClass = CloseCommonTest.class;



    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Description("Делаем CLOSE без SELECT или EXAMINE")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("61")
    public void closeWithoutSelectOrExamine() {
        imap.request(unselect());
        imap.request(close()).shouldBeBad().statusLineContains(CloseResponse.WRONG_SESSION_STATE);
    }

    @Description("Делаем CLOSE без SELECT или EXAMINE дважды")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("62")
    public void doubleCloseWithoutSelectOrExamine() {
        imap.request(unselect());
        imap.request(close()).shouldBeBad().statusLineContains(CloseResponse.WRONG_SESSION_STATE);
        imap.request(close()).shouldBeBad().statusLineContains(CloseResponse.WRONG_SESSION_STATE);
    }

}
