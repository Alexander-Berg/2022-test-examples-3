package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface TodoItemBlock extends MailElement {

    @Name("Название")
    @FindByCss(".qa-TodoItem-Title")
    MailElement itemTitle();

    @Name("Дата")
    @FindByCss(".qa-TodoItem-Date")
    MailElement itemDate();

    @Name("Кнопка редактировать")
    @FindByCss(".qa-TodoItem-Edit")
    MailElement editBtn();

    @Name("Кнопка удалить")
    @FindByCss(".qa-TodoItem-Delete")
    MailElement deleteBtn();

    @Name("Инпут ввода названия")
    @FindByCss(".qa-TodoItem-EditContent .textinput__control")
    MailElement inputName();

    @Name("Инпут ввода даты")
    @FindByCss(".qa-TodoItem-EditContent .qa-TodoItem-DateInput .react-datepicker__input")
    MailElement inputDate();

    @Name("Чекбокс для выполнения дела")
    @FindByCss(".qa-TodoItem-Checkbox")
    MailElement doneCheckBox();
}
