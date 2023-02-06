package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ChangeAliasDropdownBlock extends MailElement {

    @Name("Элемент в выпадающем списке")
    @FindByCss("._nb-select-item")
    ElementsCollection<MailElement> conditionsList();
}
