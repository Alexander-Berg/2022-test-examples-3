package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.config.BlockCollectorServerSetup;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main.BlockConnectedCollectors;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main.newcollector.BlockCreateNewCollector;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main.newcollector.BlockNotifications;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupCollectors extends MailElement {

    @Name("Блок «Подключенные ящики»")
    @FindByCss(".b-pop")
    BlockConnectedCollectors blockConnected();

    @Name("Блок «Забирать почту из ящика»")
    @FindByCss(".b-form-layout_collectors")
    BlockCreateNewCollector blockNew();

    @Name("Кнопка «Включить сборщик»")
    @FindByCss("[type='submit']")
    MailElement turnOnCollector();

    @Name("Блок “Параметры вашего почтового сервера“")
    @FindByCss(".b-form-layout__block_server-settings")
    BlockCollectorServerSetup serverSetup();

    @Name("Уведомления об ошибках")
    @FindByCss("*")
    BlockNotifications notifications();

    @Name("Кнопка «Mail.ru» oAuth")
    @FindByCss(".mail-OAuthButton_mailru")
    MailElement mailruOauthCollectorBtn();
}
