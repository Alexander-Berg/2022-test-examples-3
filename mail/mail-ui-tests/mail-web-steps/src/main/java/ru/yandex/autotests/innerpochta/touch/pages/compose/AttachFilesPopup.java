package ru.yandex.autotests.innerpochta.touch.pages.compose;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface AttachFilesPopup extends MailElement {

    @Name("Кнопка «Из почты»")
    @FindByCss(".ComposeAttachmentSourcesMenu-Item:nth-child(3)")
    MailElement fromMail();

    @Name("Кнопка «С диска»")
    @FindByCss(".ComposeAttachmentSourcesMenu-Item:nth-child(2)")
    MailElement fromDisk();
}
