package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface WidgetElements extends MailElement{

    @Name("Виджет PkPass")
    @FindByCss(".message_eticket_cinema")
    MailElement widgetPkpass();

    @Name("Виджет авиабилетов")
    @FindByCss(".message_eticket_avia")
    MailElement widgetAviatickets();

    @Name("Виджет отелей")
    @FindByCss(".message_eticket_hotel")
    MailElement widgetHotel();
}

