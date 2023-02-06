package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by cosmopanda on 22.04.2016.
 */

public interface ContainerPrevNextMsg extends MailElement{

    @Name("Тема сообщения")
    @FindByCss(".mail-Message-PrevNext_subject")
    MailElement subject();

    @Name("Имя отправителя")
    @FindByCss(".mail-Message-PrevNext_from")
    MailElement from();
}
