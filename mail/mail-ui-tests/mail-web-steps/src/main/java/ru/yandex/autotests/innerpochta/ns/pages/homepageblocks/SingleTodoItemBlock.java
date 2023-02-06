package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 17.02.16.
 */
public interface SingleTodoItemBlock extends MailElement {

    @Name("Чекбокс напротив Дела")
    @FindByCss(".js-todo-item-toggle-complete")
    MailElement completeTodoItemCheckbox();

    @Name("Название Дела")
    @FindByCss(".b-grid-item__content__i")
    MailElement todoTitle();

    @Name("Дата Дела")
    @FindByCss(".b-grid-item__column")
    MailElement todoDate();
}
