package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main.newcollector;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.BlockSetupCollector;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockCreateNewCollector extends MailElement {

    @Name("Поле ввода «E-mail»")
    @FindByCss("[name = 'email']")
    MailElement email();

    @Name("Поле ввода «Пароль»")
    @FindByCss("[name = 'password']")
    MailElement password();

    @Name("Все настройки → Сбор почты → Изменить правила работы сборщика")
    @FindByCss(".ns-view-setup-collector")
    BlockSetupCollector settings();

    @Name("Уведомления об ошибках")
    @FindByCss("*")
    BlockNotifications notifications();
}
