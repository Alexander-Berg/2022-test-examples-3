package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 24.06.14.
 */
public interface MailNotifyPopup extends MailElement {

    @Name("Выбрать через сколько напомнить")
    @FindByCss(".ComposeTimeOptions-Item")
    ElementsCollection<MailElement> waitTime();

    @Name("Опции оповещений")
    @FindByCss(".ComposeNotificationsOptions-Item .checkbox__control")
    ElementsCollection<MailElement> options();

    @Name("Хэдер напоминания")
    @FindByCss(".ComposeReplyNotificationOptions-Header")
    MailElement header();

    @Name("Чекбокс “Напоминать всегда“")
    @FindByCss(".js-compose-noreply-always-notify-checkbox input")
    MailElement notifyAlwaysCheckbox();

    @Name("Чекбокс  - “Напомнить, если не будет получен ответ в течение ... ")
    @FindByCss(".js-compose-noreply-toggle-checkbox input")
    MailElement notifyCheckbox();

    @Name("Выбрать через сколько напомнить")
    @FindByCss(".js-compose-noreply-days-select")
    MailElement notifyWaitTime();
}
