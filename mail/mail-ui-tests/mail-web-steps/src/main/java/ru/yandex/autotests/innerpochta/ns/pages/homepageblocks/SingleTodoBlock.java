package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * author @mabelpines
 */
public interface SingleTodoBlock extends MailElement {

    @Name("Редактировать Список дел (“Появляется по ховеру“)")
    @FindByCss(".js-todo-lists-item-edit")
    MailElement editTodoListBtn();

    @Name("Инпут редактирования имени Списка дел")
    @FindByCss(".js-todo-lists-item-input input")
    MailElement todoNameInput();

    @Name("Крестик в углу инпута - Очистить имя тудушки")
    @FindByCss(".js-todo-lists-item-input ._nb-input-reset")
    MailElement resetTodoNameInput();

    @Name("Кнопка “Сохранить“")
    @FindByCss("button[type='submit']")
    MailElement saveTodoBtn();

    @Name("Кнопка “Отменить“")
    @FindByCss(".js-todo-lists-item-close-edit")
    MailElement cancelTodoEditBtn();

    @Name("Кнопка “Удалить“")
    @FindByCss(".js-todo-lists-item-delete")
    MailElement deleteTodoBtn();

    @Name("Название Списка дел")
    @FindByCss(".js-todo-lists-item-go")
    MailElement title();
}
