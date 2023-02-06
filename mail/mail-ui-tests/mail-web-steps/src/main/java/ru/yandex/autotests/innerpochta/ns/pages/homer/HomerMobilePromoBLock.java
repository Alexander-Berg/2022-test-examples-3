package ru.yandex.autotests.innerpochta.ns.pages.homer;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author crafty
 */
public interface HomerMobilePromoBLock extends MailElement {

    @Name("Поле ввода телефона")
    @FindByCss(".PromoMobile-Phone-Input .textinput__control")
    MailElement phoneInput();

    @Name("Кнопка «Отправить» в промо мобильных")
    @FindByCss(".PromoMobile-Phone-Send")
    MailElement sendSmsBtn();

    @Name("Блок с текстом мобильного промо")
    @FindByCss(".PromoMobile-Descr")
    MailElement textBlock();

    @Name("Блок с капчей")
    @FindByCss(".Captcha")
    MailElement captchaBlock();

    @Name("Капча - сама картинка")
    @FindByCss(".Captcha-Image")
    MailElement captchaImage();

    @Name("Поле ввода капчи - инпут")
    @FindByCss(".Captcha-Form-Text .textinput__control")
    MailElement captchaInput();

    @Name("Кнопка - ссылка на приложение в Google Play")
    @FindByCss(".PromoMobile-GPlay")
    MailElement googlePlayAppLinkBtn();

    @Name("Кнопка - ссылка на приложение в Itunes")
    @FindByCss(".PromoMobile-AppStore")
    MailElement itunesAppLinkBtn();
}