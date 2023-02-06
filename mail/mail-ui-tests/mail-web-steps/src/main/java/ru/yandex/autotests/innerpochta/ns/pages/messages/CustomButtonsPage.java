package ru.yandex.autotests.innerpochta.ns.pages.messages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks.CustomButtonsConfigureAutoReplyBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks.CustomButtonsConfigureFolderButtonBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks.CustomButtonsConfigureForwardButtonBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks.CustomButtonsConfigureLabelButtonBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks.CustomButtonsCreateButtonBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.05.13
 * Time: 15:34
 */
public interface CustomButtonsPage extends MailPage {

    @Name("Блок создания пользовательской кнопки")
    @FindByCss(".ns-view-toolbar-settings .js-toolbar-settings:not([style*='margin-left: -750px'])")
    CustomButtonsCreateButtonBlock overview();

    @Name("Блок создания кнопки для автоответа")
    @FindByCss(".js-toolbar-settings-form")
    CustomButtonsConfigureAutoReplyBlock autoReplyButtonConfigure();

    @Name("Блок создания кнопки для установки меток")
    @FindByCss(".js-toolbar-settings-form")
    CustomButtonsConfigureLabelButtonBlock configureLabelButton();

    @Name("Блок создания кнопки для перемещения писем в папку")
    @FindByCss(".js-toolbar-settings-form")
    CustomButtonsConfigureFolderButtonBlock configureFoldersButton();

    @Name("Блок создания кнопки для переадресации писем")
    @FindByCss(".js-toolbar-settings-form")
    CustomButtonsConfigureForwardButtonBlock configureForwardButton();
}
