package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface SignatureDropdownBlock extends MailElement {

    @Name("Кнопка «Добавить подпись» в выпадющем меню композа")
    @FindByCss(".js-signature-add-button")
    MailElement addSignatureButton();

    @Name("Список подписей в выпадающем меню композа")
    @FindByCss(".qa-Compose-SignaturesPopup-Item")
    ElementsCollection<MailElement> signaturesList();

    @Name("Блок подписей")
    @FindByCss(".qa-Compose-SignaturesPopup-List")
    MailElement signaturesBlock();

}
