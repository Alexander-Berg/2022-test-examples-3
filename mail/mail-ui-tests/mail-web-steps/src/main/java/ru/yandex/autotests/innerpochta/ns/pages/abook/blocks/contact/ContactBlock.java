package ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.contact;

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
public interface ContactBlock extends MailElement {

    @Name("Имя контакта")
    @FindByCss(".mail-AbookEntry-Name")
    MailElement name();

    @Name("Email контакта")
    @FindByCss(".mail-AbookEntry-Emails")
    MailElement email();

    @Name("Чекбоксы контактов")
    @FindByCss(".js-abook-entry-checkbox-controller")
    MailElement contactCheckBox();

    @Name("Метка группы контакта")
    @FindByCss(".js-abook-label")
    ElementsCollection<MailElement> groupLabel();

    @Name("Показать «Ещё n» адресов контакта")
    @FindByCss(".js-abook-entry-remaining")
    MailElement remainingEmailsBtn();

    @Name("Показать «Ещё n» адресов контакта в попапе")
    @FindByCss(".js-abook-entry-popup-remaining")
    MailElement remainingEmailsInPopupBtn();

    @Name("Аватар контакта")
    @FindByCss(".mail-AbookEntry-Userpic")
    MailElement contactAvatar();

    @Name("Чекбоксы контактов")
    @FindByCss(".mail-AbookEntry-Checkbox-View-Content")
    MailElement contactAvatarWithCheckBox();
}
