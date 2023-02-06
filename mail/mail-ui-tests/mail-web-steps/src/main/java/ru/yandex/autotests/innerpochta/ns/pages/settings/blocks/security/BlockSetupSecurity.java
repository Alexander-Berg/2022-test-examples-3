package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.security;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupSecurity extends MailElement {

    @Name("Ссылка «Редактировать...»")
    @FindByCss("[href*='passport.yandex.ru/profile/emails']")
    MailElement editAliases();

    @Name("Ссылка «менять пароль»")
    @FindByCss("[href*='changepass']")
    MailElement changePasswordLink();

    @Name("Ссылка «Как придумать сложный пароль»")
    @FindByCss("[href*='yandex.ru/blog/mail/1063']")
    MailElement howtoCreateSecurePasswordLink();

    @Name("Ссылка «Номера телефонов»")
    @FindByCss("[href*='passport.yandex.ru/profile/phones']")
    MailElement phoneNumbersLink();

    @Name("Показать подсказку о номерах телефона (знак вопроса)")
    @FindByCss("[data-click-action='common.show-hint']")
    MailElement toggleHintAboutPhoneNumbers();

    @Name("Ссылка «Посмотреть журнал посещений»")
    @FindByCss("[href='#setup/journal']")
    MailElement journalLink();
}
