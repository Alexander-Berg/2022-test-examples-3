package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.mailclients;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockIMAPSetup extends MailElement {

    @Name("Чекбокс «С сервера imap.yandex.ru по протоколу IMAP»")
    @FindByCss(".js-enable-imap ._nb-checkbox-input")
    MailElement enableImapCheckbox();

    @Name("Блок «Мы рекомендуем использовать протокол IMAP»")
    @FindByCss(".b-box_imap")
    ImapAdvantagesBlock imapAdvantages();

    @Name("Чекбокс «Отключить автоматическое удаление писем, помеченных в IMAP как удаленные»")
    @FindByCss(".js-disable-imap-autoexpunge")
    MailElement disableAutoexpungeCheckbox();

    @Name("Чекбокс «Портальный пароль»")
    @FindByCss(".js-enable-imap-auth-plain ._nb-checkbox-input")
    MailElement portalPassword();
}