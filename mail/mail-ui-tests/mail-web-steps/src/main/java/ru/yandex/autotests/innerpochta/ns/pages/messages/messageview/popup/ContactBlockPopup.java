package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mabelpines
 */

public interface ContactBlockPopup extends MailElement {

    @Name("Строка с email-адресом")
    @FindByCss(".mail-ContactMenu-Item_selectonly")
    MailElement emailAddress();

    @Name("Написать письмо")
    @FindByCss("[href*='#compose']")
    MailElement composeLetterBtn();

    @Name("В черный список")
    @FindByCss("[data-click-action='message.lists-add-from-message']")
    MailElement addToBlacklistBtn();

    @Name("В адресную книгу")
    @FindByCss("[data-click-action='message.add-contact']")
    MailElement addToAbookBtn();

    @Name("Скопировать адрес")
    @FindByCss(".js-clipboard")
    MailElement copyAddress();
}
