package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.abook.BlockSetupAbook;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.abook.popup.NewContactGroupPopUp;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 04.09.12
 * Time: 15:45
 * To change this template use File | settings | File Templates.
 */
public interface AbookSettingsPage extends MailPage {

    @Name("Все настройки → Контакты")
    @FindByCss(".setup-abook")
    BlockSetupAbook blockSetupAbook();

    @Name("Попап «Создать новую группу»")
    @FindByCss(".b-popup__box__content")
    NewContactGroupPopUp newGroupPopup();

}
