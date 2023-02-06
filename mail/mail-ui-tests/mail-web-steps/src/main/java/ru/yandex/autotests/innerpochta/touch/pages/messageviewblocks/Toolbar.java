package ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface Toolbar extends MailElement {

    @Name("Кнопка «Развернуть на весь экран» в тулбаре")
    @FindByCss(".messageHead-toolbarItem-fullscreen-open")
    MailElement fullScreenOpenBtn();

    @Name("Кнопка «Выйти из полноэкранного просмотра» в тулбаре")
    @FindByCss(".messageHead-toolbarItem-fullscreen-close")
    MailElement fullScreenCloseBtn();

    @Name("Кнопка «Ответить всем» в тулбаре")
    @FindByCss(".messageHead-toolbarItem-reply-all")
    MailElement replyAllBtn();

    @Name("Кнопка «Удалить» в тулбаре")
    @FindByCss(".messageHead-toolbar .messageHead-toolbarItem-trash")
    MailElement delete();

    @Name("Флажок важности")
    @FindByCss(".ico_important-active")
    MailElement importantLabel();

    @Name("Кнопка открытия черновика в композе")
    @FindByCss(".messageHead-toolbarItem-open-draft")
    MailElement openDraft();
}
