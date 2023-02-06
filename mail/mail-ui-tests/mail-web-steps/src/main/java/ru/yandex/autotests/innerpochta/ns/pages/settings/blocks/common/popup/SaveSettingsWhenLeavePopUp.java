package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.common.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SaveSettingsWhenLeavePopUp extends MailElement {

    @Name("Кнопка «Сохранить и перейти»")
    @FindByCss("[data-dialog-action='dialog.save']")
    MailElement saveAndContinueBtn();

    @Name("Кнопка «Не сохранять»")
    @FindByCss("[data-dialog-action='dialog.dont_save']")
    MailElement dontSaveBtn();

    @Name("Кнопка «Отмена»")
    @FindByCss("[data-dialog-action='dialog.cancel']")
    MailElement cancelBtn();

    @Name("Закрыть окно")
    @FindByCss(".b-popup__close")
    MailElement closePopUpBtn();
}
