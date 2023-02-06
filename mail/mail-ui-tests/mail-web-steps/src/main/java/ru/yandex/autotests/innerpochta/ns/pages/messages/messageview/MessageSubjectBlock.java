package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MessageSubjectBlock extends MailElement {

    @Name("Тема письма")
    @FindByCss(".js-toolbar-subject")
    MailElement subject();

    @Name("Количество сообщений в треде")
    @FindByCss(".mail-MessageThread-MessagesCount_content")
    MailElement threadCount();

    @Name("Прыщ: тред прочитан")
    @FindByCss(".mail-Icon-Read:not(.is-active)")
    MailElement threadRead();

    @Name("Прыщ: тред непрочитан")
    @FindByCss(".mail-Icon-Read.is-active")
    MailElement threadUnread();

    @Name("Стрелка вверх")
    @FindByCss(".js-open-prev-thread")
    MailElement nextThread();

    @Name("Стрелка вниз")
    @FindByCss(".js-open-next-thread")
    MailElement prevThread();

    @Name("Кнопка вызова тулбара треда")
    @FindByCss(".js-toolbar-item-more")
    MailElement threadToolbarButton();
}
