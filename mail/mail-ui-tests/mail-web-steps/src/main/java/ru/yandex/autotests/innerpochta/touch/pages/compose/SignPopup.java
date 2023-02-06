package ru.yandex.autotests.innerpochta.touch.pages.compose;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface SignPopup extends MailElement {

    @Name("Чекбоксы")
    @FindByCss("[class*=SignatureOptionItem]")
    ElementsCollection<MailElement> signList();

    @Name("Крестик закрытия попапа")
    @FindByCss(".ComposeBottomPopup-Close")
    MailElement closeBtn();
}
