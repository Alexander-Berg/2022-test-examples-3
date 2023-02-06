package ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MessagePageNotificationBlock extends MailElement {

    @Name("Ссылка на спамооборону")
    @FindByCss("a")
    MailElement spamProtectionLink();

    @Name("Кнопка “Очистить папку“")
    @FindByCss(".Button2_view_action")
    MailElement cleanSpamFolderButton();

    @Name("Кнопка “Создать шаблон“")
    @FindByCss(".js-create-template")
    MailElement createTemplateBtn();
}




