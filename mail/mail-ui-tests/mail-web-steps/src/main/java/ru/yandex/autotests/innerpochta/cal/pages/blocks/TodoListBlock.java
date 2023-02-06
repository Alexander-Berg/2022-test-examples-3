package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface TodoListBlock extends MailElement {

    @Name("Название")
    @FindByCss(".qa-TodoList-Title")
    MailElement listTitle();

    @Name("Кнопка сворачивания/разворачивания")
    @FindByCss(".qa-TodoList-Expander")
    MailElement expanderBtn();

    @Name("Кнопка Создать дело")
    @FindByCss(".qa-TodoList-CreateItem")
    MailElement createTodoItemBtn();

    @Name("Кнопка редактировать")
    @FindByCss(".qa-TodoList-Edit")
    MailElement editBtn();

    @Name("Кнопка удалить")
    @FindByCss(".qa-TodoList-Delete")
    MailElement deleteBtn();

    @Name("Поле ввода названия дела")
    @FindByCss(".qa-TodoList-EditContent")
    MailElement fieldName();

    @Name("Инпут ввода названия списка дел")
    @FindByCss(".qa-TodoList-EditContent .textinput__control")
    MailElement inputName();

    @Name("Дело")
    @FindByCss(".qa-TodoItem")
    ElementsCollection<TodoItemBlock> items();

}
