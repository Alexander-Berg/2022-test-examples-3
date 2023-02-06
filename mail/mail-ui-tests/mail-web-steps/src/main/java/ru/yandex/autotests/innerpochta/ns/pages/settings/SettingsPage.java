package ru.yandex.autotests.innerpochta.ns.pages.settings;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.common.SettingsPageNavigationBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.common.popup.LanguageSelectionBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.common.popup.SaveSettingsWhenLeavePopUp;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.dropdown.SelectConditionDropdown;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.mailinterface.BlockSetupInterface;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.popup.AddImagePopup;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.todo.BlockSetupTodo;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 01.06.12
 * <p> Time: 15:21
 */
public interface SettingsPage extends MailPage {

    @Name("Все настройки → Дела")
    @FindByCss(".ns-view-setup-todo")
    BlockSetupTodo setupTodo();

    @Name("Все настройки → Оформление почты")
    @FindByCss(".ns-view-setup-interface")
    BlockSetupInterface setupInterface();

    @Name("Все настройки → Красивый @ адрес")
    @FindByCss(".js-setup-index-item_beautiful-email")
    MailElement domain();

    @Name("Все настройки → Управление рассылками")
    @FindByCss(".js-setup-item_unsubscribe-filters")
    MailElement subscriptions();

    @Name("Блок навигации по настройкам (слева)")
    @FindByCss(".ns-view-left-box")
    SettingsPageNavigationBlock blockSettingsNav();

    @Name("Попап «Сохранить сделанные изменения?»")
    @FindByCss(".b-popup:not(.g-hidden)")
    SaveSettingsWhenLeavePopUp saveSettingsPopUp();

    @Name("Попап-меню выбора языка")
    @FindByCss("[id = 'footer-langs-dropdown']")
    LanguageSelectionBlock languageSelect();

    @Name("Выпадающее меню c выбором условия")
    @FindByCss(".nb-select-dropdown .ui-autocomplete[style*='display: block;']")
    SelectConditionDropdown selectConditionDropdown();

    @Name("Попап добавления картинки в подпись")
    @FindByCss(".mail-Compose-AddImage-Popup-Wrapper")
    AddImagePopup addImagePopup();

    @Name("Попап")
    @FindByCss(".b-popup__box .b-popup__body")
    MailElement popup();

    @Name("Дропдаун")
    @FindByCss(".nb-select-dropdown .ui-autocomplete[style*='display: block();']")
    MailElement dropdown();

    @Name("Дропдаун для выбора часов или языка")
    @FindByCss(".b-mail-dropdown__box")
    MailElement langTimeDropdown();

    @Name("Сообщение «Некорректная ссылка» при добавлении картинки")
    @FindByCss("._nb-error-popup")
    MailElement addImageError();

    @Name("Развернутый дропдаун выбора папки для прихода пушей")
    @FindByCss(".mail-CheckboxTreePopup")
    MailElement folderListPushDropdown();

    @Name("Ссылка на сервисы в тулбаре")
    @FindByCss(".mail-MoreServices")
    MailElement services();
}
