package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface ContactPopup extends MailElement {

    @Name("Имя контакта")
    @FindByCss(".mail-Abook-Person-Head-Name")
    MailElement contactName();

    @Name("Email контакта")
    @FindByCss(".ns-view-abook-person-email-list")
    ElementsCollection<MailElement> contactEmail();

    @Name("Телефон контакта")
    @FindByCss(".ns-view-abook-person-phone-list")
    ElementsCollection<MailElement> contactPhone();

    @Name("Дата рождения контакта")
    @FindByCss(".mail-Abook-Person-Birthday")
    MailElement contactBDay();

    @Name("Комментарий")
    @FindByCss(".mail-Abook-Person-Description")
    MailElement description();

    @Name("Кнопка «Закрыть»")
    @FindByCss("a._nb-popup-close")
    MailElement closeBtn();

    @Name("Кнопка «Написать письмо»")
    @FindByCss(".mail-Abook-Person-Head-Controls [href*='#compose']")
    MailElement composeBtn();

    @Name("Кнопка «Переписка»")
    @FindByCss(".js-abook-person-all-messages-link")
    MailElement showAllMsg();

    @Name("Кнопка «Все вложения»")
    @FindByCss(".js-abook-person-all-attachments-link")
    MailElement showAllAttach();

    @Name("Кнопка «В черный список»")
    @FindByCss(".js-abook-person-to-blacklist")
    MailElement toBlackListBtn();

    @Name("Кнопка «Добавить в контакты»")
    @FindByCss(".js-abook-person-save")
    MailElement saveToAbookBtn();

    @Name("Кнопка «Изменить»")
    @FindByCss(".js-abook-person-edit")
    MailElement editContactBtn();

    @Name("Кнопка «Создать фильтр»")
    @FindByCss(".mail-Abook-Person-Head-Controls [href*='#setup/filters-create']")
    MailElement createFilterBtn();

    @Name("Кнопка «Сохранить изменения»")
    @FindByCss(".js-abook-person-edit-save")
    MailElement saveChangesBtn();

    @Name("Поле «Добавить телефон»")
    @FindByCss(".ns-view-abook-person-phone-list .js-abook-person-list-item-add input")
    MailElement addNumberField();

    @Name("Крестик удаление группы контакта")
    @FindByCss(".mail-Abook-Person-Tags .js-abook-person-list-item-remove")
    ElementsCollection<MailElement> groupDeleteCrossList();

    @Name("Группы контакта")
    @FindByCss(".mail-Abook-Person-Tags")
    MailElement contactGroups();

    @Name("Поле для ввода коментария")
    @FindByCss(".mail-Abook-Person-Description-Input")
    MailElement descriptionInput();

    @Name("Должность общего контакта")
    @FindByCss(".mail-Abook-Person-Head-Role")
    ElementsCollection<MailElement> contactPost();

    @Name("Аватар контакта в карточке")
    @FindByCss(".mail-Abook-Person-Userpic")
    MailElement abookPersonAvatar();

    @Name("Добавить email")
    @FindByCss(".js-abook-person-list-item-add input")
    MailElement abookAddEmail();

}
