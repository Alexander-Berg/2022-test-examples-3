package ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CustomButtonsConfigureLabelButtonBlock extends MailElement {

    @Name("Выбор метки")
    @FindByCss(".b-toolbar-settings__select[name='label']")
    Select labelSelect();

    @Name("Кнопка «Сохранить»")
    @FindByCss("[data-click-action='toolbar.settings.submitButtonOptions']")
    MailElement saveButton();

    @Name("Кнопка отмены")
    @FindByCss("input[data-click-action='toolbar.settings.back']")
    MailElement cancelButton();

    @Name("Ввод названия новой метки")
    @FindByCss(".b-form-element__input-text.js-input-name[name='label_name']")
    MailElement labelNameInput();

    @Name("Цвета метки")
    @FindByCss(".b-label.b-label_rounded.b-label_sample.js-label-sample")
    ElementsCollection<MailElement> labelColors();
}

