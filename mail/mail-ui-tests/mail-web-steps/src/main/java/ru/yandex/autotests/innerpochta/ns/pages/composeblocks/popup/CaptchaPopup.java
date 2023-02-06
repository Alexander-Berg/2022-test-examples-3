package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;


/**
 * Created by mabelpines on 16.03.15.
 */

public interface CaptchaPopup extends MailElement{

    @Name("Форма капчи")
    @FindByCss(".ComposeReactCaptcha-Section")
    MailElement captchaForm();

    @Name("Изображение с капчой")
    @FindByCss(".ComposeReactCaptcha-Image")
    MailElement captchaImg();

    @Name("Инпут для ввода капчи")
    @FindByCss(".ComposeReactCaptcha-Input")
    MailElement captchaInput();

    @Name("Кнопка «Показать другую картинку»")
    @FindByCss(".ComposeReactCaptcha-Link")
    MailElement refreshCaptcha();

    @Name("Закрыть попап")
    @FindByCss(".ComposeConfirmPopup-Close")
    MailElement closeBtn();

    @Name("Кнопка «Отправить»")
    @FindByCss(".ComposeConfirmPopup-Button_action")
    MailElement sendBtn();
}
