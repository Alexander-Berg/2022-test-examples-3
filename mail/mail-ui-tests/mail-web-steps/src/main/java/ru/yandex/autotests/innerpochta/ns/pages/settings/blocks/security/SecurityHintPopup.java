package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.security;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 09.04.14.
 */
public interface SecurityHintPopup extends MailElement {

    @Name("Кнопка «Закрыть»")
    @FindByCss(".b-popup__box .b-popup__body")
    MailElement closeButton();
}
