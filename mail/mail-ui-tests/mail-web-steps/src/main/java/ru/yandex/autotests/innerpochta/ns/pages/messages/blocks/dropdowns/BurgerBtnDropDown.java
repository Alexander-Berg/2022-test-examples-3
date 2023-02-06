package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 @author yaroslavna
 */
public interface BurgerBtnDropDown extends MailElement {

    @Name("Сервисы в бургере")
    @FindByCss(".mail-ui-IconList-Item")
    ElementsCollection<MailElement> burgerMenuItem();

    @Name("Ссылка «Все сервисы»")
    @FindByCss(".mail-Logo-Popup-all-services")
    MailElement allSevicesBtn();
}
