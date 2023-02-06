package ru.yandex.autotests.innerpochta.touch.pages.compose;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface Attachments extends MailElement {

    @Name("Аттачи в написании письма")
    @FindByCss(".ComposeAttachments-Item")
    ElementsCollection<MailElement> attachments();

    @Name("Крестик удаления аттача")
    @FindByCss(".compose-AttachmentItem-Close")
    ElementsCollection<MailElement> attachmentsDelete();

    @Name("Загрузившийся в композ аттач")
    @FindByCss(".compose-AttachmentItem:not(.compose-AttachmentItem_loading)")
    ElementsCollection<MailElement> uploadedAttachment();

    @Name("Троббер во время загрузки аттача")
    @FindByCss(".compose-AttachmentItem-Loader")
    MailElement trobber();

    @Name("Крестик для отмены загрузки аттача")
    @FindByCss(".compose-AttachmentItem-CancelLoading")
    MailElement cancelUpload();

    @Name("Кнопка «Удалить все»")
    @FindByCss(".ComposeAttachmentsHeader-DeleteAllButton")
    MailElement deleteAll();
}
