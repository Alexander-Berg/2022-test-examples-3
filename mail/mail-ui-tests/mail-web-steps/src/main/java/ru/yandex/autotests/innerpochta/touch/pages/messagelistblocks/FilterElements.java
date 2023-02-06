package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author  oleshko
 */

public interface FilterElements extends MailElement {

    @Name("Кнопка фильтров")
    @FindByCss(".messagesFilterBubbler")
    MailElement filterBubbler();

    @Name("Меню фильтров")
    @FindByCss(".messagesFilterTypes")
    MailElement filterTypes();

    @Name("Фильтр по людям")
    @FindByCss(".messagesFilterTypes-item_people")
    MailElement filterPeople();

    @Name("Фильтр покупок")
    @FindByCss(".messagesFilterTypes-item_eshops")
    MailElement filterEshops();

    @Name("Фильтр поездок")
    @FindByCss(".messagesFilterTypes-item_trips")
    MailElement filterTrips();

    @Name("Фильтр соцсетей")
    @FindByCss(".messagesFilterTypes-item_social")
    MailElement filterSocial();
}
