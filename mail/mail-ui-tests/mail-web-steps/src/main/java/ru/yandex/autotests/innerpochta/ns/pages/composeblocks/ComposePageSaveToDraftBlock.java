package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ComposePageSaveToDraftBlock extends MailElement {

    @Name("Кнопка «Сохранить и перейти»")
    @FindByCss("[data-action='save']")
    MailElement saveAndContinueButton();

    @Name("Кнопка «Не сохранять»")
    @FindByCss("[data-action='cancel']")
    MailElement doNotSaveButton();

    @Name("Кнопка «Отмена»")
    @FindByCss(".nb-button:nth-of-type(3)")
    MailElement cancelButton();
}
