package ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MessagePageStickyToolBarBlock extends MailElement {

    @Name("Иконка на кнопке удалить письмо")
    @FindByCss(".js-toolbar-item-delete")
    MailElement deleteButtonIcon();

    @Name("Иконка на кнопке «Это спам»")
    @FindByCss(".js-toolbar-item-spam")
    MailElement spamButtonIcon();

    @Name("Кнопка удалить письмо")
    @FindByCss(".js-toolbar-item-delete:not(.b-toolbar__item_disabled)")
    MailElement deleteButton();

    @Name("Кнопка Спам")
    @FindByCss(".js-toolbar-item-spam")
    MailElement spamButton();

    @Name("Кнопка отметить прочитанным")
    @FindByCss(".js-toolbar-item-mark-as-read")
    MailElement markAsReadButton();

    @Name("Кнопка переслать")
    @FindByCss(".js-toolbar-item-forward")
    MailElement forwardButton();

    @Name("Кнопка отметить непрочитанным")
    @FindByCss(".js-toolbar-item-mark-as-unread")
    MailElement markAsUnreadButton();

    @Name("Кнопка “Закрепить“")
    @FindByCss(".js-toolbar-item-pin")
    MailElement pinBtn();

    @Name("Кнопка “Открепить“")
    @FindByCss(".js-toolbar-item-unpin")
    MailElement unPinBtn();
}



