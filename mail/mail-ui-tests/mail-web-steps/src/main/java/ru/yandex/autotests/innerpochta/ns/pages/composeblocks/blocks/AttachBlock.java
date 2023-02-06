package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface AttachBlock extends MailElement {

    @Name("Количество прикреплённых аттачей")
    @FindByCss(".qa-Compose-AttachmentsHeader-Count")
    MailElement attachCount();

    @Name("Размер прикреплённых аттачей")
    @FindByCss(".qa-Compose-AttachmentsHeader-Size")
    MailElement attachSize();

    @Name("Кнопка «Удалить все»")
    @FindByCss(".qa-Compose-AttachmentsHeader-DeleteAll")
    MailElement deleteAllAttachesBtn();

    @Name("Кнопка «Свернуть-Развернуть» аттачи")
    @FindByCss(".ComposeAttachmentsHeader-Actions .ComposeAttachmentsHeader-ToggleButton .ComposeAttachmentsHeader-Button")
    MailElement collapseAttachBtn();

    @Name("Список прикреплённых аттачей")
    @FindByCss(".qa-Compose-Attachment")
    ElementsCollection<AttachElementsBlock> linkedAttach();

    @Name("Загружаемый аттач")
    @FindByCss(".qa-Compose-Attachment-loading")
    MailElement loadingAttach();
}
