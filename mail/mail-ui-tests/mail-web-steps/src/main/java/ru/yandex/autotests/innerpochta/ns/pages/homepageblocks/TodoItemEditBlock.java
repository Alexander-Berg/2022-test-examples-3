package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 20.02.16.
 */
public interface TodoItemEditBlock extends MailElement {

    @Name("Кнопка “Сохранить“")
    @FindByCss("button[type='submit']")
    MailElement submitTodoItemBtn();

    @Name("Кнопка “Отменить“")
    @FindByCss(".js-todo-item-cancel")
    MailElement cancelEditBtn();

    @Name("Кнопка “Удалить“")
    @FindByCss(".js-todo-item-delete")
    MailElement deleteTodoItemBtn();

    @Name("Поле редактирования названия Дела")
    @FindByCss("input[name='title']")
    MailElement editTitleInput();

    @Name("Поле редактирования даты Дела")
    @FindByCss(".js-date-select")
    MailElement editDateInput();

    @Name("Поле редактирования времени Дела")
    @FindByCss("select[name='notification-time']")
    Select editTimeSelect();

    @Name("Сыылка “завтра“ под полем редактирования даты")
    @FindByCss(".js-todo-item-edit-set-date[data-offset='86400000']")
    MailElement setTomorrowDateBtn();
}
