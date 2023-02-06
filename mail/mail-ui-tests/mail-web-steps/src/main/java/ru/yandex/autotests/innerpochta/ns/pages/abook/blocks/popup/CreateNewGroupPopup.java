package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 03.10.12
 * Time: 18:30
 */
public interface CreateNewGroupPopup extends MailElement {

    @Name("Кнопка для выбора группы контактов")
    @FindByCss(".js-abook-head-box-group-select")
    MailElement changeGroupBtn();

    @Name("Кнопка «Создать группу»")
    @FindByCss(".js-group-create")
    MailElement createGroupButton();

    @Name("Поле ввода «Название группы»")
    @FindByCss("._nb-input-controller")
    MailElement groupNameInput();

    @Name("Контакты")
    @FindByCss(".js-abook-entry")
    ElementsCollection<MailElement> contacts();

    @Name("Кнопка «Выбрать все»")
    @FindByCss(".js-abook-head-toggler")
    MailElement selectAllContacts();

    @Name("Поле поиска контакта")
    @FindByCss(".js-abook-head-search")
    MailElement searchContactInput();

    @Name("Кнопка «Еще контакты»")
    @FindByCss(".js-abook-load-more")
    MailElement moreContactsBtn();

    @Name("Кнопка разворачивания адресов контакта")
    @FindByCss(".js-abook-entry-popup-remaining")
    MailElement moreEmailsInContactBtn();
}
