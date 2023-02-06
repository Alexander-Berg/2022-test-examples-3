package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface ConfirmPopup extends MailElement {

    @Name("Крестик нотификации")
    @FindByCss("[class*=TouchConfirm__text]")
    MailElement confirmSaveText();

    @Name("Кнопка «Отмена»")
    @FindByCss(".button2_theme_normal")
    MailElement cancelBtn();

    @Name("Кнопка «До...»")
    @FindByCss(".button2_theme_action")
    MailElement addBtn();
}
