package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CleanFolderPopUp extends MailElement {

    @Name("Кнопка подтверждения очистки папки")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement confirmCleaningBtn();

    @Name("Закрыто окно очистки папки")
    @FindByCss(".b-popup__close")
    MailElement cancelBtn();

    @Name("Кнопка отмены очистки папки")
    @FindByCss("[data-dialog-action='dialog.cancel']")
    MailElement cancelButton();

    @Name("Дополнительные условия")
    @FindByCss(".b-teaser.js-teaser-toggle .b-link.b-link_js")
    MailElement advancedOptions();

    @Name("Письмо старше")
    @FindByCss(".js-old_f")
    MailElement msgDateSelect();

    @Name("Адрес содержит")
    @FindByCss("[id='from_f']")
    MailElement address();

    @Name("Тема содержит")
    @FindByCss("[id='subj_f']")
    MailElement subject();

    @Name("Переложить письма в удаленные")
    @FindByCss("[value='clear']")
    MailElement moveMessagesToTrash();

    @Name("Удалить письма навсегда")
    @FindByCss("[value='purge']")
    MailElement deleteMsgsBtn();

    @Name("Закрыто окно очистки папки")
    @FindByCss(".b-popup__close")
    MailElement closePopUpBtn();
}
