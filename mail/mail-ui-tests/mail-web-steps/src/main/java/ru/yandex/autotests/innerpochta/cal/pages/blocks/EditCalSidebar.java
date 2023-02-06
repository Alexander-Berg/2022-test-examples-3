package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author eremin-n-s
 */
public interface EditCalSidebar extends MailElement {

    @Name("Название календаря")
    @FindByCss(".qa-LayerName .textinput__control")
    MailElement nameInput();

    @Name("Цвет")
    @FindByCss(".qa-ColorPicker-Color")
    ElementsCollection<MailElement> colors();

    @Name("Добавить уведомление")
    @FindByCss(".qa-NotificationsField-Add")
    MailElement addNotifyBtn();

    @Name("Чекбокс «События влияют на занятость»")
    @FindByCss("[class*=SidebarsFormCheckbox__wrap]:not(.qa-LayerExtraParams-IsDefault) .checkbox__box .checkbox__control")
    MailElement setEmploymentCheckbox();

    @Name("Сохранить")
    @FindByCss(".qa-EditLayer-Save")
    MailElement saveBtn();

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