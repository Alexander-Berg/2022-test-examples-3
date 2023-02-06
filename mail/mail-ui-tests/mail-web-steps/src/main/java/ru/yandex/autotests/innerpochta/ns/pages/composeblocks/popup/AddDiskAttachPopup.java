package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface AddDiskAttachPopup extends MailElement {

    @Name("Список аттачей")
    @FindByCss(".browseDisk-Listing-Item")
    ElementsCollection<MailElement> attachList();

    @Name("Кнопка «Прикрепить»")
    @FindByCss(".DiskResourcesFooterDesktop-Button_action")
    MailElement addAttachBtn();

    @Name("Кнопка «Прикрепить»")
    @FindByCss(".DiskResourcesFooterDesktop-Button_cancel")
    MailElement closePopupBtn();
}
