package ru.yandex.calendar.logic.event.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.event.ExternalId;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author Stepan Koltsov
 */
public class MainEventDaoTest extends AbstractConfTest {

    @Autowired
    private MainEventDao mainEventDao;

    @Test
    public void getByNormalized() {
        String externalId = "saveMainEvents@yandex.ru";
        String normalized = "saveMainEventsyandexru";

        mainEventDao.deleteMainEventByExternalId(new ExternalId(externalId));

        long id = mainEventDao.saveMainEvent(new ExternalId(externalId), MoscowTime.TZ, TestDateTimes.moscow(2011, 6, 13, 1, 1));

        MainEvent mainEvent = mainEventDao.findMainEventsByExternalId(new ExternalId(normalized)).single();
        Assert.equals(id, mainEvent.getId());
        Assert.equals(externalId, mainEvent.getExternalId());
    }

} //~
