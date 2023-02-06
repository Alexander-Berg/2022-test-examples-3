package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface DeleteFilterPopUpBlock extends MailElement {

    @Name("Кнопка «Отмена»")
    @FindByCss("button[data-dialog-action='dialog.cancel']")
    MailElement cancelButton();

    @Name("Кнопка «Удалить правило»")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement deleteFilterButton();
}
