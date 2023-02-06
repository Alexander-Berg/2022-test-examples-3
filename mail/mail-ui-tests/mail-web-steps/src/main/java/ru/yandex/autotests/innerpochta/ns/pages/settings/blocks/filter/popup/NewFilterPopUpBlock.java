package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface NewFilterPopUpBlock extends MailElement {

    @Name("Кнопка «Отмена»")
    @FindByCss("button[data-dialog-action='dialog.cancel']")
    MailElement cancelButton();

    @Name("Кнопка «Создать правило»")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement submitFilterButton();

    @Name("Кнопка создания новой метки")
    @FindByCss(".b-link.js-filter-link")
    MailElement createComplexFilterLink();

    @Name("Поле «От кого»")
    @FindByCss("[id='sfcs-from']")
    MailElement fromInputBox();

    @Name("Поле «Тема»")
    @FindByCss("[id='sfcs-subject']")
    MailElement subjectInputBox();

    @Name("Сообщение «Необходимо заполнить хотя бы одно из полей»")
    @FindByCss(".b-notification.b-notification_error")
    MailElement emptyNotification();
}
