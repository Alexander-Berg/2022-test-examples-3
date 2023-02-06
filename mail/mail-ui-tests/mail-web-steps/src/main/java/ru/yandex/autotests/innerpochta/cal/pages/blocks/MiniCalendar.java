package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface MiniCalendar extends MailElement {

    @Name("Дни следующего месяца")
    @FindByCss(".react-datepicker__day--outside-month")
    ElementsCollection<MailElement> daysOutMonth();

    @Name("Активные дни текущего месяца")
    @FindByCss(".react-datepicker__day:not(.react-datepicker__day--disabled)")
    ElementsCollection<MailElement> daysThisMonth();

    @Name("Кнопка выбора текущего месяца")
    @FindByCss(".react-datepicker__month-read-view--selected-month")
    MailElement currentMonth();

    @Name("Кнопка выбора года")
    @FindByCss(".react-datepicker__year-read-view--selected-year")
    MailElement currentYear();

    @Name("Текущий день")
    @FindByCss(".react-datepicker__day--today")
    MailElement currentDay();

    @Name("Выбранный день")
    @FindByCss(".react-datepicker__day--selected")
    MailElement selectedDay();

    @Name("Стрелочка «Предыдущий месяц»")
    @FindByCss(".react-datepicker__navigation--previous")
    MailElement previousMonth();

    @Name("Стрелочка «Следующий месяц»")
    @FindByCss(".react-datepicker__navigation--next")
    MailElement nextMonth();

    @Name("Выпадушка со списком месяцев")
    @FindByCss(".react-datepicker__month-option")
    ElementsCollection<MailElement> monthOption();
}
