package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.other;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * User: lanwen
 * Date: 19.11.13
 * Time: 18:31
 */

public interface BlockSetupOtherBottom extends MailElement {

    //Цитирование
    @Name("«Цитировать исходное письмо при ответе»")
    @FindByCss("[name = 'enable_quoting']")
    MailElement enableQuoting();

    //Сохранять письма
    @Name("Сохранять письма в «Отправленные»")
    @FindByCss("[name = 'save_sent']")
    MailElement savingSent();

    @Name("«В папке «Черновики», автоматически до отправки»")
    @FindByCss("[name = 'enable_autosave']")
    MailElement enableAutosave();

    //Поиск писем
    @Name("«Показывать мои запросы в подсказке поиска»")
    @FindByCss("[name = 'dont_save_history']")
    MailElement dontSaveHistory();

    @Name("«После удаления письма переходить»")
    @FindByCss("[name = 'page_after_delete']")
    Select pageAfterDelete();

    //TODO: костыль!

    /**
     * 0 - Выпадушка <После отправки письма переходить> after_send
     * 1 - Выпадушка <После удаления письма переходить> after_delete
     * 2 - Выпадушка <После перемещения письма>         after_move
     * 3 - Выпадушка <Показывать браузерные уведомления о новых письмах>
     */
    @Name("Список из выпадающих кнопок")
    @FindByCss(".nb-select ._nb-button-content")
    ElementsCollection<MailElement> pageAfterOptionsList();

    @Name("Список из трёх выпадающих кнопок в 3pane")
    @FindByCss("div.b-setup__inner:nth-of-type(2) .nb-select")
    ElementsCollection<MailElement> pageAfterOptions3PaneList();

    @Name("«Показывать уведомления о письмах»")
    @FindByCss("[name = 'notify_message']")
    MailElement notifyMessage();

    @Name("Кнопка «сохранить изменения»")
    @FindByCss("button[type='submit']")
    MailElement save();

    @Name("Дропдаун настройки папок для браузерных пушей")
    @FindByCss(".js-selection-status")
    MailElement folderPushFoldedDropdown();

}
