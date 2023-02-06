package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 13.09.12
 * Time: 14:14
 */

public interface PasswordConfirmationBlock extends MailElement {

    @Name("Поле ввода пароля")
    @FindByCss("[name = 'password']")
    MailElement passwordInbox();

    @Name("Кнопка подтвердить")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement submitPasswordButton();
}

