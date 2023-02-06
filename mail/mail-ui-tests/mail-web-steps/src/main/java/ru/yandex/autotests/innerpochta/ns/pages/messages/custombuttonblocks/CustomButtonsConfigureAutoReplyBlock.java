package ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CustomButtonsConfigureAutoReplyBlock extends MailElement {

    @Name("Окно ввода текста для автоответа")
    @FindByCss(".b-toolbar-settings_textarea")
    MailElement autoReplyTextInput();

    @Name("Выбор автоответа")
    @FindByCss(".b-toolbar-settings__select[name='tmpl']")
    Select autoReplySelect();

    @Name("Кнопка «Сохранить»")
    @FindByCss("[data-click-action='toolbar.settings.submitButtonOptions']")
    MailElement saveButton();

    @Name("Кнопка отмены")
    @FindByCss("input[data-click-action='toolbar.settings.back']")
    MailElement cancelButton();
}

