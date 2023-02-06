package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface PassportPage extends MailPage {

    @Name("Инпут логина")
    @FindByCss("[name='login']")
    MailElement inputLogin();

    @Name("Инпут пароля")
    @FindByCss("[name='passwd']")
    MailElement inputPass();

    @Name("Кнопка «Войти»")
    @FindByCss(".passport-Button")
    MailElement logInBtnCorp();

    @Name("Кнопка «Войти»")
    @FindByCss(".passp-sign-in-button")
    MailElement logInBtn();

    @Name("Кнопка facebook в паспорте")
    @FindByCss(".PasspIcon_fb")
    MailElement fbBtn();

    @Name("Инпут логина на facebook")
    @FindByCss("input[name='email']")
    MailElement inputLoginOnFb();

    @Name("Инпут пароля на facebook")
    @FindByCss("input[name='pass']")
    MailElement inputPassOnFb();

    @Name("Кнопка «Войти» на facebook")
    @FindByCss("button[name='login']")
    MailElement logInOnFb();

    @Name("Кнопка «Войти» на facebook")
    @FindByCss("button[type='submit']")
    MailElement submitOnFb();
}
