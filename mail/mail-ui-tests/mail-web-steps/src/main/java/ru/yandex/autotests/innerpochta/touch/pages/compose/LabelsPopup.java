package ru.yandex.autotests.innerpochta.touch.pages.compose;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface LabelsPopup extends MailElement {

    @Name("Список меток")
    @FindByCss(".ComposeLabelOptions-List .ComposeLabelOptions-Item")
    ElementsCollection<MailElement> labels();

    @Name("Метка «Важные»")
    @FindByCss(".ComposeLabelOptions-List .ComposeLabel_important")
    MailElement importantLabel();

    @Name("Кнопка «Редактировать метки»")
    @FindByCss(".ComposeLabelOptions-Actions")
    MailElement editLabels();

    @Name("Крестик закрытия попапов")
    @FindByCss(".ComposeBottomPopup-Close")
    MailElement closeBtn();
}
