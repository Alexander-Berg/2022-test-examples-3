package ru.yandex.autotests.innerpochta.touch.pages;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface ComposePage extends MailPage {

    @Name("Поле ввода тела письма")
    @FindByCss(".js-compose-body")
    MailElement inputBody();

    @Name("Кнопка «Отправить»")
    @FindByCss(".is-active .ico_send")
    MailElement sendBtn();

    @Name("Надпись «Новое письмо» в шапке")
    @FindByCss(".composeHead-title")
    MailElement composeTitle();

    @Name("Кнопка выхода из композа")
    @FindByCss(".composeHead-back")
    MailElement closeBtn();
}
