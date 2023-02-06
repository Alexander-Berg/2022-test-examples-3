package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author crafty
 */
public interface EventDecisionPopup extends MailElement {

    @Name("Кнопка «Отмена»")
    @FindByCss(".qa-EventDecision-Cancel")
    MailElement cancelPopupBtn();

    @Name("Кнопка «Не пойду»")
    @FindByCss(".qa-EventDecision-RejectThisEvent")
    MailElement rejectThisEventBtn();

    @Name("Кнопка «Отклонить все»")
    @FindByCss(".qa-EventDecision-RejectAllEvents")
    MailElement rejectAllEventsBtn();

    @Name("Инпут для комментария")
    @FindByCss(".qa-EventDecision-Popup textarea")
    MailElement commentInput();
}
