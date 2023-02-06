package ru.yandex.autotests.innerpochta.touch.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author sshelgunova
 */

public interface CopyYabblePopup extends MailElement {

    @Name("Кнопка «Написать письмо»")
    @FindByCss(".popup-yabble_button")
    MailElement btnWriteMail();

    @Name("Кнопка «Скопировать адрес»")
    @FindByCss(".popup-yabble_button:nth-child(2)")
    MailElement btnCopyAddress();

    @Name("Кнопка «Показать переписку»")
    @FindByCss(".popup-yabble_button:nth-child(3)")
    MailElement btnShowCrrspndnce();

    @Name("Кнопка «Создать событие»")
    @FindByCss(".popup-yabble_button:nth-child(4)")
    MailElement btnCreateEvent();
}