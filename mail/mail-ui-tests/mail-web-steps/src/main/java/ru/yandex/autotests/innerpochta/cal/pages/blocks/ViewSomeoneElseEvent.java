package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author eremin-n-s
 */
public interface ViewSomeoneElseEvent extends MailElement {

    @Name("Поле «Название»")
    @FindByCss("[class*=EventForm__title]")
    MailElement nameField();

    @Name("Символ повторения события")
    @FindByCss("[class*=EventRepetition__value]")
    MailElement repeatSymbol();

    @Name("Поле «Время и дата»")
    @FindByCss("[class*=EventDatesField__value]")
    MailElement timeAndDateField();

    @Name("Кнопка «Добавить в свой календарь")
    @FindByCss(".qa-EventForm-AttachButton")
    MailElement addBtn();

    @Name("Кнопка «Убрать из календаря")
    @FindByCss(".qa-EventForm-DetachButton")
    MailElement deleteBtn();

    @Name("Список стандартных уведомлений")
    @FindByCss("[class*=NotificationsFieldItem__wrap]")
    ElementsCollection <Notifications> notifyList();
}
