package ru.yandex.autotests.innerpochta.ns.pages.nonmail;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SocialAuthorizationPages extends MailPage {

    @Name("Логин в твиттере")
    @FindByCss("[id='username_or_email']")
    MailElement twitterLogin();

    @Name("Пароль в твиттере")
    @FindByCss("[id='password']")
    MailElement twitterPwd();

    @Name("Кнопка входа в твиттере")
    @FindByCss(".submit.button.selected")
    MailElement twitterLogInButton();
}
