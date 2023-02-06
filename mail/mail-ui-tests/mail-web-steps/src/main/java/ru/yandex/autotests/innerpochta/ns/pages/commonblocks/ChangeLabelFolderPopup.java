package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by eremin-n-s
 */
public interface ChangeLabelFolderPopup extends MailElement{

    @Name("Инпут названия")
    @FindByCss(".js-input-name")
    MailElement nameInput();

    @Name("Цвета меток")
    @FindByCss(".js-label-sample")
    ElementsCollection <MailElement> colors();

    @Name("Кнопки подтверждения")
    @FindByCss(".nb-button")
    ElementsCollection <MailElement> confirmButtons();
}
