package ru.yandex.autotests.innerpochta.touch.pages.loginblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface MordaMailPopup extends MailElement{

    @Name("Поле ввода логина")
    @FindByCss("[name='login']")
    MailElement login();

    @Name("Поле ввода пароля")
    @FindByCss("[name='passwd']")
    MailElement password();

    @Name("Кнопка входа")
    @FindByCss(".passport-Button")
    MailElement enter();
}
