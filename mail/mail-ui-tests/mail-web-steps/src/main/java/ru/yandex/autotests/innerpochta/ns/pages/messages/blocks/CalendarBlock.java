package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface CalendarBlock extends MailElement {

    @Name("Список доступных дат")
    @FindByCss(".Calendar-DateButton:not([class*='today']):not([class*='disabled'])")
    ElementsCollection<MailElement> calendarDates();

    @Name("Кнопка «Сохранить»")
    @FindByCss(".Button2_view_action")
    MailElement saveBtn();

    @Name("Инпут времени")
    @FindByCss("[class*='DateTimePicker__row--'] label:nth-child(2) .DateTimeField-EditableSegment")
    MailElement timeInput();
}
