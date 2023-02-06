package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 30.11.15.
 */
public interface InboxMsgInfoline extends MailElement{

    @Name("Количество выделенных сообщений")
    @FindByCss("[class*=MultipleEmailsSelected-m__text]")
    MailElement msgCount();

    @Name("Кнопка - “Снять выделение“")
    @FindByCss("[class*=MultipleEmailsSelected-m__close]")
    MailElement deselectLink();
}
