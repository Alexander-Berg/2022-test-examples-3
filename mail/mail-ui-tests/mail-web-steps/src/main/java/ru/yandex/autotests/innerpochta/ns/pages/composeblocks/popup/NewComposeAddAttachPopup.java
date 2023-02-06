package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface NewComposeAddAttachPopup extends MailElement {

    @Name("Кнопка «Файлы с компьютера»")
    @FindByCss(".ComposeAttachmentSourcesMenu-Item:nth-child(1)")
    MailElement localAttachBtn();

    @Name("Кнопка «Файлы с Диска»")
    @FindByCss(".ComposeAttachmentSourcesMenu-Item:nth-child(2)")
    MailElement diskAttachBtn();

    @Name("Кнопка «Файлы из Почты»")
    @FindByCss(".ComposeAttachmentSourcesMenu-Item:nth-child(3)")
    MailElement mailAttachBtn();
}
