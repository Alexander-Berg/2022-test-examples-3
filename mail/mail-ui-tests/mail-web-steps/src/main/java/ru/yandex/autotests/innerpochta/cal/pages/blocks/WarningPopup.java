package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface WarningPopup extends MailElement {

    @Name("Кнопка «Да»")
    @FindByCss(".qa-Confirm-Agree")
    MailElement agreeBtn();

    @Name("Кнопка «Нет»")
    @FindByCss(".qa-Confirm-Disagree")
    MailElement cancelBtn();
}
