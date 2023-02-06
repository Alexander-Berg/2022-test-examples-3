package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface Toolbar3PanelBlock extends MailElement {

    @Name("Кнопка удалить письмо (3pane)")
    @FindByCss(".b-message-toolbar__item_delete")
    MailElement deleteButton();

    @Name("Кнопка переслать письмо (3pane)")
    @FindByCss(".b-message-toolbar__item_forward")
    MailElement forwardButton();

    @Name("Кнопка дополнительных действий (3pane)")
    @FindByCss(".b-message-toolbar__item_more")
    MailElement moreActionsButton();
}



