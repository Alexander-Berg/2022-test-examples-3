package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 11.02.16.
 */

public interface TodoListsBlock extends MailElement {

    @Name("Поле ввода названия нового списка дел")
    @FindByCss(".js-todo-lists-form input")
    MailElement todoTitleInput();

    @Name("Кнопка «Создать список»")
    @FindByCss(".js-todo-lists-form button")
    MailElement submitTodoBtn();

    @Name("Список тудушек")
    @FindByCss(".ns-view-todo-lists-item")
    ElementsCollection<SingleTodoBlock> todoList();

    @Name("Кнопка «свернуть»")
    @FindByCss(".ns-view-todo-minimizer")
    TodoListsBlock closeTodoBlockBtn();
}
