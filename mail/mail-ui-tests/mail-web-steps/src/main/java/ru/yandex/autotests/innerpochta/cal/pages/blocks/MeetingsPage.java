package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface MeetingsPage extends MailElement {
    @Name("Названия переговорок")
    @FindByCss("[class*=SpaceshipResources__resourceNameWrap]")
    ElementsCollection<MailElement> roomName();

    @Name("Строка слотов переговорки")
    @FindByCss("[class*=SpaceshipResources__resourceIntervals]")
    ElementsCollection<RoomSlot> roomSlots();

    @Name("Строка слотов для бронирования переговорки")
    @FindByCss("[class*=SpaceshipReservableZone__wrap]")
    ElementsCollection<RoomSlot> roomBookSlots();

    @Name("Стрелки смены даты")
    @FindByCss("[class*=SpaceshipHeader__navArrow]")
    ElementsCollection<MailElement> arrows();
}
