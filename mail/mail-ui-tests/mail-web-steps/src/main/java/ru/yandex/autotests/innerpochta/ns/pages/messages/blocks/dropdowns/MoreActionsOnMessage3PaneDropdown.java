package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MoreActionsOnMessage3PaneDropdown extends MailElement {

    @Name("Кнопка «Переложить в папку» (3pane)")
    @FindByCss(".b-message-toolbar__item_small_movefolder")
    MailElement moveToFolderButton();

}


