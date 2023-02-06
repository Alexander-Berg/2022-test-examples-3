package ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CustomButtonsConfigureFolderButtonBlock extends MailElement {

    @Name("Выбор папки")
    @FindByCss(".b-toolbar-settings__select[name='folder']")
    Select folderSelect();

    @Name("Кнопка 'Сохранить'")
    @FindByCss("[data-click-action='toolbar.settings.submitButtonOptions']")
    MailElement saveButton();

    @Name("Кнопка отмены")
    @FindByCss("[data-click-action='toolbar.settings.back']")
    MailElement cancelButton();

    @Name("Ввод названия новой папки")
    @FindByCss("[name='folder_name']")
    MailElement folderNameInput();
}

