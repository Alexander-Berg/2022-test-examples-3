package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 24.02.16.
 */
public interface TodoListSentBlock extends MailElement {

    @Name("Поле ввода получателя списка дел")
    @FindByCss(".js-todo-email-input")
    MailElement todoEmailsInput();

    @Name("Кнопка “Отправить“")
    @FindByCss("button[type='submit']")
    MailElement sentTodoListBtn();
}
