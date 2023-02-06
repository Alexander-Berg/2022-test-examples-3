package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 31.08.15.
 */

public interface MyFiltersPopup extends MailElement {

    @Name("Крестик - закрыть письмо")
    @FindByCss("._nb-popup-close")
    MailElement closeBtn();

    @Name("Кнопка “Спасибо, я знаю“")
    @FindByCss(".jane-better-popup-link")
    MailElement iKnowBtn();

    @Name("Кнопка “Изменить правило“")
    @FindByCss(".nb-with-l-right-gap")
    MailElement changeFilter();
}
