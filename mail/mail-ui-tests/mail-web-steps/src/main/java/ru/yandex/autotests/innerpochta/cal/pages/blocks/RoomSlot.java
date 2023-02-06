package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface RoomSlot extends MailElement {
    @Name("Встреча в переговорке")
    @FindByCss("[class*=TimelineInterval__wrap]")
    ElementsCollection<MailElement> roomEvent();

    @Name("Выделенный слот времени")
    @FindByCss("[class*=TimelineInterval__wrap_reserved]")
    MailElement roomEventReserved();
}
