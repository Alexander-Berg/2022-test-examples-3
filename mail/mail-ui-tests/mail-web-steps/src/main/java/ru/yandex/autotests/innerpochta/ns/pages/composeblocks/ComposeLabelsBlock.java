package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * @author mariya-murm
 */
public interface ComposeLabelsBlock extends MailElement {

    @Name("Метка Важное")
    @FindByCss(".qa-ComposeLabel_important")
    MailElement importantLabel();

    @Name("Список меток")
    @FindByCss(".ComposeLabels-Item")
    ElementsCollection<MailElement> labelsList();
}
