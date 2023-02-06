package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface ComposeThumb extends MailElement {
    @Name("Аватар свёрнутого композа")
    @FindByCss(".composeHeader-Avatar")
    MailElement avatar();

    @Name("Тема свёрнутого композа")
    @FindByCss(".composeHeader-Text")
    MailElement theme();

    @Name("Кнопка «Развернуть» свёрнутый композ")
    @FindByCss(".qa-ControlButton_button_maximize")
    MailElement expandBtn();

    @Name("Кнопка «Закрыть» свёрнутый композ")
    @FindByCss(".qa-ControlButton_button_close")
    MailElement closeBtn();
}
