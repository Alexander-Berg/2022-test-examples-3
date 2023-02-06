package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface GroupOperationsToolbarPhone extends MailElement {

    @Name("Кнопка «Удалить»")
    @FindByCss(".ico_trash-bin")
    MailElement delete();

    @Name("Кнопка «Это спам!»")
    @FindByCss(".ico_spam-colored")
    MailElement spam();

    @Name("Кнопка «Не спам!»")
    @FindByCss(".ico_spam-colored")
    MailElement unspam();

    @Name("Кнопка «Прочитано»")
    @FindByCss(".ico_read-upd")
    MailElement read();

    @Name("Кнопка «Не прочитано»")
    @FindByCss(".ico_unread-upd")
    MailElement unread();

    @Name("Кнопка «Ещё»")
    @FindByCss(".ico_more-upd")
    MailElement more();
}
