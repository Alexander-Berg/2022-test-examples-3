package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.09.12
 * Time: 17:13
 */
public interface NewLabelPopUp extends MailElement {

    @Name("Поле ввода названия метки")
    @FindByCss(".b-form-element__input-text.js-input-name[name='label_name']")
    MailElement markNameInbox();

    @Name("Кнопка создания метки")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement createMarkButton();

    @Name("Ссылка для разворачивания фильтра")
    @FindByCss(".js-filter-open")
    MailElement filterLink();

    @Name("Форма создания простого фильтра")
    @FindByCss(".b-popup .b-form-layout_filters-simple")
    MailElement simpleFilter();
}
