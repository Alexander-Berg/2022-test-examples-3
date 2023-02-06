package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.09.12
 * Time: 17:13
 */
public interface DeleteFolderPopUpOld extends MailElement{

    @Name("Кнопка «Удалить с письмами»")
    @FindByCss("[data-dialog-action='dialog.submit']")
    MailElement confirmDelete();

    @Name("Закрыть окно подтверждения (крестик)")
    @FindByCss("div[data-click-action='dialog.cancel']")
    MailElement closePopUpButton();

    @Name("Кнопка «Отменить» удаление")
    @FindByCss("button[data-dialog-action='dialog.cancel']")
    MailElement cancelButton();

    @Name("Сообщение о удалении писем")
    @FindByCss(".b-popup__body")
    MailElement notification();

    @Name("Ссылка на настройку сборщиков")
    @FindByCss("p a")
    MailElement collectorSettingsLink();
}
