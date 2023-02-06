package ru.yandex.market.loyalty.admin.event.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.event.CheckouterEventHandler;
import ru.yandex.market.loyalty.admin.event.EventError;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author dinyat
 * 04/09/2017
 */
public class EventErrorDaoTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private EventErrorDao eventErrorDao;

    private final EventError eventError1 = new EventError(
            13L, "Event object 1", "Error message 1", CheckouterEventHandler.UNKNOWN
    );
    private final EventError eventError2 = new EventError(
            10L, null, "Error message 2", CheckouterEventHandler.UNKNOWN
    );
    private final EventError eventError3 = new EventError(
            13L, "Event object 3", "Error message 3", CheckouterEventHandler.UNKNOWN
    );

    @Before
    public void fillEventErrorTable() {
        eventErrorDao.insertEventError(eventError1);
        eventErrorDao.insertEventError(eventError2);
        eventErrorDao.insertEventError(eventError3);
    }

    @Test
    public void testSave() {
        List<EventError> allEventErrors = eventErrorDao.getAll();

        assertEquals(3, allEventErrors.size());
        assertEqualsEventErrors(eventError1, allEventErrors.get(0));
        assertEqualsEventErrors(eventError2, allEventErrors.get(1));
        assertEqualsEventErrors(eventError3, allEventErrors.get(2));
    }

    @Test
    public void testGetByEventId() {
        List<EventError> eventErrors = eventErrorDao.getAll(13L);

        assertEquals(2, eventErrors.size());
        assertEqualsEventErrors(eventError1, eventErrors.get(0));
        assertEqualsEventErrors(eventError3, eventErrors.get(1));
    }

    private static void assertEqualsEventErrors(EventError expected, EventError actual) {
        assertEquals(expected.getSourceId(), actual.getSourceId());
        assertEquals(expected.getEvent(), actual.getEvent());
        assertEquals(expected.getErrorMessage(), actual.getErrorMessage());
        assertNotNull(actual.getCreationTime());
    }
}
