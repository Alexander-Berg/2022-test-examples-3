package ru.yandex.autotests.innerpochta.touch.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */
public interface ClearFolderPopup extends MailElement {

    @Name("Кнопка «Очистить»")
    @FindByCss(".confirm-popup-btn")
    MailElement confirm();

    @Name("Кнопка «Отмена»")
    @FindByCss(".confirm-popup-btn:nth-child(2)")
    MailElement cancelClear();
}
