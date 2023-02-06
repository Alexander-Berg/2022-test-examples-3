package ru.yandex.market.logistics.management.service.calendar;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.configuration.ExternalClientTestConfiguration;
import ru.yandex.market.logistics.management.domain.dto.CalendarHolidayDto;
import ru.yandex.market.logistics.management.domain.dto.Locations;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.request.trace.Module;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YaCalendarServiceTest extends AbstractTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String YA_CALENDAR_HOST = "http://test.yandex-team.ru/";

    private final RestTemplate restTemplate = ExternalClientTestConfiguration.restTemplate(Module.YANDEX_CALENDAR);
    private final TvmTicketProvider ticketProvider = Mockito.mock(TvmTicketProvider.class);
    private final YaCalendarService yaCalendarService =
        new YaCalendarService(YA_CALENDAR_HOST, restTemplate, ticketProvider);
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        Mockito.reset(ticketProvider);
        Mockito.doReturn("ticket").when(ticketProvider).provideServiceTicket();
    }

    @Test
    void testHolidaysSuccess() {
        int locationId = Locations.RUSSIA;
        LocalDate from = LocalDate.of(2011, 3, 3);
        LocalDate to = LocalDate.of(2011, 3, 12);
        Resource holidaysJson = new ClassPathResource("data/calendar/holidays_success.json");

        mockServer.expect(requestTo(buildHolidaysUrl(locationId, from, to)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(holidaysJson, MediaType.APPLICATION_JSON_UTF8));

        List<CalendarHolidayDto> calendarHolidays = yaCalendarService
            .fetchCalendarHolidays(locationId, from, to);

        mockServer.verify();

        softly.assertThat(calendarHolidays)
            .extracting(CalendarHolidayDto::getDate)
            .containsExactlyInAnyOrder(
                LocalDate.of(2011, 3, 6),
                LocalDate.of(2011, 3, 7),
                LocalDate.of(2011, 3, 8)
            );
    }

    @Test
    void testHolidaysNull() {
        int locationId = Locations.RUSSIA;
        LocalDate from = LocalDate.of(2011, 3, 3);
        LocalDate to = LocalDate.of(2011, 3, 12);

        mockServer.expect(requestTo(buildHolidaysUrl(locationId, from, to)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("null", MediaType.APPLICATION_JSON_UTF8));

        softly.assertThatThrownBy(() -> yaCalendarService.fetchCalendarHolidays(locationId, from, to))
            .isNotNull();

        mockServer.verify();
    }

    @Test
    void testHolidaysServerError() {
        int locationId = 1;
        LocalDate from = LocalDate.of(2011, 3, 3);
        LocalDate to = LocalDate.of(2011, 3, 12);

        mockServer.expect(anything())
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError());

        softly.assertThatThrownBy(() -> yaCalendarService.fetchCalendarHolidays(locationId, from, to))
            .isInstanceOf(HttpServerErrorException.class);

        mockServer.verify();
    }

    @Test
    void testHolidaysClientError() {
        int locationId = 1;
        LocalDate from = LocalDate.of(2011, 3, 3);
        LocalDate to = LocalDate.of(2011, 3, 12);

        mockServer.expect(anything())
            .andExpect(method(HttpMethod.GET))
            .andRespond(withBadRequest());

        softly.assertThatThrownBy(() -> yaCalendarService.fetchCalendarHolidays(locationId, from, to))
            .isInstanceOf(HttpClientErrorException.class);

        mockServer.verify();
    }

    private String buildHolidaysUrl(int locationId, LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromHttpUrl(YA_CALENDAR_HOST)
            .path("get-holidays")
            .queryParam("from", from.format(FORMATTER))
            .queryParam("to", to.format(FORMATTER))
            .queryParam("for", locationId)
            .queryParam("outMode", "overrides")
            .toUriString();
    }
}
