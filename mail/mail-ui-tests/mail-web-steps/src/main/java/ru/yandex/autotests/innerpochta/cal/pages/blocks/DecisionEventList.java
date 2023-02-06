package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface DecisionEventList extends MailElement {

    @Name("Вариант «Возможно, пойду» в выпадушке решений")
    @FindByCss(".qa-EventDecision-MenuItemMaybe")
    MailElement maybe();

    @Name("Вариант «Пойду» в выпадушке решений")
    @FindByCss(".qa-EventDecision-MenuItemYes")
    MailElement yes();
}
