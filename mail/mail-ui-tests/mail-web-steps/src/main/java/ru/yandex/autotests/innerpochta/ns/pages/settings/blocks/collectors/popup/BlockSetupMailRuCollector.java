package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
/**
 * @author marchart
 */

public interface BlockSetupMailRuCollector extends MailElement {

    @Name("Поле «имя аккаунта»")
    @FindByCss("[name ='username']")
    MailElement username();

    @Name("Кнопка «Ввести пароль»")
    @FindByCss("[data-test-id='next-button']")
    MailElement nextBtn();

    @Name("Поле «Пароль»")
    @FindByCss("[name ='password']")
    MailElement password();

    @Name("Кнопка «Ввести пароль»")
    @FindByCss("[data-test-id='submit-button']")
    MailElement submitBtn();
}
