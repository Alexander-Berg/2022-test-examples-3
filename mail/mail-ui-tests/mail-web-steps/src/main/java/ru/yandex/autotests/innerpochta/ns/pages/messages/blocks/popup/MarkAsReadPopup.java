package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by mabelpines on 08.02.16.
 */
public interface MarkAsReadPopup extends MailElement {

    @Name("Кнопка “Да, пометить“")
    @FindByCss(".qa-LeftColumn-ConfirmMarkRead-ActionButton")
    MailElement agreeBtn();

    @Name("Чекбокс “Больше не спрашивать“")
    @FindByCss(".qa-LeftColumn-ConfirmMarkRead-Checkbox")
    MailElement doNotAskAgainCheckbox();
}

