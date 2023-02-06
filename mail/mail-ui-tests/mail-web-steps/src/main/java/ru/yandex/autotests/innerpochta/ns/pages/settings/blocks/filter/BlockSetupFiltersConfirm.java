package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter;


/**
 * User: lanwen
 * Date: 19.11.13
 * Time: 13:36
 */

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupFiltersConfirm extends MailElement {

    @Name("Кнопка «Включить правило»")
    @FindByCss(".nb-button")
    MailElement turnOnForwardingButton();
}
