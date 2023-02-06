package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ComposeYabbleDropdown extends MailElement {

    @Name("Выбрать другой емейл контакта")
    @FindByCss(".js-bubble-change-email")
    MailElement changeEmail();

    @Name("Скопировать емейл")
    @FindByCss(".js-bubble-copy")
    MailElement copyEmail();

    @Name("Редактировать адрес")
    @FindByCss(".js-bubble-edit")
    MailElement editYabble();

    @Name("Написать только этому получателю")
    @FindByCss(".js-bubble-single-target")
    MailElement singleTarget();

    @Name("Удалить из получателей")
    @FindByCss(".js-bubble-remove")
    MailElement removeFromRecipients();

    @Name("Первый чекбокс в выпадушке группового ябла")
    @FindByCss(".b-mail-dropdown__item[data-nb='checkbox']")
    MailElement groupYabbleCheckbox();
}
