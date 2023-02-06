package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface AttachElementsBlock extends MailElement {

    @Name("Кнопка удаления аттача")
    @FindByCss(".qa-Compose-Attachment-CloseButton")
    MailElement attachDeteteBtn();

    @Name("Имя аттача")
    @FindByCss(".clamped-text")
    MailElement attachName();

    @Name("Иконка папки")
    @FindByCss(".file-icon_dir")
    MailElement folderIcon();

    @Name("Превью аттача - картинки")
    @FindByCss(".qa-Attachment-preview")
    MailElement attachPreview();

    @Name("Превью аттача pdf")
    @FindByCss(".file-icon_pdf")
    MailElement attachPdf();

    @Name("Превью аттача eml")
    @FindByCss(".file-icon_mail")
    MailElement attachEml();
}
