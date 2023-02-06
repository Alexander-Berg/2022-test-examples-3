package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface AddImageLinkPopup extends MailElement{

    @Name("Поле ввода адреса ссылки")
    @FindByCss(".textinput__control")
    MailElement hrefInput();

    @Name("Кнопка вставки ссылки")
    @FindByCss(".CKEnterUrlForm-Button")
    MailElement addLinkBtn();
}
