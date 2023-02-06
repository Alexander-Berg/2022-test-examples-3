package ru.yandex.calendar.logic.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.calendar.logic.log.dao.EventsLogDao;
import ru.yandex.calendar.test.generic.AbstractConfTest;

import java.sql.SQLException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class EventsLogDaoTest extends AbstractConfTest {
    @Autowired
    private EventsLogDao eventsLogDao;

    @Test
    public void shouldLogAndAfterCollectGarbage() throws SQLException, JsonProcessingException {
        eventsLogDao.logUnsafe(EventMocks.externalIdChangeEvent(), EventMocks.webActionInfo(), Duration.ZERO);
        eventsLogDao.logUnsafe(EventMocks.mailLogEvent(), EventMocks.mailActionInfo(), Duration.ZERO);
        eventsLogDao.logUnsafe(EventMocks.ewsCallEvent(), EventMocks.exchangeActionInfo(), Duration.ZERO);
        eventsLogDao.logUnsafe(EventMocks.eventChangeEvent(), EventMocks.caldavActionInfo(), Duration.ZERO);
        eventsLogDao.logUnsafe(EventMocks.mailerHandlingEvent(), EventMocks.mailActionInfo(), Duration.ZERO);
        eventsLogDao.logUnsafe(EventMocks.notificationEvent(), EventMocks.xivaActionInfo(), Duration.ZERO);
        eventsLogDao.logUnsafe(EventMocks.todoMailEvent(), EventMocks.mailActionInfo(), Duration.ZERO);
        assertThat(eventsLogDao.gc()).isGreaterThanOrEqualTo(7);
    }
}
