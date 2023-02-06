package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main.newcollector;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockNotifications extends MailElement {

    String CANT_CREATE_ON_CURRENT_TEXT = "Вы пытаетесь создать сборщик почты для текущего почтового ящика. \n" +
            "Для сортировки входящей почты в ящиках логин@yandex.ru, логин@yandex.ua, логин@yandex.com и логин@ya.ru " +
            "используйте фильтры.";


    @Name("Нотификация о пустом поле для почты")
    @FindByCss(".b-notification_error-required_email")
    MailElement emptyEmailNotification();

    @Name("Нотификация при попытке создать сборщик на текущий ящик")
    @FindByCss(".b-notification_this_server_belongs_to_yandex")
    MailElement currentEmailNotification();

    @Name("Нотификация о пустом поле пароля")
    @FindByCss(".b-notification_error-required_password")
    MailElement emptyPasswordNotification();

    @Name("Нотификация 'Сервер не отвечает, либо введен неверный логин или пароль.'")
    @FindByCss(".b-notification_auth_failed")
    MailElement noResponseNotification();

    @Name("Нотификация такой сборщик уже есть")
    @FindByCss(".b-notification_duplicated")
    MailElement alreadyExistsNotification();

    @Name("Ссылка на «фильтры» в нотифайке")
    @FindByCss(".b-notification__note>a")
    MailElement filtersLink();
}
