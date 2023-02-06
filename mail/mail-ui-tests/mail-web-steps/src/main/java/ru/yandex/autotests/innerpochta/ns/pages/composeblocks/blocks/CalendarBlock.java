package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CalendarBlock extends MailElement {

    @Name("Список доступных дат")
    @FindByCss(".react-datepicker__day:not([class*='today']):not([class*='disabled'])")
    ElementsCollection<MailElement> calendarDates();

    @Name("Кнопка «Сохранить»")
    @FindByCss(".ComposeDateTimePicker-Button_save")
    MailElement saveBtn();

    @Name("Инпут времени")
    @FindByCss(".ComposeDateTimePicker-Inputs .ComposeDateTimePicker-Input_time .ComposeDateTimePicker-InputField .textinput__control")
    MailElement timeInput();
}
