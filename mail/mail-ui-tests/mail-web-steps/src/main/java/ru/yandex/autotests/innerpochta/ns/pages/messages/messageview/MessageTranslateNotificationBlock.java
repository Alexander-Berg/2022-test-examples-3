package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;


/**
 * @author a-zoshchuk
 */
public interface MessageTranslateNotificationBlock extends MailElement {

    @Name("Текст сообщения")
    @FindByCss(".qa-MessageViewer-TranslateControls")
    MailElement text();

    @Name("Кнопка «Перевести»")
    @FindByCss(".qa-MessageViewer-TranslateControls-Button")
    MailElement translateButton();

    @Name("Крестик")
    @FindByCss(".qa-MessageViewer-Widget-Close")
    MailElement closeButton();

    @Name("Вопросик")
    @FindByCss(".qa-MessageViewer-Widget-RightIcon")
    MailElement helpButton();

    @Name("Кнопка «Посмотреть оригинал»")
    @FindByCss(".qa-MessageViewer-RevertControls-revert")
    MailElement revert();

    @Name("Текст сообщения после перевода")
    @FindByCss(".qa-MessageViewer-RevertControls")
    MailElement textTranslated();

    }
