package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface AddLinkPopup extends MailElement{

    @Name("Поле ввода адреса ссылки")
    @FindByCss(".textinput__control")
    MailElement hrefInput();

    @Name("Поле ввода текста ссылки")
    @FindByCss("[name='description']")
    MailElement textInput();

    @Name("Кнопка вставки ссылки")
    @FindByCss(".linkModal__btn")
    MailElement addLinkBtn();
}
