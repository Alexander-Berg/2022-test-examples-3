package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author kurau
 */
public interface MessageMiscFieldBlock extends MailElement {

    @Name("Метка")
    @FindByCss("a.js-toolbar-item-labels-actions")
    MailElement label();

    @Name("Папка")
    @FindByCss("a.js-toolbar-item-folders-actions")
    MailElement folder();

    @Name("Закрепить")
    @FindByCss("a.js-toolbar-item-pin")
    MailElement pin();

    @Name("Создать правило")
    @FindByCss("[data-click-action='message.counter-filters-create']")
    MailElement createFilter();

    @Name("Свойства письма")
    @FindByCss("[data-click-action='message.counter-properties']")
    MailElement messageInfo();

    @Name("Архивировать")
    @FindByCss("a.js-toolbar-item-archive")
    MailElement archiveButton();

    @Name("Переслать")
    @FindByCss("a.js-toolbar-item-forward")
    MailElement forwardButton();

    @Name("Ответить")
    @FindByCss("a.js-toolbar-item-reply")
    MailElement replyButton();

    @Name("Распечатать")
    @FindByCss("a.js-kbd-print")
    MailElement printBtn();

    @Name("Все элементы")
    @FindByCss("._nb-popup-link")
    ElementsCollection<MailElement> allItems();

    @Name("Напомнить позже")
    @FindByCss(".js-toolbar-item-reply-later")
    MailElement remindLaterBtn();
}
