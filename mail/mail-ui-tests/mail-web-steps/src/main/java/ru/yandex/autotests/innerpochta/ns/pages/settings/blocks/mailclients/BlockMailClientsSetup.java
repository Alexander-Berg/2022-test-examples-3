package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.mailclients;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockMailClientsSetup extends MailElement {

    @Name("Блок «...по протоколу IMAP»")
    @FindByCss(".b-form-layout__block_settings")
    BlockIMAPSetup imap();

    @Name("Блок «...по протоколу POP3»")
    @FindByCss(".js-settings-pop3")
    BlockPOP3Setup pop3();

    @Name("Чекбокс «При получении ... по POP3 ... помечать как прочитанные»")
    @FindByCss("[name = 'pop3_makes_read']")
    MailElement markAsReadCheckBox();

    @Name("Чекбоксы на странице почтовых клиентов")
    @FindByCss("[type='checkbox']")
    ElementsCollection<MailElement> allCheckboxes();

    @Name("Кнопка «сохранить изменения»")
    @FindByCss("[type='submit']")
    MailElement saveBtn();
}