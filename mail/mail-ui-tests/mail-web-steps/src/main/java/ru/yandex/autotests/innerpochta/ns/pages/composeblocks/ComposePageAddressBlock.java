package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.contact.ContactBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ComposePageAddressBlock extends MailElement {

    @Name("Кнопка «Готово»")
    @FindByCss(".js-abook-popup-ok")
    MailElement selectButton();

    @Name("Cелект для выбора группы")
    @FindByCss(".js-abook-head-box-group-select")
    MailElement selectGroupBtn();

    @Name("Показать все контакты")
    @FindByCss(".js-abook-load-all")
    MailElement showAllContactsBtn();

    @Name("Блок каждого контакта в адресной книге")
    @FindByCss(".ns-view-abook-contacts .js-abook-entry")
    ElementsCollection<ContactBlock> contacts();

    @Name("Чекбоксы в попапе в композе")
    @FindByCss(".js-abook-entry-single-popup-checkbox-controller")
    ElementsCollection<MailElement> abookAdressesCheckboxList();

    @Name("Инпут поиск по контактам")
    @FindByCss(".js-abook-head-search")
    MailElement searchInput();

    @Name("Чекбокс «Выбрать все контакты»")
    @FindByCss(".mail-AbookHead-SelectAll")
    MailElement checkAllContacts();

    @Name("Кнопка «Еще контакты»")
    @FindByCss(".js-abook-load-more")
    MailElement moreContactsBtn();
}
