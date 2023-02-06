package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface AddContactPopup extends MailElement {

    @Name("Имя контакта")
    @FindByCss("[name='first_name']")
    MailElement name();

    @Name("Отчество контакта")
    @FindByCss("[name='middle_name']")
    MailElement middleName();

    @Name("Фамилия контакта")
    @FindByCss("[name='last_name']")
    MailElement lastName();

    @Name("Адрес контакта")
    @FindByCss("[name='mail_addr-0']")
    ElementsCollection<MailElement> addressNames();

    @Name("Инпут «Добавить адрес»")
    @FindByCss("[name='mail_addr_add']")
    MailElement addNewAddress();

    @Name("Список адресов контакта")
    @FindByCss("[name='mail_addr-0']")
    ElementsCollection<MailElement> contactAddresses();

    @Name("Номер телефона контакта")
    @FindByCss("[name='phone-0']")
    MailElement telNumber();

    @Name("Инпут «Добавить номер телефона»")
    @FindByCss("[name='phone_add']")
    MailElement addPhoneNumber();

    @Name("Комментарии")
    @FindByCss("[name='descr']")
    MailElement description();

    @Name("День рождения контакта")
    @FindByCss(".js-abook-birthday-select-day")
    MailElement contactBDay();

    @Name("Месяц рождения контакта")
    @FindByCss(".js-abook-birthday-select-month")
    MailElement contactBMonth();

    @Name("Год рождения контакта")
    @FindByCss(".js-abook-birthday-select-year")
    MailElement contactBYear();

    @Name("Кнопка «Сохранить изменения»")
    @FindByCss(".js-abook-person-edit-save")
    MailElement submitContactButton();

    @Name("Кнопка «Добавить в контакты»")
    @FindByCss(".js-abook-person-save")
    MailElement addContactButton();

    @Name("Кнопка «Закрыть»")
    @FindByCss("a._nb-popup-close")
    MailElement closeButton();

    @Name("Попап об ошибке")
    @FindByCss("._nb-error-popup")
    MailElement error();

}


