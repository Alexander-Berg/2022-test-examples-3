package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface TouchTodo extends MailElement {

    @Name("Кнопка «Назад» в списке дел")
    @FindByCss("[class*=TouchHeader__control]")
    MailElement back();

    @Name("Табы в делах")
    @FindByCss("[class*=TabsItem__wrap]")
    ElementsCollection<MailElement> tabs();

    @Name("Таб «Дела»")
    @FindByCss(".qa-Todo-TabAll")
    MailElement tabAll();

    @Name("Таб «Просроченные»")
    @FindByCss(".qa-Todo-TabExpired")
    MailElement tabExpired();

    @Name("Таб «Выполненные»")
    @FindByCss(".qa-Todo-TabCompleted")
    MailElement tabCompleted();

    @Name("Списки дел")
    @FindByCss("[class=qa-TouchTodoList]")
    ElementsCollection<TouchTodoListBlock> todoLists();

    @Name("Кнопка «Создать список дел»")
    @FindByCss(".qa-Todo-CreateList")
    MailElement createList();

    @Name("Кнопка «Создать список дел»")
    @FindByCss("[placeholder='Название списка']")
    MailElement inputTodoTitle();
}
