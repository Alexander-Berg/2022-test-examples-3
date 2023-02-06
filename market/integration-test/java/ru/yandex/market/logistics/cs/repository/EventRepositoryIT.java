package ru.yandex.market.logistics.cs.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.domain.entity.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Репозиторий для хранения событий")
public class EventRepositoryIT extends AbstractIntegrationTest {

    private static final String ORDER_ID = "42069";

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DatabaseSetup("/repository/event/events_rows.xml")
    @DisplayName("Корректный поиск последнего события")
    void testGetLastEvent() {
        List<Event> events = eventRepository.findAll();
        Optional<Event> lastEvent = eventRepository.findLast(ORDER_ID);
        events = events.stream()
            .filter(event -> event.getKey().split("_")[0].equals(ORDER_ID))
            .sorted(Comparator.comparing(Event::getEventTimestamp))
            .collect(Collectors.toList());

        Event expectedLastEvent = events.get(events.size() - 1);

        assertTrue(lastEvent.isPresent());
        assertEquals(expectedLastEvent, lastEvent.get());
    }
}
