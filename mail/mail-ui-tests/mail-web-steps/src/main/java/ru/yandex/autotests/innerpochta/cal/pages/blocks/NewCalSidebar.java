package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface NewCalSidebar extends MailElement {

    @Name("Название календаря")
    @FindByCss(".qa-LayerName .textinput__control")
    MailElement nameInput();

    @Name("Цвет")
    @FindByCss(".qa-ColorPicker-Color")
    ElementsCollection<MailElement> colors();

    @Name("Добавить уведомление")
    @FindByCss(".qa-NotificationsField-Add")
    MailElement addNotifyBtn();

    @Name("Чекбокс Сделать основным")
    @FindByCss(".qa-LayerExtraParams-IsDefault input")
    MailElement setDefaultCalCheckbox();

    @Name("Создать")
    @FindByCss(".qa-AddLayerNew-Create")
    MailElement createBtn();

    @Name("Сохранить")
    @FindByCss(".qa-EditLayer-Save")
    MailElement saveBtn();

    @Name("Импортировать")
    @FindByCss(".qa-AddLayerImport-Import")
    MailElement importBtn();

    @Name("Адрес календаря для импорта")
    @FindByCss(".qa-LayerUrl .textinput__control")
    MailElement urlInput();

    @Name("Кнопка «По ссылке»")
    @FindByCss(".qa-LayerImportFrom .radio-button__radio_side_right")
    MailElement fromLinkBtn();

    @Name("Кнопка «Из файла»")
    @FindByCss(".qa-LayerImportFrom .radio-button__radio_side_left")
    MailElement fromFileBtn();

    @Name("Кнопка «Выбрать файл»")
    @FindByCss(".attach__control")
    MailElement selectFileBtn();

    @Name("Импортировать в")
    @FindByCss(".qa-LayerReference .button2")
    MailElement layerSelect();

    @Name("Видимость создаваемых событий")
    @FindByCss(".radio-button__radio:not(.radiobox__radio_checked_yes)")
    MailElement eventVisibleOption();

    @Name("Кнопка «Отписаться»")
    @FindByCss(".qa-EditLayerUnsubscribe-Button")
    MailElement unsubscribe();

    @Name("Список стандартных уведомлений")
    @FindByCss("[class*=NotificationsFieldItem__wrap]")
    ElementsCollection <Notifications> notifyList();
}