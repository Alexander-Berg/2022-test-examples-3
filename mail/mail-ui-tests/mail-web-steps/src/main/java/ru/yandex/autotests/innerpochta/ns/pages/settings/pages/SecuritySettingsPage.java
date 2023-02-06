package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.security.BlockSetupSecurity;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.security.SecurityHintPopup;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 04.09.12
 * Time: 15:45
 * To change this template use File | settings | File Templates.
 */
public interface SecuritySettingsPage extends MailPage {

    String PHONE_HINT_TEXT = "Вы сможете восстановить доступ к своей почте" +
        " с помощью смс, даже если забудете или потеряете пароль.";

    @Name("Все настройки → Безопасность")
    @FindByCss(".ns-view-setup-security")
    BlockSetupSecurity blockSecurity();

    @Name("Окно пояснения (тултип)")
    @FindByCss(".b-popup__box .b-popup__body")
    SecurityHintPopup hintPopUp();

}
