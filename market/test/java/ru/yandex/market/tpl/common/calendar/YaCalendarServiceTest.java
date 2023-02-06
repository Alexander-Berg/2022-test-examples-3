package ru.yandex.market.tpl.common.calendar;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.common.calendar.request.CalendarOutMode;
import ru.yandex.market.tpl.common.calendar.request.CalendarTarget;
import ru.yandex.market.tpl.common.calendar.response.CalendarHolidayDto;
import ru.yandex.market.tpl.common.calendar.response.CalendarHolidayType;
import ru.yandex.market.tpl.common.calendar.tvm.YaCalendarProperties;
import ru.yandex.market.tpl.common.util.test.MockServerUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YaCalendarServiceTest {

    private static final ClientAndServer MOCK_SERVER = MockServerUtil.INSTANCE.mockServer();

    private static final LocalDate DATE_FROM = LocalDate.of(2020, 2, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2020, 2, 29);

    private static final String NULL_BODY = "";
    private static final String NULL_HOLIDAYS = "{}";

    private YaCalendarService yaCalendarService;

    @BeforeEach
    void setup() {
        MOCK_SERVER.reset();
        yaCalendarService = new YaCalendarService(
                new YaCalendarProperties(null, MockServerUtil.INSTANCE.getUrl() + "/"), new RestTemplate());
    }

    @Test
    void testGetOverrideHolidays() {
        prepareMockServer(getRealResponse());

        List<CalendarHolidayDto> holidays = yaCalendarService.getHolidays(
                CalendarTarget.RUSSIA,
                DATE_FROM, DATE_TO,
                CalendarOutMode.OVERRIDES
        );

        HttpRequest[] requests = MOCK_SERVER.retrieveRecordedRequests(HttpRequest.request());
        assertThat(requests.length).isEqualTo(1);

        HttpRequest request = requests[0];
        assertThat(request.getPath().getValue()).isEqualTo("/get-holidays");
        assertThat(request.getFirstQueryStringParameter("for")).isEqualTo("rus");
        assertThat(request.getFirstQueryStringParameter("from")).isEqualTo("2020-02-01");
        assertThat(request.getFirstQueryStringParameter("to")).isEqualTo("2020-02-29");
        assertThat(request.getFirstQueryStringParameter("outMode")).isEqualTo("overrides");

        assertThat(holidays).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(List.of(
                CalendarHolidayDto.builder()
                        .name("День защитника Отечества")
                        .date(LocalDate.of(2020, 2, 23))
                        .type(CalendarHolidayType.HOLIDAY)
                        .build(),

                CalendarHolidayDto.builder()
                        .name("Перенос выходного с 23 февраля")
                        .date(LocalDate.of(2020, 2, 24))
                        .type(CalendarHolidayType.WEEKEND)
                        .build()
        ));
    }

    @Test
    void testThrowsOnNullBody() {
        prepareMockServer(NULL_BODY);

        assertThatThrownBy(() -> yaCalendarService.getHolidays(
                CalendarTarget.RUSSIA,
                DATE_FROM, DATE_TO,
                CalendarOutMode.OVERRIDES
        ));
    }

    @Test
    void testThrowsOnNullHolidays() {
        prepareMockServer(NULL_HOLIDAYS);

        assertThatThrownBy(() -> yaCalendarService.getHolidays(
                CalendarTarget.RUSSIA,
                DATE_FROM, DATE_TO,
                CalendarOutMode.OVERRIDES
        ));
    }

    private void prepareMockServer(String body) {
        MOCK_SERVER.when(
                HttpRequest.request()
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                        .withBody(body)
        );
    }

    @SneakyThrows
    private String getRealResponse() {
        return IOUtils.toString(
                this.getClass().getResourceAsStream("/calendar/get-holidays.json"),
                StandardCharsets.UTF_8
        );
    }

}
