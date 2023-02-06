package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.common;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.09.12
 * Time: 17:08
 */

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SettingsPageNavigationBlock extends MailElement {

    @Name("Выбор оформления")
    @FindByCss("[href='#setup/interface']")
    MailElement interfaceSetupLink();

    @Name("Информация об отправителе")
    @FindByCss("[href='#setup/sender']")
    MailElement senderInfoSetupLink();

    @Name("Сбор почты с других ящиков")
    @FindByCss("[href='#setup/collectors']")
    MailElement collectorsSetupLink();

    @Name("Папки и метки")
    @FindByCss("[href='#setup/folders']")
    MailElement foldersAndLabelsLink();

    @Name("Правила обработки почты")
    @FindByCss("[href='#setup/filters']")
    MailElement filtersSetupLink();

    @Name("Безопасность")
    @FindByCss("[href='#setup/security']")
    MailElement securitySetupLink();

    @Name("Контакты")
    @FindByCss("[href='#setup/abook']")
    MailElement abookSetupLink();

    @Name("Дела")
    @FindByCss("[href='#setup/todo']")
    MailElement todoSetupLink();

    @Name("Подписки")
    @FindByCss("[href='#setup/lenta']")
    MailElement subscriptionsSetupLink();

    @Name("Почтовые программы")
    @FindByCss("[href='#setup/client']")
    MailElement mailClientsSetupLink();

    @Name("Прочие параметры")
    @FindByCss("[href='#setup/other']")
    MailElement otherSettingsSetupLink();

    @Name("Кнопка «Язык»")
    @FindByCss("div.b-setup-aside__item:nth-of-type(1) .b-selink")
    MailElement selectLang();

    @Name("Кнопка «Часы»")
    @FindByCss(".js-timezone")
    MailElement selectTime();

    @Name("Кнопка «Поменять пароль»")
    @FindByCss("[href^='https://passport.yandex.ru/profile/password']")
    MailElement changePassword();

    @Name("Кнопка «Указать свои данные»")
    @FindByCss("[href^='https://passport.yandex.ru/passport']")
    MailElement changeInfo();

    @Name("Кнопка «Изменить часовой пояс»")
    @FindByCss(".mail-Settings_time")
    MailElement changeTimeZone();
}
