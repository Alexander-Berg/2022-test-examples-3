package ru.yandex.autotests.innerpochta.imap.expunge;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.responses.NoopExpungeResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.expunge;
import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.uidExpunge;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 14.04.14
 * Time: 13:00
 */
@Aqua.Test
@Title("Команда EXPUNGE. Удаление писем")
@Features({ImapCmd.EXPUNGE})
@Stories(MyStories.COMMON)
@Description("Общие тесты на EXPUNGE. Делаем EXPUNGE без селекта")
public class ExpungeCommonTest extends BaseTest {
    private static Class<?> currentClass = ExpungeCommonTest.class;


    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Description("Пробуем выполнить EXPUNGE, без выбора папки\n"
            + "Ожидаемый результат: BAD")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("179")
    public void expungeWithoutSelect() {
        imap.request(unselect());
        imap.request(expunge()).shouldBeBad().statusLineContains(NoopExpungeResponse.WRONG_SESSION_STATE);
    }

    @Description("Делаем UID EXPUNGE с несуществующим UID")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("180")
    public void uidExpungeWithNoExistUid() {
        imap.select().inbox();
        imap.request(uidExpunge(String.valueOf(imap.status().getUidNext()))).shouldBeOk();
    }

}
