package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface FilterList extends MailElement {

    @Name("Чекбоксы фильтрации переговорок")
    @FindByCss(".checkbox__control")
    ElementsCollection<MailElement> roomFilterCheckbox();
}
