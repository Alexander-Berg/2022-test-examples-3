package ru.yandex.market.logistics.management.client;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.schedule.CalendarsFilter;
import ru.yandex.market.logistics.management.entity.response.schedule.CalendarHolidaysResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

public class LmsClientGetHolidaysTest extends AbstractClientTest {

    @Test
    void getHolidays() {
        mockServer.expect(requestTo(uri + "/externalApi/calendar/holidays"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/calendar/filter.json"), true))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/calendar/holidays.json")));

        List<CalendarHolidaysResponse> holidays = client.getHolidays(
            CalendarsFilter.builder()
                .calendarIds(ImmutableList.of(2L, 1L))
                .dateFrom(LocalDate.of(2019, 1, 1))
                .dateTo(LocalDate.of(2020, 1, 1))
                .build()
        );

        softly.assertThat(holidays).hasSize(2);
        softly.assertThat(holidays).extracting(CalendarHolidaysResponse::getId).containsExactly(2L, 1L);
        softly.assertThat(holidays).flatExtracting(CalendarHolidaysResponse::getDays).containsExactly(
            LocalDate.of(2019, 3, 8),
            LocalDate.of(2019, 5, 9),
            LocalDate.of(2019, 7, 4),
            LocalDate.of(2019, 11, 28)
        );
    }

    @Test
    void getHolidaysEmptyResult() {
        mockServer.expect(requestTo(uri + "/externalApi/calendar/holidays"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/calendar/filter.json"), true))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json")));

        List<CalendarHolidaysResponse> holidays = client.getHolidays(
            CalendarsFilter.builder()
                .calendarIds(ImmutableList.of(2L, 1L))
                .dateFrom(LocalDate.of(2019, 1, 1))
                .dateTo(LocalDate.of(2020, 1, 1))
                .build()
        );

        softly.assertThat(holidays).isEmpty();
    }

}
