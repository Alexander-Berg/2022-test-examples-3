package ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by mabelpines on 23.06.15.
 */
public interface ClearFolderPopUp extends MailElement {

    @Name("Кнопка «Очистить»")
    @FindByCss(".qa-LeftColumn-ConfirmClear-ActionButton")
    MailElement clearFolderButton();

    @Name("Кнопка «Отменить»")
    @FindByCss(".qa-LeftColumn-ConfirmClear-CancelButton")
    MailElement cancelButton();

    @Name("Закрыть попап")
    @FindByCss(".qa-LeftColumn-ConfirmPopup-Close")
    MailElement closeButton();

    @Name("Кнопка «Очистить»")
    @FindByCss("button[data-dialog-action='dialog.submit']")
    MailElement clearFolderButtonOld();

    @Name("Кнопка «Отменить»")
    @FindByCss("button[data-dialog-action='dialog.cancel']")
    MailElement cancelButtonOld();

    @Name("Закрыть попап")
    @FindByCss(".b-popup__close")
    MailElement closeButtonOLd();
}
