package ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface Translator extends MailElement {

    @Name("Кнопка «Перевести»")
    @FindByCss("[class*='translator__translator-submitButton--']")
    MailElement translateBtn();

    @Name("Крестик закрытия переводчика")
    @FindByCss("[class*='translator__translator-closeButton--']")
    MailElement closeBtn();

    @Name("Кнопка, открывающая список языков оригинала")
    @FindByCss("[class*='translator__translator-langSelect--']")
    MailElement sourceLangBtn();

    @Name("Кнопка, открывающая список языков перевода")
    @FindByCss("[class*='translator__translator-langSelect--']:nth-child(5)")
    MailElement translateLangBtn();

    @Name("Ошибка переводчика")
    @FindByCss("[class*='translator__translator-errorMessage--']")
    MailElement errorTranslate();

    @Name("Троббер на кнопке во время перевода")
    @FindByCss("[class*='Button__button-spinner--']")
    MailElement throbber();
}
