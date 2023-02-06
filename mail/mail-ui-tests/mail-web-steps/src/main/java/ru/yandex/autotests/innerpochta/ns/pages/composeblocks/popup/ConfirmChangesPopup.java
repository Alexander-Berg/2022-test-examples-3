package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 20.05.15.
 */
public interface ConfirmChangesPopup extends MailElement {

    @Name("Кнопка “Сохранить и перейти“")
    @FindByCss("button[data-action='save']")
    MailElement saveChangesBtn();
}
