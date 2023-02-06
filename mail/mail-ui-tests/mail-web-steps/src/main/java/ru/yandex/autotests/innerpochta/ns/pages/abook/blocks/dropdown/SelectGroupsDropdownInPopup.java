package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.dropdown;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau
 */
public interface SelectGroupsDropdownInPopup extends MailElement {

    @Name("Список групп")
    @FindByCss(".ui-menu-item")
    ElementsCollection<MailElement> groupsNames();
}
