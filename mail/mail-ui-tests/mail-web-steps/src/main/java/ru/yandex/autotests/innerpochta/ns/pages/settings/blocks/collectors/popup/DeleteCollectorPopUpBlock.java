package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 04.04.13
 * Time: 16:06
 */
public interface DeleteCollectorPopUpBlock extends MailElement {

    @Name("Кнопка «Удалить» сборщик")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement deleteBtn();

    @Name("Кнопка «Отмена»")
    @FindByCss("[data-dialog-action='dialog.cancel']")
    MailElement cancelBtn();

    @Name("Сообщение")
    @FindByCss("p")
    MailElement message();
}
