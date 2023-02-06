package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.abook;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface AbookViewAndExportBlock extends MailElement {

    @Name("Чекбокс «Автоматически собирать» (контакты)")
    @FindByCss("[name = 'collect_addresses']")
    MailElement autoCollectContacts();

    @Name("Кнопка импортировать контакты")
    @FindByCss(".js-abook-import")
    MailElement importBtn();

    @Name("Кнопка экспортировать контакты")
    @FindByCss(".js-abook-export")
    MailElement exportBtn();

    @Name("Чекбокс «Показывать имена получателей и отправителей вместо почтовых адресов» (баблы в композе)")
    @FindByCss("[name = 'disable_yabbles']")
    MailElement showSenderNameCheckbox();

    @Name("Кнопка «Сохранить изменения»")
    @FindByCss(".b-mail-button_setup-saver")
    MailElement saveChangeButton();
}