package ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CustomButtonsConfigureForwardButtonBlock extends MailElement {

    @Name("Кнопка 'Сохранить'")
    @FindByCss("[data-click-action='toolbar.settings.submitButtonOptions']")
    MailElement saveButton();

    @Name("Кнопка отмены")
    @FindByCss("input[data-click-action='toolbar.settings.back']")
    MailElement cancelButton();

    @Name("Ввод почты для переадресации")
    @FindByCss(".b-toolbar-settings__input[name='email']")
    MailElement emailInput();
}

