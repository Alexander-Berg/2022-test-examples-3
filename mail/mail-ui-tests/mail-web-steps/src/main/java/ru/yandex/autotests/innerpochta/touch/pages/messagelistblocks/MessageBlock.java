package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */
public interface MessageBlock extends MailElement {

    @Name("Аватарка на письме")
    @FindByCss(".avatar")
    MailElement avatar();

    @Name("Тема письма")
    @FindByCss(".messagesMessage-subject")
    MailElement subject();

    @Name("Метка письма")
    @FindByCss(".messagesMessageLabels-label")
    MailElement label();

    @Name("Счетчик писем в треде")
    @FindByCss(".messagesMessage-threadCounter")
    MailElement threadCounter();

    @Name("Флаг важности")
    @FindByCss(".messagesMessage-importantLabel")
    MailElement importantLabel();

    @Name("Аттачи на письме")
    @FindByCss(".messages-message__attachments .js-attachment-wrapper")
    ElementsCollection<MailElement> attachmentsInMessageList();

    @Name("Первая кнопка в свайп-меню")
    @FindByCss(".swipeAction.ico_more_swipe")
    MailElement swipeFirstBtn();

    @Name("Первая кнопка в свайп-меню черновика")
    @FindByCss(".messageActions-item_first")
    MailElement swipeFirstBtnDraft();

    @Name("Кнопка удаления в свайп-меню")
    @FindByCss(".swipeAction.ico_trash")
    MailElement swipeDelBtn();

    @Name("Прыщ непрочитанности")
    @FindByCss(".messagesMessage-unreadToggler")
    MailElement toggler();

    @Name("Прыщ: письмо непрочитано")
    @FindByCss(".is-unread .messagesMessage-unreadToggleInner")
    MailElement unreadToggler();

    @Name("Скрепка вместо аттачей")
    @FindByCss(".messagesMessage-attachmentIco")
    MailElement clipOnMsg();

    @Name("Стрелочка, намекающая поскролить вперёд")
    @FindByCss(".messages-message__attachments-arrow.is-visible")
    MailElement arrorNext();

    @Name("Стрелочка, намекающая поскролить назад")
    @FindByCss(".messages-message__attachments-arrow.is-reverse.is-visible")
    MailElement arrorBack();

    @Name("Метка «Ещё n»")
    @FindByCss(".messagesMessageLabels-labelMore")
    MailElement labelMore();

    @Name("Аватарка удалённого письма")
    @FindByCss(".userpic svg.ico_in-trash")
    MailElement avatarDeleteMsg();

    @Name("Ферстлайн")
    @FindByCss(".messagesMessage-firstline")
    MailElement firstline();
}
