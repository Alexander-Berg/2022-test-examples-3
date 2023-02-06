package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface BlockWhiteList extends MailElement {

    @Name("Поле ввода адреса")
    @FindByCss(".js-input-email")
    MailElement emailField();

    @Name("Кнопка «Добавить»")
    @FindByCss(".js-button-submit")
    MailElement submitButton();

    @Name("Адреса в белом списке")
    @FindByCss(".js-email-from-list")
    ElementsCollection<MailElement> whitedAddressBlock();

    @Name("Блок адресов в списке")
    @FindByCss(".b-filters-list__added-list")
    MailElement whitedAddressesBlock();

    @Name("Кнопка «Удалить из списка»")
    @FindByCss(".js-delete-button")
    MailElement deleteButton();
}
