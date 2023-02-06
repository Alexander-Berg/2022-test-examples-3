package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface MordaPage extends MailPage {

    @Name("Блок «Почта» на морде у телефонов")
    @FindByCss("[data-statlog='shortcut.mail']")
    MailElement mailBlockPhone();

    @Name("Блок «Почта» на морде у планшетов")
    @FindByCss(".desk-notif-card__login-new-item_mail")
    MailElement mailBlockTablet();

    @Name("Крестик в промо на морде")
    @FindByCss(".b-hs-balloon__close")
    MailElement closeMordaPromo();

    @Name("Инпут для ввода пароля")
    @FindByCss("[name = 'passwd'")
    MailElement inputPass();

    @Name("Инпут для ввода пароля")
    @FindByCss("[name = 'login'")
    MailElement inputLogin();

    @Name("Кнопка «Войти»")
    @FindByCss("[type='submit'")
    MailElement logInBtn();
}
