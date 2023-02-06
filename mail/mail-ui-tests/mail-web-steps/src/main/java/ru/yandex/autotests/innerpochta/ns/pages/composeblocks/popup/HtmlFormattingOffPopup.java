package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface HtmlFormattingOffPopup extends MailElement {

    @Name("Кнопка Продолжить")
    @FindByCss(".js-resolve")
    MailElement continueButton();

    @Name("Кнопка Отменить")
    @FindByCss(".js-reject")
    MailElement cancelButton();
}
