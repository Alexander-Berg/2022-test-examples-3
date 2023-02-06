package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.dropdown;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 04.03.14.
 */
public interface SelectConditionDropdown extends MailElement {

    @Name("Выбранный айтем в списке. С галочкой.")
    @FindByCss(".is-selected")
    MailElement selectedCondition();

    @Name("Список условий.")
    @FindByCss("._nb-select-item")
    ElementsCollection<MailElement> conditionsList();
}
