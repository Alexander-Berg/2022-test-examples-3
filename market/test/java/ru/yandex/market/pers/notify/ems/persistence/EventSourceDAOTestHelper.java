package ru.yandex.market.pers.notify.ems.persistence;

import java.util.List;

import org.springframework.stereotype.Service;

import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.model.NotificationSubtype;

/**
 * @author semin-serg
 */
@Service
public class EventSourceDAOTestHelper extends EventSourceDAO {

    private static final String GET_EVENTS_BY_ADDRESS = "" +
        "select " + EVENT_SOURCE_COLUMNS + " from EVENT_SOURCE e where TYPE_ID = ?";

    public List<NotificationEvent> getEventsByType(NotificationSubtype type) {
        return getMailEvents(GET_EVENTS_BY_ADDRESS, new Object[]{type.getId()});
    }

}
