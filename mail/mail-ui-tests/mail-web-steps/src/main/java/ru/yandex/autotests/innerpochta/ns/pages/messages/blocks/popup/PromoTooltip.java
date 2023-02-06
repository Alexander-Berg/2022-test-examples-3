package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by oleshko
 */
public interface PromoTooltip extends MailElement {

    @Name("Кнопка «Позже»")
    @FindByCss(".tooltip__buttons .button2_theme_clear")
    MailElement laterBtn();

    @Name("Кнопка «Включить»")
    @FindByCss(".tooltip__buttons .button2_theme_action")
    MailElement enableBtn();
}

