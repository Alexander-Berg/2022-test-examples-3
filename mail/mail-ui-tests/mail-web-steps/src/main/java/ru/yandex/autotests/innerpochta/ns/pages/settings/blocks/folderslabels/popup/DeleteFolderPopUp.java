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
public interface DeleteFolderPopUp extends MailElement{

    @Name("Кнопка «Удалить с письмами»")
    @FindByCss(".qa-LeftColumn-ConfirmPopup-ActionButton")
    MailElement confirmDelete();

    @Name("Закрыть окно подтверждения (крестик)")
    @FindByCss(".qa-LeftColumn-ConfirmPopup-Close")
    MailElement closePopUpButton();

    @Name("Кнопка «Отменить» удаление")
    @FindByCss(".qa-LeftColumn-ConfirmPopup-CancelButton")
    MailElement cancelButton();

    @Name("Сообщение о удалении писем")
    @FindByCss(".b-popup__body")
    MailElement notification();

    @Name("Ссылка на настройку сборщиков")
    @FindByCss("p a")
    MailElement collectorSettingsLink();
}
