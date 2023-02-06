package ru.yandex.autotests.innerpochta.touch.pages.messageviewblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */
public interface AttachmentsBlock extends MailElement {

    @Name("Аттачи")
    @FindByCss(".messageAttachments>div>:nth-child(2) .attachment-wrapper")
    ElementsCollection<MailElement> attachments();

    @Name("Кнопка «Сохранить на диск»")
    @FindByCss(".js-save-all-disk")
    MailElement saveToDisk();

    @Name("Троббер во время загрузки всех аттачей на диск")
    @FindByCss(".js-save-all-disk-loading")
    MailElement trobber();
}
