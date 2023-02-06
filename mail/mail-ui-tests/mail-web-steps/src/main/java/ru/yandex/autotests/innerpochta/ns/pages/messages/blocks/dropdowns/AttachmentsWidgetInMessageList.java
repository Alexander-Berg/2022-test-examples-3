package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.AttachmentInMessageList;

/**
 * @author mabelpines
 */
public interface AttachmentsWidgetInMessageList extends MailElement {

    @Name("Размер аттачей")
    @FindByCss(".mail-ui-Button-Extras")
    MailElement size();

    @Name("Кнопка для скачивания аттачей")
    @FindByCss(".with-extras")
    MailElement download();

    @Name("Кнопка с количеством аттачей. Открывает дропдаун с аттачами.")
    @FindByCss(".js-show-info")
    MailElement infoBtn();

    @Name("Аттач")
    @FindByCss(".mail-File")
    ElementsCollection<AttachmentInMessageList> list();
}
