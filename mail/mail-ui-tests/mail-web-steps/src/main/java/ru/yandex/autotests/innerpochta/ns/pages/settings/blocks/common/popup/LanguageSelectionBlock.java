package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.common.popup;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 13.09.12
 * Time: 14:14
 */

public interface LanguageSelectionBlock extends MailElement {

    @Name("Языки")
    @FindByCss(".mail-ui-Link-Content")
    ElementsCollection<MailElement> languagesList();
}


