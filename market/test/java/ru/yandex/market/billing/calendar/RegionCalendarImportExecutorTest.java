package ru.yandex.market.billing.calendar;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.calendar.api.model.GetHolidaysOutMode;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link RegionCalendarImportExecutor}
 */
@DbUnitDataSet(before = "RegionCalendarImportExecutorTest.before.csv")
class RegionCalendarImportExecutorTest extends FunctionalTest {

    @Autowired
    private MockWebServer yandexCalendarApiMockWebServer;

    @Autowired
    private RegionCalendarImportExecutor executor;

    @Autowired
    private Clock clock;

    @Autowired
    @Qualifier("mbiTvm")
    private Tvm2 mbiTvm;

    @Test
    @DbUnitDataSet(
            before = "RegionCalendarImportExecutorTest.testImportFromNewCalendarApi.before.csv",
            after = "RegionCalendarImportExecutorTest.testImportFromNewCalendarApi.after.csv"
    )
    void testImportFromNewCalendarApi() throws Exception {
        String tvmTicketId = "some-ticket-id";
        when(mbiTvm.getServiceTicket(anyInt())).thenReturn(Option.of(tvmTicketId));
        when(clock.instant())
                .thenReturn(LocalDate.of(2021, 2, 3)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                );

        String yandexCalendarApiResponse = IOUtils.toString(
                getClass().getResourceAsStream("RegionCalendarImportExecutorTest.calendarApi.response.json"), UTF_8);
        yandexCalendarApiMockWebServer.enqueue(new MockResponse().setBody(yandexCalendarApiResponse));

        executor.doJobLocked(null);

        RecordedRequest recordedRequest = yandexCalendarApiMockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("X-Ya-Service-Ticket")).isEqualTo(tvmTicketId);
        HttpUrl requestedUrl = recordedRequest.getRequestUrl();
        assertThat(requestedUrl.pathSegments()).containsExactly("get-holidays");
        assertThat(requestedUrl.queryParameter("for")).isEqualTo("225");
        assertThat(requestedUrl.queryParameter("from")).isEqualTo("2021-02-01");
        assertThat(requestedUrl.queryParameter("to")).isEqualTo("2021-02-08");
        assertThat(requestedUrl.queryParameter("outMode")).isEqualTo(GetHolidaysOutMode.OVERRIDES.getId());
    }
}
