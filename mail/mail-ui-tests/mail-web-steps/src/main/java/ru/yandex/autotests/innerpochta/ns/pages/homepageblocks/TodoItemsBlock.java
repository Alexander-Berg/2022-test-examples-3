package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 11.02.16.
 */
public interface TodoItemsBlock  extends MailElement {

    String EMPTY_TODO_TEXT = "Дел нет\nЗаписывайте дела, чтобы не забывать о них, а чтоб получать " +
        "напоминания — добавляйте даты.";

    @Name("Блок с текстом внутри пустой тудушки")
    @FindByCss(".js-todo-items-wrap-dummy")
    MailElement emptyTodoLabel();

    @Name("Заголовок открытого списка дел")
    @FindByCss(".js-todo-header .b-window__title span")
    MailElement todoListTitle();

    @Name("Стрелочка “Назад“ в список Тудушек")
    @FindByCss(".js-todo-items-back")
    MailElement itemsBackLink();

    @Name("Стрелочка “Назад“ из системных списоков дел")
    @FindByCss(".js-todo-back")
    MailElement todoBackLink();

    @Name("Список дел")
    @FindByCss(".ns-view-todo-item")
    ElementsCollection<SingleTodoItemBlock> todoItems();

    @Name("Инпут для ввода нового дела")
    @FindByCss(".js-todo-create-input")
    MailElement newTodoItemInput();

    @Name("Кнопка “Добавить дело“")
    @FindByCss("button[type='submit']")
    MailElement  submitTodoItemBtn();

    @Name("Иконка - отправить список дел письмам")
    @FindByCss(".js-todo-items-send")
    MailElement sentTodoItemsIcon();

    @Name("Ссылка с датой")
    @FindByCss(".js-todo-item-goto-create")
    MailElement makeWithDateLink();

    @Name("Стрелка для минимизации окна")
    @FindByCss(".ns-view-todo-minimizer")
    MailElement minimize();
}
