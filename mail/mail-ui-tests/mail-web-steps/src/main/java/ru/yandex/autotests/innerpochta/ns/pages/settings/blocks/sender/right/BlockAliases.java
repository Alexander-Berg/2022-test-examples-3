package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.right;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockAliases extends MailElement {

    @Name("Выбор домена")
    @FindByCss(".js-radio-select")
    ElementsCollection<MailElement> domainsList();

    @Name("Список адресов")
    @FindByCss("[name = 'default_email']")
    ElementsCollection<MailElement> logins();

    @Name("Ссылка «Добавить адрес...»")
    @FindByCss("a[href*='passport.yandex.ru/profile/emails']")
    MailElement addAddressLink();

    @Name("Блок каждого отдельго адреса. Радиобаттон + Выпадушка.")
    @FindByCss(".b-form-layout__line")
    ElementsCollection<UserAddressesBlock> userAdresses();

    @Name("Ссылка «Сделать адресом номер телефона»")
    @FindByCss("a[href*='passport.yandex.ru/profile/phones']")
    MailElement addPhoneLink();
}
