package ru.yandex.autotests.innerpochta.pop3.common;

import java.io.EOFException;
import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.base.Pop3Cmd;
import ru.yandex.autotests.innerpochta.imap.core.pop3.Pop3Client;
import ru.yandex.autotests.innerpochta.pop3.base.BaseTest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

@Web
@Aqua.Test
@Features({MyStories.POP3})
@Stories({Pop3Cmd.USER, Pop3Cmd.PASS})
@Title("Авторизируемся через POP3 с неправильным паролем.")
@Description("Ожидаем, что соединение будет разовано")
public class LoginFailTest extends BaseTest {


    public static final String WRONG_PASS = "wrong";
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private Pop3Client pop3 = new Pop3Client().pop3(LOGIN_GROUP);

    @Test
    @Title("Должны разрывать сессию если логинимся с не подходящим пароль")
    @ru.yandex.qatools.allure.annotations.TestCaseId("662")
    public void shouldCloseConnectionAfterWrongPass() throws IOException {
        thrown.expect(EOFException.class);
        thrown.expectMessage("Connection closed without indication.");

        String pwd = pop3.getPass();

        pop3.withPass(WRONG_PASS)
                .connectWithoutLogin()
                .login(false);

        pop3.withPass(pwd).login(true);
    }

    @After
    public void disconnect() {
        pop3.disconnect();
    }

}
