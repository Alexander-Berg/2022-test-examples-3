package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface TouchScheduleViewPage extends MailElement {

    @Name("События в расписании")
    @FindByCss("[class*=TouchScheduleEvent__wrap]")
    ElementsCollection<MailElement> eventsShedule();

    @Name("Время события в прошлом")
    @FindByCss("[class*=TouchScheduleEvent__past]")
    MailElement eventTimePast();

    @Name("Время события")
    @FindByCss("[class*=TouchScheduleEvent__time]")
    MailElement eventTime();
}
