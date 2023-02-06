package ru.yandex.autotests.innerpochta.imap.starttls;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.StartTlsRequest.startTls;
import static ru.yandex.autotests.innerpochta.imap.responses.StartTlsResponse.CLIENT_BUG_STARTTLS_ACTIVE;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.09.14
 * Time: 18:59
 * <p>
 * MPROTO-335
 * MAILPROTO-2360
 */
@Aqua.Test
@Title("Команда STARTTLS. Тесты на 993 порту")
@Features({ImapCmd.STARTTLS})
@Stories(MyStories.COMMON)
@Description("Делаем STARTTLS на 993 порту")
public class StartTlsBadTest extends BaseTest {
    private static Class<?> currentClass = StartTlsBadTest.class;

    @ClassRule
    public static ImapClient imap = new ImapClient().onSslPort();
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Description("Делаем STARTTLS на 993 порту, должны увидеть BAD [MPROTO-335]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("586")
    public void startTlsBeforeLoginOn993ShouldSeeBad() {
        imap.request(startTls()).shouldBeBad().statusLineContains(CLIENT_BUG_STARTTLS_ACTIVE);
    }

    @Test
    @Description("Делаем после логина STARTTLS на 993 порту, должны увидеть BAD [MPROTO-335]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("587")
    public void startTlsAfterLoginOn993ShouldSeeBad() {
        imap.request(login(currentClass.getSimpleName())).shouldBeOk();
        imap.request(startTls()).shouldBeBad().statusLineContains(CLIENT_BUG_STARTTLS_ACTIVE);
    }
}
