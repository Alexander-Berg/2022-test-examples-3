package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.mailclients.BlockMailClientsSetup;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 01.06.12
 * <p> Time: 15:21
 */
public interface MailClientsSettingsPage extends MailPage{

    @Name("Все настройки → Почтовые программы")
    @FindByCss(".ns-view-setup-client")
    BlockMailClientsSetup blockSetupClients();
}
