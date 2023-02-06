package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * * @author yaroslavna
 */

public interface AddImagePopup extends MailElement {

    @Name("Кнопка «Добавить» в попапе добавления картинки")
    @FindByCss(".mail-Compose-AddImage-Popup-Action_add")
    MailElement addImageButton();

    @Name("Поле ввода в попапе добавления картинки")
    @FindByCss("[name='add-image-field']")
    MailElement inputForImage();
}
