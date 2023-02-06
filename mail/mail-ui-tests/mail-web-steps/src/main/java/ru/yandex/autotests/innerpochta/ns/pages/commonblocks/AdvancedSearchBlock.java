package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface AdvancedSearchBlock extends MailElement {

    @Name("Кнопки расширенного поиска")
    @FindByCss(".mail-AdvancedSearch__button")
    ElementsCollection<MailElement> advancedSearchRows();
}
