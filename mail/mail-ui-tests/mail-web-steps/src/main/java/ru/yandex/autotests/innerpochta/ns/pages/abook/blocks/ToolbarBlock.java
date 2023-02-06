package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ToolbarBlock extends MailElement {

    @Name("Кнопка «Добавить контакт»")
    @FindByCss(".js-toolbar-item-abook-add")
    MailElement addContactButton();

    @Name("Кнопка «Добавить в группу» контакт")
    @FindByCss(".js-toolbar-item-title-abook-togroup:not(.b-toolbar__item_disabled)")
    MailElement addContactToGroupButton();

    @Name("Кнопка «Удалить контакт»")
    @FindByCss(".js-toolbar-item-abook-remove:not(.b-toolbar__item_disabled)")
    MailElement deleteContactButton();

    @Name("Кнопка «Написать» письмо")
    @FindByCss(".js-toolbar-item-compose-abook")
    MailElement composeButton();

    @Name("Поле поиска «Найти контакт»")
    @FindByCss("[name='text']")
    MailElement searchInput();

    @Name("Кнопка «Еще»")
    @FindByCss(".js-toolbar-item-abook-more")
    MailElement moreButton();

    @Name("Кнопка «Восстановить»")
    @FindByCss(".js-toolbar-item-abook-restore")
    MailElement restoreButton();

    @Name("Кнопка «Чекбокс все контакты»")
    @FindByCss(".js-toolbar-item-abook-select-all")
    MailElement selectAllContacts();
}




