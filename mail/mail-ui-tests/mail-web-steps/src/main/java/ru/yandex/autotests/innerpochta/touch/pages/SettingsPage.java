package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.touch.pages.blocks.Popup;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

import io.qameta.atlas.webdriver.ElementsCollection;

/**
 * @author oleshko
 */

public interface SettingsPage extends MailPage {

    @Name("Пункт «Управление рассылками» в настройках")
    @FindByCss(".qa-subscriptions")
    MailElement subsSettingItem();

    // ПЕРЕКЛЮЧАТЕЛИ НАСТРОЕК

    @Name("Переключатель настройки рекламы")
    @FindByCss(".qa-adv .settingsToggler")
    MailElement advertToggler();

    @Name("Переключатель настройки рекламы: Включено")
    @FindByCss(".qa-adv .settingsToggler_on.settingsToggler")
    MailElement advertTogglerOn();

    @Name("Переключатель настройки табов")
    @FindByCss(".qa-tabs .settingsToggler")
    MailElement tabsToggler();

    @Name("Переключатель настройки табов: Включено")
    @FindByCss(".qa-tabs .settingsToggler_on")
    MailElement tabsTogglerOn();

    @Name("Переключатель настройки умных ответов")
    @FindByCss(".qa-smart_replies .settingsToggler")
    MailElement srToggler();

    @Name("Переключатель настройки умных ответов: Включено")
    @FindByCss(".qa-smart_replies .settingsToggler_on")
    MailElement srTogglerOn();

    // ОБЩИЕ ЭЛЕМЕНТЫ

    @Name("Попап на странице настроек")
    @FindByCss(".is-active.is-settings.popup")
    Popup popup();

    @Name("Разделы настроек")
    @FindByCss(".settingsItem")
    ElementsCollection<MailElement> settingsItem();

    @Name("Крестик")
    @FindByCss(".topBar-item_ico")
    MailElement closeBtn();

    @Name("Пункты в любом разделе настроек")
    @FindByCss(".settings-item")
    ElementsCollection<MailElement> settingSectionItems();

    // ОДИНАКОВЫЕ ЭЛЕМЕНТЫ НА СТРАНИЦАХ ПАПОК, МЕТОК И ПОДПИСЕЙ

    @Name("Кнопка «Назад» на планшетах в папках, метках и подписях")
    @FindByCss(".settings-back")
    MailElement backTablet();

    @Name("«Создать» на странице папок, меток и подписей")
    @FindByCss(".settings-actionLine_link")
    MailElement create();

    @Name("«Править» или «Готово» на странице папок, меток и подписей")
    @FindByCss(".topBar-item_second")
    MailElement editOrSave();

    @Name("«Готово» на странице папок, меток и подписей на планшетах")
    @FindByCss(".foldersAndLabels-buttons_ready")
    MailElement saveTablet();

    @Name("«Отмена» на странице папок, меток и подписей на планшетах")
    @FindByCss(".foldersAndLabels-buttons_cancel")
    MailElement cancelTablet();

    // НАСТРОЙКИ ПАПОК И МЕТОК

    @Name("«Править» на странице папок и меток на планшетах")
    @FindByCss(".preferences-title_action")
    MailElement editTablet();

    @Name("Список папок")
    @FindByCss(".settings-settingsFolder")
    ElementsCollection<MailElement> folders();

    @Name("Список меток")
    @FindByCss(".settingsLabel")
    ElementsCollection<MailElement> labels();

    @Name("Кнопки редактирования одной папки/метки")
    @FindByCss(".ico_settings-folder-edit")
    ElementsCollection<MailElement> editElement();

    @Name("Кнопки удаления одной папки/метки")
    @FindByCss(".ico_settings-folder-delete")
    ElementsCollection<MailElement> deleteElement();

    @Name("Поле ввода имени папки/метки")
    @FindByCss(".settings-componentNameInput input")
    MailElement nameInput();

    @Name("Таб создания метки")
    @FindByCss(".foldersAndLabels-chooseCreator_labels")
    MailElement labelTab();

    @Name("Список цветов в редактировании и создании метки")
    @FindByCss(".foldersAndLabels-labelColorsSelector_item")
    ElementsCollection<MailElement> colors();

    @Name("Ошибка об уже существующем имени в редактировании и создании")
    @FindByCss(".foldersAndLabels-errorMessage")
    MailElement nameExistError();

    @Name("Кнопка «Вложить в другую папку»")
    @FindByCss(".settings-parentFoldersSelected")
    MailElement putInFolder();

    @Name("Пункт «Не выбрано» на странице вложения папки")
    @FindByCss(".settingsFolder-item_notChosen")
    MailElement notChosen();

    @Name("Блок с подпапками")
    @FindByCss(".settingsFolder-subfolders")
    ElementsCollection<MailElement> subfolders();

    // НАСТРОЙКИ ПОДПИСЕЙ

    @Name("Раздел «Ваши подписи»")
    @FindByCss(".qa-signatures")
    MailElement signaturesItem();

    @Name("Подписи")
    @FindByCss(".settings-signature")
    ElementsCollection<MailElement> signatures();

    @Name("Кнопка «Удалить подпись»")
    @FindByCss(".qa-remove")
    MailElement removeSign();

    @Name("Заглушка на пустах страницах папок, меток и подписей")
    @FindByCss(".settings-preferencesItemDescription")
    MailElement emptyList();

    @Name("Инпут подписи")
    @FindByCss(".signatureEditor-input")
    MailElement signatureInput();
}
