package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;


import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SaveAfterSendPopup extends MailElement {

    @Name("Включить")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement submitButton();

    @Name("Не включать")
    @FindByCss("[data-dialog-action='dialog.cancel']")
    MailElement cancelButton();

    @Name("Сообщение в попапе")
    @FindByCss("p")
    MailElement text();
}
