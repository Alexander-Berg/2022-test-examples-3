package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.Attachment;

/**
 * @author mabelpines
 */
public interface AttachmentsWidget extends MailElement {

    @Name("Кнопка для скачивания аттачей")
    @FindByCss(".qa-MessageViewer-Attachments-DownloadAllBtn")
    MailElement download();

    @Name("Кнопка с количеством аттачей. Открывает дропдаун с аттачами.")
    @FindByCss(".qa-MessageViewer-Attachments-HiddenCountBtn")
    MailElement infoBtn();

    @Name("Аттач")
    @FindByCss(".qa-MessageViewer-Attachment-wrapper")
    ElementsCollection<Attachment> list();
}
