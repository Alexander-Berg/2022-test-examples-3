package ru.yandex.autotests.innerpochta.imap.starttls;

import org.junit.Ignore;
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
import static ru.yandex.autotests.innerpochta.imap.requests.LogoutRequest.logout;
import static ru.yandex.autotests.innerpochta.imap.requests.StartTlsRequest.startTls;
import static ru.yandex.autotests.innerpochta.imap.responses.StartTlsResponse.CLIENT_BUG_STARTTLS_ACTIVE;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.09.14
 * Time: 16:35
 */
@Aqua.Test
@Title("Команда STARTTLS. Общие тесты")
@Features({ImapCmd.STARTTLS})
@Stories(MyStories.COMMON)
@Description("Общие тесты на STARTTLS.\n " +
        "Проверяем, что корректно стартуем шифрованный трансфер и можем отвечать")
@Ignore
public class StartTlsCommonTest extends BaseTest {
    private static Class<?> currentClass = StartTlsCommonTest.class;


    @Rule
    public ImapClient imap = new ImapClient().onPlainPort();

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Description("Проверяем STARTTLS на 143 порту, начинаем общаться после комманды с шифрованием [MPROTO-335]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("588")
    public void startTlsAndEncryptTest() throws Throwable {
        imap.request(startTls()).shouldBeOk();
        imap.startEncrypt();
        //тут уже исходящий и входящий траффик шифрованные
        imap.request(login(currentClass.getSimpleName())).shouldBeOk();
        imap.request(logout()).shouldBeOk();
    }

    @Test
    @Stories(MyStories.DOUBLE_CMD)
    @Description("Дважды делаем STARTTLS")
    @ru.yandex.qatools.allure.annotations.TestCaseId("589")
    public void doubleStartTlsTest() throws Throwable {
        imap.request(startTls()).shouldBeOk();
        imap.startEncrypt();
        //тут уже исходящий и входящий траффик шифрованные
        imap.request(startTls()).shouldBeBad().statusLineContains(CLIENT_BUG_STARTTLS_ACTIVE);

        imap.request(login(currentClass.getSimpleName())).shouldBeOk();
        imap.request(logout()).shouldBeOk();
    }
}
