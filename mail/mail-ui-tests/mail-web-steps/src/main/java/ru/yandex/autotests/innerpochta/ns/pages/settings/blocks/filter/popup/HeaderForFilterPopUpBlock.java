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

public interface HeaderForFilterPopUpBlock extends MailElement {

    @Name("Поле ввода <Название заголовка>")
    @FindByCss("[value='sender']")
    MailElement headerInbox();

    @Name("Кнопка <Сохранить>")
    @FindByCss("button[data-dialog-action='dialog.submit']")
    MailElement saveHeaderButton();
}
