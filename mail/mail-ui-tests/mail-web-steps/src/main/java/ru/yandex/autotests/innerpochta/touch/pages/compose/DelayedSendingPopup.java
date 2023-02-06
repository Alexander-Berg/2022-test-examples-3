package ru.yandex.autotests.innerpochta.touch.pages.compose;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface DelayedSendingPopup extends MailElement {

    @Name("Пресеты для отложенной отправки")
    @FindByCss(".ComposeTimeOptions-Item")
    ElementsCollection<MailElement> presets();

    @Name("Кнопка «Выбрать дату и время»")
    @FindByCss(".DelayedSendingOptions-CustomDate")
    MailElement customDate();

    @Name("Кнопка «Сбросить»")
    @FindByCss(".DelayedSendingOptions-ClearControl")
    MailElement clearControl();

    @Name("Крестик закрытия попапов")
    @FindByCss(".ComposeBottomPopup-Close")
    MailElement closeBtn();
}
