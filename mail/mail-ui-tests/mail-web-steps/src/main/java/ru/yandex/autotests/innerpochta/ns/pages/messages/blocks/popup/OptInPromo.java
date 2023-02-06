package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by oleshko
 */
public interface OptInPromo extends MailElement {

    @Name("Крестик закрытия попапа")
    @FindByCss("[class*='PromoOptInSubscriptions__closeButton--']")
    MailElement closeBtn();

    @Name("Кнопка «Позже»")
    @FindByCss(".Button2_view_pseudo")
    MailElement laterBtn();

    @Name("Кнопка «Включить»")
    @FindByCss(".Button2_view_action")
    MailElement enableBtn();
}

