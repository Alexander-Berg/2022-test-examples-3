package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SettingsPageCreateSimpleFilterBlock extends MailElement {

    @Name("Поле «От кого»")
    @FindByCss("[id='sfcs-from']")
    MailElement fromInputBox();

    @Name("Поле «Тема»")
    @FindByCss("[id='sfcs-subject']")
    MailElement subjectInputBox();

    @Name("Выбор папки")
    @FindByCss(".setup-filters-create-simple-folder")
    Select folderSelect();

    @Name("Выбор метки")
    @FindByCss(".setup-filters-create-simple-label")
    MailElement selectLabelDropdown();

    @Name("Кнопка создания правила")
    @FindByCss("[type='submit']")
    MailElement submitFilterButton();

    @Name("Кнопка «Отмена»")
    @FindByCss("a[href='#setup/filters']")
    MailElement cancelButton();
}
