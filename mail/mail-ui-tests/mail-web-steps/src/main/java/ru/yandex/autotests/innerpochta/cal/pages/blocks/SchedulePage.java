package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface SchedulePage extends MailElement {

    @Name("Кнопка добавления нового события на странице расписания")
    @FindByCss("[class*=ScheduleEmptyState__createButton]")
    MailElement addEventButtonSchedule();

    @Name("События в расписании")
    @FindByCss("[class*=ScheduleEvent__event]")
    ElementsCollection<ScheduleEvent> eventsSchedule();

    @Name("Сайдбар просмотра события")
    @FindByCss(".qa-EventFormPreview")
    ViewEventSidebar eventPreviewSchedule();

    @Name("Активное событие")
    @FindByCss("[class*=ScheduleEvent__active]")
    MailElement activeEvent();

    @Name("Иконка повтора")
    @FindByCss("[class*=ScheduleEvent__icon] svg")
    MailElement repeatIcon();

    @Name("Шапка расписания")
    @FindByCss("[class*=ScheduleHeader__container]")
    MailElement scheduleHeader();

    @Name("Превью события в расписании")
    @FindByCss("[class*=EventFormPreview__wrap]")
    ViewEventPopup eventPreview();
}
