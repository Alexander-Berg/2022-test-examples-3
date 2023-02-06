package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface TouchTodoListBlock extends MailElement {

    @Name("Название дела")
    @FindByCss(".qa-TouchTodoList-Title")
    MailElement listTitle();

    @Name("Инпут ввода названия списка")
    @FindByCss(".qa-TouchTodoList-EditContent .textinput__control")
    MailElement inputTitle();

    @Name("Кнопка «Удалить список»")
    @FindByCss(".qa-TouchTodoList-Delete")
    MailElement deleteBtn();

    @Name("Кнопка сворачивания/разворачивания")
    @FindByCss(".qa-TodoList-Expander")
    MailElement expanderBtn();

    @Name("Дело")
    @FindByCss(".qa-TouchTodoItem")
    ElementsCollection<TouchTodoItemBlock> items();

    @Name("Кнопка «Новое дело»")
    @FindByCss(".qa-TouchTodoList-CreateItem")
    MailElement newTodoItemBtn();

}
