package ru.yandex.autotests.innerpochta.ns.pages.lite;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 30.05.12
 * <p> Time: 15:51
 */
public interface MessageLitePage extends MailPage {

    // Кому
    @FindByCss(".b-message-head__subject-text")
    MailElement subjectHeader();

    // Тема
    @FindByCss(".b-message-body")
    MailElement msgBodyBlock();

    // Текст
    @FindByCss(".b-message-head")
    MailElement msgHeadBlock();

    // Флаг важности
    @FindByCss(".b-message-head .b-mail-icon_important")
    MailElement hightPriorityFlag();
}
