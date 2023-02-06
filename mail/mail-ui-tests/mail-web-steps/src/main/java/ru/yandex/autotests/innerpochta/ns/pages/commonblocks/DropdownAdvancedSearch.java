package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by cosmopanda
 */
public interface DropdownAdvancedSearch extends MailElement {

    @Name("Список полей для выбора")
    @FindByCss(".menu__text")
    ElementsCollection<MailElement> fields();
}
