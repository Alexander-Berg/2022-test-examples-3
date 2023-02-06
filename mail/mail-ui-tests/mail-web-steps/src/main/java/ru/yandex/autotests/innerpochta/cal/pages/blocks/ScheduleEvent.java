package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface ScheduleEvent extends MailElement {

    @Name("Название события")
    @FindByCss("[class*=ScheduleEvent__eventName]")
    MailElement eventName();

    @Name("Время события")
    @FindByCss("[class*=ScheduleEvent__additionalLine]")
    MailElement eventTime();
}
