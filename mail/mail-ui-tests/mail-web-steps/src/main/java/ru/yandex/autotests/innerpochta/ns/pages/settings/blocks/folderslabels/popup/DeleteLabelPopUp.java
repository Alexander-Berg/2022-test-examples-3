package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.09.12
 * Time: 17:13
 */
public interface DeleteLabelPopUp extends MailElement {

    @Name("Кнопка «Удалить метку»")
    @FindByCss(".qa-LeftColumn-ConfirmPopup-ActionButton")
    MailElement deleteBtn();

    @Name("Закрыть окно")
    @FindByCss(".qa-LeftColumn-ConfirmPopup-Close")
    MailElement closePopUpBtn();

    @Name("Кнопка «Отменить»")
    @FindByCss(".qa-LeftColumn-ConfirmPopup-CancelButton")
    MailElement cancelBtn();

    @Name("Ссылка «Моё правило»")
    @FindByCss("[href^='#setup/filters-create']")
    MailElement myFilter();

    @Name("Ссылка «в настройках»")
    @FindByCss("[href$='filters']")
    MailElement toSettings();

    @Name("Кнопка «Удалить метку»")
    @FindByCss("button[data-dialog-action='dialog.submit']")
    MailElement deleteBtnOld();

    @Name("Кнопка «Отменить»")
    @FindByCss("button[data-dialog-action='dialog.cancel']")
    MailElement cancelBtnOld();

    @Name("Закрыть окно")
    @FindByCss(".b-popup__close")
    MailElement closePopUpBtnOld();
}
