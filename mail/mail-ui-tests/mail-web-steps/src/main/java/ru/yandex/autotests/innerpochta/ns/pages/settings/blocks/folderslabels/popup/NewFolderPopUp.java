package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface NewFolderPopUp extends MailElement {

    @Name("Поле ввода «Название» папки")
    @FindByCss(".js-input-name")
    MailElement folderName();

    @Name("Предупреждение о недопустимом имени папки")
    @FindByCss(".b-notification__text")
    MailElement invalidNameNotify();

    @Name("Кнопка «Создать папку»")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement create();

    @Name("Кнопка «Вложить в другую папку»")
    @FindByCss(".js-selink__folder")
    MailElement putInFolder();

    @Name("Ссылка для разворачивания фильтра")
    @FindByCss(".js-filter-open")
    MailElement filterLink();

    @Name("Форма создания простого фильтра")
    @FindByCss(".b-popup .b-form-layout_filters-simple")
    MailElement simpleFilter();

    @Name("Кнопка «Вложить в другую папку»")
    @FindByCss(".js-dropdown-folder-labels")
    MailElement selectFolderButton();
}
