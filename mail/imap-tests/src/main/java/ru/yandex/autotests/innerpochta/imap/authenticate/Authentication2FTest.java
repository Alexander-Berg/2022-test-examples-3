package ru.yandex.autotests.innerpochta.imap.authenticate;

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

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.requests.LogoutRequest.logout;

/**
 * Created by nmikutskiy on 26.09.16.
 */
@Aqua.Test
@Title("Команда AUTHENTICATE. Двухфакторная аутентификация.")
@Features({ImapCmd.AUTHENTICATE})
@Stories(MyStories.COMMON)
@Description("Проверяем AUTHENTICATE с 2FA")
public class Authentication2FTest extends BaseTest {
    private static Class<?> currentClass = Authentication2FTest.class;
    public static final String APPLICATION_PASSWORD = "hzyipfnndlzkevlo";

    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @Description("Аутенфикация через пароль приложений")
    public void authenticateWithAppPass() {
        imap.request(login(props().account(currentClass.getSimpleName()).getLogin(), APPLICATION_PASSWORD)).shouldBeOk();
        imap.request(logout()).shouldBeOk();
    }

    @Test
    @Description("Аутенфикация через обычный параль не должна работать")
    public void authenticateWithMainPass() {
        imap.request(login(currentClass.getSimpleName())).shouldBeNo();
    }

}
