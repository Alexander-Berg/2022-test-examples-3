package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ReplyNotificationBlock extends MailElement {

    @Name("Ответить")
    @FindByCss("[data-click-action='reply']")
    MailElement replyLink();

    @Name("Ответить всем")
    @FindByCss("[data-click-action='reply-all']")
    MailElement getReplyToAllLink();

    @Name("Чекбокс больше не спрашивать")
    @FindByCss("._nb-checkbox-label")
    MailElement getDoNotAskAgainCheckBox();
}

