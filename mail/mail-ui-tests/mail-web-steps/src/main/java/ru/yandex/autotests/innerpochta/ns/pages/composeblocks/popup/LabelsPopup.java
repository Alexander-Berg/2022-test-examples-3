package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.popup;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * @author mariya-murm
 */

public interface LabelsPopup extends MailElement {

    @Name("Список меток")
    @FindByCss(".ComposeLabelOptions-List .ComposeLabelOptions-Item")
    ElementsCollection<MailElement> labels();

    @Name("Метка «Важные»")
    @FindByCss(".ComposeLabelOptions-List .qa-ComposeLabel_important")
    MailElement importantLabel();

    @Name("Кнопка «Редактировать метки»")
    @FindByCss(".ComposeLabelOptions-Actions")
    MailElement editLabels();
}
