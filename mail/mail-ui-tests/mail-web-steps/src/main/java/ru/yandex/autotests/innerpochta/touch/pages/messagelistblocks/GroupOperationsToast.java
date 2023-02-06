package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface GroupOperationsToast extends MailElement {

    @Name("Кнопка «В папку»")
    @FindByCss(".ico_folder-upd")
    MailElement folder();

    @Name("Кнопка «Метки»")
    @FindByCss(".ico_tag-upd")
    MailElement label();

    @Name("Кнопка «В архив»")
    @FindByCss(".ico_archive")
    MailElement archive();

    @Name("Крестик закрытия")
    @FindByCss(".bottomSheet-closeButton")
    MailElement closeBtn();
}
