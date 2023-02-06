package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface RoomEvent extends MailElement {

    @Name("Имя встречи на странице переговорок")
    @FindByCss("[class*=TimelineIntervalCard__eventName]")
    MailElement roomEventName();

    @Name("Время встречи на странице переговорок")
    @FindByCss("[class*=TimelineIntervalCard__eventTime]")
    MailElement roomEventTime();

    @Name("Участники встречи на странице переговорок")
    @FindByCss("[class*=TimelineIntervalCard__eventMembers]")
    MailElement roomEventMembers();

    @Name("Список участников встречи")
    @FindByCss("[class*=YabbleList__item]")
    ElementsCollection<MailElement> roomEventYabbles();
}
