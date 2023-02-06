package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface LanguagesDropDown extends MailElement {

    @Name("Список языков")
    @FindByCss("[data-click-action='setup.signature.change.lang']")
    ElementsCollection<MailElement> langList();
}
