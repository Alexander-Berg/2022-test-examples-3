package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface Todo extends MailElement {

    @Name("Кнопка все Дела")
    @FindByCss(".qa-Todo-TabAll")
    MailElement allListsBtn();

    @Name("Кнопка Просроченные")
    @FindByCss(".qa-Todo-TabExpired")
    MailElement expiredListBtn();

    @Name("Кнопка Выполненные")
    @FindByCss(".qa-Todo-TabCompleted")
    MailElement completedListBtn();

    @Name("Кнопка Закрыть тудушку")
    @FindByCss(".qa-SidebarsHeader-Closer")
    MailElement closeTodoBtn();

    @Name("Кнопка Создать список дел")
    @FindByCss(".qa-Todo-CreateList")
    MailElement createListBtn();

    @Name("Список дел")
    @FindByCss(".qa-TodoList")
    ElementsCollection<TodoListBlock> lists();
}
