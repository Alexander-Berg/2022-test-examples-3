package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.abook.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface NewContactGroupPopUp extends MailElement {

    @Name("Кнопка «Создать группу»")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement сreateBtn();

    @Name("Поле ввода «Название группы»")
    @FindByCss(".js-setup-abook-popup-group-name")
    MailElement groupName();
}