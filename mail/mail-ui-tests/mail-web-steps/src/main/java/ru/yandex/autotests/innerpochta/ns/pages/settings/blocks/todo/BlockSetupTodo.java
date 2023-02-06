package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.todo;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupTodo extends MailElement {

    String MOBILE_INFO = "Скачайте мобильную почту, чтобы ваши дела были всегда под рукой" +
            " (для смартфонов на iOS).\nСкачать приложение";

    @Name("Чекбокс «Показывать Дела на страницах почты»")
    @FindByCss(".daria-form-changeble")
    MailElement showTodoCheckbox();

    @Name("Ссылка «отменить изменения» (справа, сверху)")
    @FindByCss(".b-setup__inner__reset")
    MailElement cancelLink();

    @Name("Кнопка «сохранить изменения»")
    @FindByCss("[type='submit']")
    MailElement saveBtn();

    @Name("Блок описания «Скачайте мобильную почту...»")
    @FindByCss(".b-box.b-box_clear-01.b-box_imap")
    MailElement mobileInfo();

    @Name("Ссылка «Скачать приложение»")
    @FindByCss(".js-todo-go-mobile")
    MailElement downloadMobileLink();
}
