package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface DeleteEventPopup extends MailElement {

    @Name("Кнопка «Отмена»")
    @FindByCss(".qa-EventFormDelete-ModalButtonCancel")
    MailElement cancelBtn();

    @Name("Кнопка «Удалить только это»")
    @FindByCss(".qa-EventFormDelete-ModalButtonDeleteSingle")
    MailElement removeOneBtn();

    @Name("Кнопка «Удалить серию»")
    @FindByCss(".qa-EventFormDelete-ModalButtonDeleteMultiple")
    MailElement removeAllBtn();
}
