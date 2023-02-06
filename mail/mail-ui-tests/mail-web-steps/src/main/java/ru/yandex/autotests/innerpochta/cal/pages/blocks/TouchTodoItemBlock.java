package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface TouchTodoItemBlock extends MailElement {

    @Name("Название")
    @FindByCss(".qa-TouchTodoItem-Title")
    MailElement itemName();

    @Name("Дата")
    @FindByCss(".qa-TouchTodoItem-Date")
    MailElement itemDate();

    @Name("Кнопка удалить")
    @FindByCss(".qa-TouchTodoItem-Delete")
    MailElement deleteBtn();

    @Name("Инпут ввода названия")
    @FindByCss(".qa-TouchTodoItem-TitleInput .textinput__control")
    MailElement inputName();

    @Name("Инпут ввода даты")
    @FindByCss(".qa-TouchTodoItem-EditContent [class*=TouchDatePickerNative] [class*=TouchNativeControl__input]")
    MailElement inputDate();

    @Name("Чекбокс для выполнения дела")
    @FindByCss(".qa-TouchTodoItem-Checkbox .checkbox__control")
    MailElement doneCheckBox();
}
