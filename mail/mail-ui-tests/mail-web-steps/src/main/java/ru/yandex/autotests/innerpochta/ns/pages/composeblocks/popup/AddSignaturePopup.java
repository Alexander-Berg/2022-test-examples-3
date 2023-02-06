package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface AddSignaturePopup extends MailElement {

    @Name("Поле ввода текста для подписи")
    @FindByCss(".js-signature-new")
    MailElement inputSignatureText();

    @Name("Кнопка «Сохранить»")
    @FindByCss(".js-save")
    MailElement saveButton();
}
