package ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.Attachment;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.AttachmentInMessageList;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.11.12
 * Time: 15:20
 */
public interface MessagePageAttachmentsBlock extends MailElement {

    @Name("Аттачи")
    @FindByCss(".mail-File")
    ElementsCollection<AttachmentInMessageList> attachmentsList();

    @Name("Кнопка с колличеством аттачей в попапе")
    @FindByCss(".js-show-info")
    MailElement counterBtn();
}

