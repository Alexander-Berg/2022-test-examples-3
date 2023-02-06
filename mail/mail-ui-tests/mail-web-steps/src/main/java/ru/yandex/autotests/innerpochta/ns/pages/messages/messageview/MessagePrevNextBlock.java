package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MessagePrevNextBlock extends MailElement {

    @Name("Предыдущее сообщение")
    @FindByCss(".mail-Message-PrevNext_prev")
    ContainerPrevNextMsg prevMsg();

    @Name("Следующee сообщение")
    @FindByCss(".mail-Message-PrevNext_next")
    ContainerPrevNextMsg nextMsg();
}
