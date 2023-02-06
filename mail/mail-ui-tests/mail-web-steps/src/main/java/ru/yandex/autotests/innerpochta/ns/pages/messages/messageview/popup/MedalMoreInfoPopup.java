package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.popup;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface MedalMoreInfoPopup extends MailElement {

    @Name("Крестик")
    @FindByCss("._nb-popup-close")
    MailElement close();

    @Name("Крестик в попапе надежности отправителя")
    @FindByCss(".Button2_view_clear")
    MailElement closeMedal();

    @Name("Описание")
    @FindByCss(".b-signature-info__header")
    MailElement headerText();

    @Name("Описание в попапе надежности отправителя")
    @FindByCss(".Text_typography_body-short-s")
    ElementsCollection<MailElement> headerTextMedal();
}
