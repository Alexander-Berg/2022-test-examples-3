package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.CalendarBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 24.06.14.
 */
public interface SendTimePopup extends MailElement {

    @Name("Опции отложенной отправки")
    @FindByCss(".ComposeTimeOptions-Item")
    ElementsCollection<MailElement> options();

    @Name("Ссылка на каледарь")
    @FindByCss(".DelayedSendingOptions-CustomDate")
    MailElement setTimeDate();

    @Name("Кнопка помощи")
    @FindByCss(".js-help")
    MailElement helpBtn();

    @Name("Текст с подсказкой к отложенной отправке письма")
    @FindByCss(".js-compose-sendtime-help-text")
    MailElement sendtimeHelpText();

    @Name("Чекбокс <Отправить ... в>")
    @FindByCss(".b-compose__sendtime-pseudo-checkbox")
    MailElement sendTimeCheckbox();

    @Name("Ссылка на каледарь")
    @FindByCss(".DelayedSendingOptions-CustomDate")
    MailElement sendTimeDate();

    @Name("Сбросить")
    @FindByCss(".DelayedSendingOptions-ClearControl")
    MailElement reset();
}
