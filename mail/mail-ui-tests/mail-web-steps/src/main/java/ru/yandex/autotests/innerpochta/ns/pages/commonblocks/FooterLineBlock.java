package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface FooterLineBlock extends MailElement {

    @Name("Ссылка на главную страницу")
    @FindByCss(".mail-ui-Link[href='https://yandex.ru']")
    MailElement mordaLink();

    @Name("Ссылка “Последний вход“")
    @FindByCss(".mail-ui-Link[href='#setup/journal']")
    MailElement journalLink();

    @Name("Смена языка")
    @FindByCss(".mail-App-Footer-Group:last-child.mail-App-Footer-Item")
    MailElement languageSwitch();

    @Name("Смена языка в 3pane")
    @FindByCss(".qa-LeftColumn-Footer-Language")
    MailElement languageSwitch3pane();

    @Name("Помощь")
    @FindByCss(".ns-view-footer-help-link")
    MailElement helpBtn();

    @Name("Ссылка на мобильную версию почты (android)")
    @FindByCss(".mail-ui-Link[href*='/apps/mail/android/']")
    MailElement androidLink();

    @Name("Ссылка на мобильную версию почты (ios)")
    @FindByCss(".mail-ui-Link[href*='/apps/mail/iphone/']")
    MailElement appleLink();

    @Name("Ссылка на рекламу")
    @FindByCss(".mail-ui-Link[href='https://yandex.ru/adv']")
    MailElement advertisingLink();

    @Name("Ссылка на лайт почту")
    @FindByCss(".mail-ui-Link[href='/u2709/go2lite']")
    MailElement liteMailLink();

    @Name("Смена языка в корп почте")
    @FindByCss(".mail-App-Footer-Group .mail-ui-ComplexLink")
    MailElement languageSwitchCorp();
}
