package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.mailclients;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockPOP3Setup extends MailElement {

    @Name("Чекбокс «С сервера pop.yandex.ru по протоколу POP3»")
    @FindByCss(".js-enable-pop ._nb-checkbox-input")
    MailElement enablePopCheckbox();

    @Name("Ссылка «Выделить всё/снять выделение»")
    @FindByCss(".b-pseudo-link.js-enable-all-checkboxes")
    MailElement enableAllCheckboxes();

    @Name("Чекбоксы папок")
    @FindByCss("[name = 'fid']")
    ElementsCollection<MailElement> checkboxesOfFolders();

    @Name("Папки")
    @FindByCss(".b-form-layout__block_settings .b-form-element>label")
    ElementsCollection<MailElement> folders();
}