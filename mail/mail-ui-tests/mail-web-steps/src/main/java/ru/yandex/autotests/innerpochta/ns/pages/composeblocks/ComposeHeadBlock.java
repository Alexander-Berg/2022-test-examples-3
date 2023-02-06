package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * User: lanwen
 * Date: 15.11.13
 * Time: 22:53
 */
public interface ComposeHeadBlock extends MailElement {

    @Name("Кнопка «Отправить», сверху")
    @FindByCss(".js-send-button")
    MailElement sendButton();

    @Name("Кнопка «Сохранить шаблон», сверху (в режиме шаблона)")
    @FindByCss(".js-save-button")
    MailElement saveTemplateButton();

    @Name("Список из двух кнопок Сохранить шаблон и Отправить")
    @FindByCss("button.nb-button")
    ElementsCollection<MailElement> buttonsList();

    @Name("Поле ввода «От кого»")
    @FindByCss(".ns-view-compose-from .mail-User-NameInput-Controller")
    MailElement fieldFromInput();

    @Name("Список алиасов в поле «От кого»")
    @FindByCss(".ns-view-compose-from .js-compose-changemail")
    MailElement aliasesDropBox();

    @Name("Крестик в правом верхнем углу шапки «Закрыть»")
    @FindByCss(".ns-view-compose-cancel-button")
    MailElement composeCancelBtn();

    @Name("Кнопка «Свернуть»/«Развернуть» в правом верхнем углу шапки")
    @FindByCss(".ns-view-compose-sidebar-button")
    MailElement composeSidebarOnOffBtn();

    @Name("Кнопка «Метка» в правом углу тулбара")
    @FindByCss(".ns-view-compose-label-button")
    MailElement labelMessageLink();

    @Name("Кнопка «Удалить» в правом углу тулбара")
    @FindByCss(".ns-view-compose-delete-button")
    MailElement composeDeleteBtn();

    @Name("Email в поле «От кого»")
    @FindByCss(".js-compose-changemail")
    MailElement fieldFromInputEmail();
}
