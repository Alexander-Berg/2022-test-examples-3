package ru.yandex.calendar.logic.event;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class EventDbManagerTest extends AbstractConfTest {
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventLayerDao eventLayerDao;


    @Test
    public void deleteEventById() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-16101").getUid();

        long eventId = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "deleteEventById - user event").getId();
        eventDbManager.deleteEventById(eventId);

        try {
            eventDao.findEventById(eventId);
            Assert.fail("Expected exception when looking for deleted event " + eventId);
        } catch (EmptyResultDataAccessException e) {
            // ok
        }
        Assert.A.isEmpty(eventLayerDao.findEventLayersByEventId(eventId));

        // ??? other event types
    }
}
