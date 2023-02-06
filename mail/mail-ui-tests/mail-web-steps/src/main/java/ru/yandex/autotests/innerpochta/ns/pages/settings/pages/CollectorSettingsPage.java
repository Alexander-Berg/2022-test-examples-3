package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.BlockSetupCollector;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.BlockSetupCollectors;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main.BlockConnectedCollector;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.popup.DeleteCollectorPopUpBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.popup.MaxCollectorCountPopUp;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 04.09.12
 * Time: 15:45
 * To change this template use File | settings | File Templates.
 */
public interface CollectorSettingsPage extends MailPage {

    @Name("Все настройки → Сбор почты → Изменить правила работы сборщика")
    @FindByCss(".js-mail-layout-content")
    BlockSetupCollector blockSetup();

    @Name("Все настройки → Сбор почты")
    @FindByCss(".ns-view-setup-collectors")
    BlockSetupCollectors blockMain();

    @Name("Все настройки → Сбор почты (Подключенные ящики)")
    @FindByCss(".b-pop__email")
    BlockConnectedCollector blockCollector();

    @Name("Попап «Удаление сборщиков»")
    @FindByCss(".b-popup:not(.g-hidden)")
    DeleteCollectorPopUpBlock deleteCollectorPopUp();

    @Name("Попап «Внимание» (Можно создавать не более 10 сборщиков...)")
    @FindByCss(".b-popup:not(.g-hidden)")
    MaxCollectorCountPopUp maxCollectorCountPopUp();

    @Name("Нотификация 'Сервер не отвечает, либо введен неверный логин или пароль.'")
    @FindByCss(".b-notification_auth_failed")
    MailElement noResponseNotification();

}
