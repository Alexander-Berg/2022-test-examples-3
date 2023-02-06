package ru.yandex.autotests.innerpochta.touch.pages.folderlistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface FolderBlock extends MailElement {

    @Name("Название")
    @FindByCss(".leftPanelItem-text")
    MailElement name();

    @Name("Иконка")
    @FindByCss(".leftPanelItem-icon")
    MailElement icon();

    @Name("Каунтер непрочитанных")
    @FindByCss(".leftPanelItem-counter")
    MailElement counter();
}
