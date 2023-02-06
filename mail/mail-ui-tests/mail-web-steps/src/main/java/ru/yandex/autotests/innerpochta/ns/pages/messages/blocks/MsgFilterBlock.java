package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by mabelpines
 */
public interface MsgFilterBlock extends MailElement{

    @Name("“Важные“")
    @FindByCss("[data-react-focusable-id='important'] a")
    MailElement showImportant();

    @Name("“Непрочитанные“")
    @FindByCss("[data-react-focusable-id='unread'] a")
    MailElement showUnread();

    @Name("“Аттачи“")
    @FindByCss("[data-react-focusable-id='attachments'] a")
    MailElement showWithAttach();

    @Name("“Ждут ответа“")
    @FindByCss("[data-react-focusable-id='remind'] a")
    MailElement waitForAnswer();
}
