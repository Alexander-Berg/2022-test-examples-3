package ru.yandex.autotests.innerpochta.ns.pages;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.ns.pages.homer.HomerFooterBlock;

/**
 * @author crafty
 */
public interface HomerPage extends MailPage {

    @Name("Гомер - футер")
    @FindByCss(".footer")
    HomerFooterBlock footerBlock();

    @Name("Кнопка Загрузить в AppStore")
    @FindByCss("[class*='Logo_appStore']")
    MailElement appStoreButton();

    @Name("Кнопка Загрузить в Google Play")
    @FindByCss("[class*='Logo_googlePlay']")
    MailElement googlePlayButton();

    @Name("Кнопка «Начать пользоваться»")
    @FindByCss("[href*='registration/mail?from=mail']")
    MailElement createAccountBtnHeadBanner();

    @Name("Кнопка «Войти» в шапке")
    @FindByCss(".PSHeader-NoLoginButton")
    MailElement logInBtnHeadBanner();

    @Name("Логотип Яндекс")
    @FindByCss(".PSHeaderLogo360-Ya")
    MailElement logoYandex();

    @Name("Вся страница полностью")
    @FindByCss("body")
    MailElement pageContent();

    @Name("Кнопка Начать с Премиум")
    @FindByCss(".Button2_view_premium")
    MailElement premiumButton();
}