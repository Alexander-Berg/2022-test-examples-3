package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by cosmopanda on 10.06.2016.
 */
public interface NotificationEventBlock extends MailElement {

    @Name("Название встречи")
    @FindByCss(".mail-Notifications-Item-EventName")
    MailElement titleEvent();

    @Name("Место встречи")
    @FindByCss(".mail-Notifications-Item-EventLocation")
    MailElement placeEvent();

    @Name("Имя отправителя")
    @FindByCss(".mail-Notifications-Item-Name")
    MailElement nameSender();

    @Name("Тема письма")
    @FindByCss(".mail-Notifications-Item-FirstLine")
    MailElement firstLine();

    @Name("Крести для закрытия")
    @FindByCss(".js-remove")
    MailElement closeEvent();
}
