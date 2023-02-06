package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface GroupOperationsToolbarHeader extends MailElement {

    @Name("Кнопка «Выделить все»")
    @FindByCss(".topBar-item_selectAll")
    MailElement selectAll();

    @Name("Кнопка «Отмена»")
    @FindByCss(".topBar-item_cancelSelection")
    MailElement cancel();

    @Name("Кнопка «Снять выделение»")
    @FindByCss(".topBar-item_active")
    MailElement clearAll();
}
