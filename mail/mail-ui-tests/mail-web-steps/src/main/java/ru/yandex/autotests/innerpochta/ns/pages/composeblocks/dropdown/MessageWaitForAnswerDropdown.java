package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MessageWaitForAnswerDropdown extends MailElement {

    @Name("Список")
    @FindByCss(".ui-corner-all")
    ElementsCollection<MailElement> itemList();
}
