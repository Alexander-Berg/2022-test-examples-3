package ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */
public interface QuickReply extends MailElement {

    @Name("Кнопка, по которой переходим в композ")
    @FindByCss(".ico_expand-compose.quickReply-icon")
    MailElement expandCompose();

    @Name("Поле для текста")
    @FindByCss("[class*='quickReplyInput__quickReplyInput-textarea--']")
    MailElement input();

    @Name("Задизэйбленная кнопка отправки")
    @FindByCss("[class*='quickReplyInput__quickReplyInput-sendButton--']:not([class*='quickReplyInput__quickReplyInput-sendButton_isActive--'])")
    MailElement disabledSend();

    @Name("Кнопка отправки")
    @FindByCss("[class*='quickReplyInput__quickReplyInput-sendButton_isActive--']")
    MailElement send();

    @Name("Блок умных ответов")
    @FindByCss(".quickReplySection-smartReplies")
    MailElement smartReply();

    @Name("Умные ответы")
    @FindByCss(".smartReplies-smartReply")
    ElementsCollection<MailElement> smartReplies();
}
