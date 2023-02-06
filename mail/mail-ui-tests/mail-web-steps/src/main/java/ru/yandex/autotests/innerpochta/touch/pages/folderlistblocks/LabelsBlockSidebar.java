package ru.yandex.autotests.innerpochta.touch.pages.folderlistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface LabelsBlockSidebar extends MailElement {

    @Name("Сервисы")
    @FindByCss(".leftPanelLabel")
    ElementsCollection<MailElement> labels();
}
