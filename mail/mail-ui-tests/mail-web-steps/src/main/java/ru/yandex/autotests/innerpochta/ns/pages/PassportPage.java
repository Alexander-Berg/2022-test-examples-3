package ru.yandex.autotests.innerpochta.ns.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface PassportPage extends MailPage {

    @Name("Логин в паспортном домике")
    @FindByCss("[name = 'login")
    MailElement login();

    @Name("Пароль в паспортном домике")
    @FindByCss("[name = 'passwd")
    MailElement psswd();

    @Name("Кнопка «Войти»")
    @FindByCss(".Button2_type_submit")
    MailElement submit();

    @Name("Кнопка «Войти»")
    @FindByCss(".passport-Button")
    MailElement submitCorp();

    @Name("Кнопка «Не сейчас» в промо привязки телефона")
    @FindByCss("[data-t = 'phone_skip']")
    MailElement notNowBtn();

    @Name("Кнопка «Не сейчас» в тачёвом промо привязки телефона")
    @FindByCss("[data-t = 'phone_skip']")
    MailElement touchNotNowBtn();

    @Name("Кнопка «Не сейчас» в промо привязки второго адреса")
    @FindByCss("[data-t = 'email_skip']")
    MailElement notNowEmailBtn();

    @Name("Кнопка «Пропустить» в промо загрузки аватара")
    @FindByCss(".registration__avatar-btn")
    MailElement skipAvatarBtn();

    //Паспорт меняет домик, пока выйгрывает новый и нужно юзать backToPrevStep()
    @Deprecated
    @Name("Ссылка «Вернуться на сервис»")
    @FindByCss(".passport-Domik-Retpath")
    MailElement backToService();

    @Name("Кнопка возвращения к предыдущемы степу")
    @FindByCss(".PreviousStepButton")
    MailElement backToPrevStep();

    @Name("Кнопка «Войти в другой аккаунт»")
    @FindByCss(".AddAccountButton")
    MailElement enterAnotherAcc();

    @Name("Поле логина")
    @FindByCss("[id = 'passp-field-login']")
    MailElement loginField();

    @Name("Поле паспорта")
    @FindByCss("[id = 'passp-field-passwd']")
    MailElement pwdField();

    @Name("Кнопка залогина через твиттер")
    @FindByCss(".AuthSocialBlock-provider_code_tw")
    MailElement twitterLogin();

    @Name("Логины на выбор при соцавторизации")
    @FindByCss(".s-profile-ya")
    ElementsCollection<MailElement> socialLogedInLogins();
}
