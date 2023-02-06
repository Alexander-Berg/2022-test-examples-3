package ru.yandex.market.logistics.management.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.calendar.LocationCalendarsSyncService;

class CalendarControllerTest extends AbstractContextualTest {

    @Autowired
    private CalendarController calendarController;

    @Autowired
    private LocationCalendarsSyncService locationCalendarsSyncService;

    @Test
    void testImportLocations() throws Exception {
        Mockito.doNothing().when(locationCalendarsSyncService).syncLocationCalendars();

        mockMvc.perform(MockMvcRequestBuilders.patch("/calendar/locations/import"))
            .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(calendarController).importLocationCalendars();
        Mockito.verify(locationCalendarsSyncService).syncLocationCalendars();
    }
}
