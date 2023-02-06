package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockConnectedCollector extends MailElement {

    @Name("Кнопка удаления ящика")
    @FindByCss(".b-pop__email-setup-link.js-collector-remove")
    MailElement deleteMailboxBtn();

    @Name("Кнопка настройки ящика")
    @FindByCss(".b-pop__email-setup-link:not(.js-collector-remove)")
    MailElement configureMailboxBtn();

    @Name("Имя сборщика")
    @FindByCss(".b-pop__email-value")
    MailElement collectorLink();

    @Name("Переключатель «вкл/выкл»")
    @FindByCss(".b-switch")
    MailElement switcher();

    @Name("Информация о сборщике")
    @FindByCss(".js-toggle-extra-info")
    MailElement showCollectorInfo();
}
