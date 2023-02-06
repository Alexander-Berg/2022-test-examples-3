package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.other.BlockSetupOther;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 01.06.12
 * <p> Time: 16:43
 */
public interface OtherSettingsPage extends MailPage{

    @Name("Все настройки → Прочие параметры")
    @FindByCss(".ns-view-setup-other")
    BlockSetupOther blockSetupOther();
}
