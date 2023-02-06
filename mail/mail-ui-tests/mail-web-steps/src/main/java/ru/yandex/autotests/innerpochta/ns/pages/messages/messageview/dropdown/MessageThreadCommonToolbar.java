package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.dropdown;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface MessageThreadCommonToolbar extends MailElement {

    @Name("Кнопка «Переслать»")
    @FindByCss(".js-toolbar-item-forward")
    MailElement forwardButton();

    @Name("Кнопка «Распечатать»")
    @FindByCss(".js-kbd-print")
    MailElement printButton();
}
