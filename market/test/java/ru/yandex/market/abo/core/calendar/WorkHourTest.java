package ru.yandex.market.abo.core.calendar;

import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.calendar.db.CalendarEntry;
import ru.yandex.market.abo.core.calendar.db.CalendarService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * @author imelnikov
 */
class WorkHourTest {

    @InjectMocks
    private WorkHour workHour;

    @Mock
    private CalendarService calendarService;

    private final Date now = new Date();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void workDay() {
        doReturn(new CalendarEntry(null, false, false, "пн")).when(calendarService).get(any(LocalDateTime.class));

        assertEquals(now, workHour.add(now, 0));
        assertTrue(now.after(workHour.add(now, -10)));
        assertTrue(now.before(workHour.add(now, 10)));
    }

}
