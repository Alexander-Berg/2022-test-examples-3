package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface HostrootPage extends MailPage {

    @Name("Поле ввода логина")
    @FindByCss(".js-login")
    MailElement loginInput();

    @Name("Поле ввода пароля")
    @FindByCss(".js-password")
    MailElement passwordInput();

    @Name("Кнопка «Войти»")
    @FindByCss(".touch-enter-btn")
    MailElement enter();

    @Name("Кнопка ВКонтакте")
    @FindByCss(".b-mail-domik__social-icon_provider_vk")
    MailElement vkButton();

    @Name("Кнопка Facebook")
    @FindByCss(".b-mail-domik__social-icon_provider_fb")
    MailElement fbButton();

    @Name("Кнопка Twitter")
    @FindByCss(".b-mail-domik__social-icon_provider_tw")
    MailElement twButton();

    @Name("Кнопка Mail.ru")
    @FindByCss(".b-mail-domik__social-icon_provider_mr")
    MailElement mrButton();

    @Name("Кнопка Google+")
    @FindByCss(".b-mail-domik__social-icon_provider_gg")
    MailElement ggButton();

    @Name("Кнопка Одноклассники")
    @FindByCss(".b-mail-domik__social-icon_provider_ok")
    MailElement okButton();

    @Name("Ссылка «Регистрация»")
    @FindByCss(".touch-register-link")
    MailElement register();

    @Name("Страничка залогина")
    @FindByCss(".touch-auth")
    MailElement auth();
}
