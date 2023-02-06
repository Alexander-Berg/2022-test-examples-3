package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.config;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockCollectorServerSetup extends MailElement {

    @Name("Проверить соединение")
    @FindByCss(".js-collector-check")
    MailElement checkConnection();

    @Name("Соединение успешно установлено")
    @FindByCss(".b-notification.b-notification_success")
    MailElement successfulConnectionNotification();

    @Name("Выпадушка «Протокол»")
    @FindByCss(".b-form-layout__line ._nb-button-content")
    MailElement selectProtocol();

    @Name("Поле ввода «Логин»")
    @FindByCss("[name='login']")
    MailElement login();

    @Name("Поле ввода «Сервер»")
    @FindByCss("[name='server']")
    MailElement server();

    @Name("Поле ввода «Порт»")
    @FindByCss("[name='port']")
    MailElement port();

    @Name("Чекбокс «Использовать протокол шифрования SSL»")
    @FindByCss("[name='use_ssl']")
    MailElement useSsl();
}
