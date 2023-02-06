package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.rightpannel;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.Attachment;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 11.05.16.
 */
public interface AttachesList extends MailElement {

    @Name("Блок с Аттачами")
    @FindByCss(".mail-File")
    ElementsCollection<Attachment> attachments();
}
