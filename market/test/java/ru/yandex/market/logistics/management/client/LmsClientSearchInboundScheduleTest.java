package ru.yandex.market.logistics.management.client;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

public class LmsClientSearchInboundScheduleTest extends AbstractClientTest {
    private static final String URI = "/logistic-segments/search/schedule/inbound";

    @Test
    void notEmptySchedule() {
        mockServer.expect(requestTo(uri + URI))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/combinator/response/inbound_schedule.json"))
            );

        List<ScheduleDayResponse> actualResponse = client.searchInboundSchedule(
            new LogisticSegmentInboundScheduleFilter()
        );

        softly.assertThat(actualResponse).containsExactlyInAnyOrderElementsOf(
            List.of(
                new ScheduleDayResponse(17L, 1, LocalTime.of(12, 0), LocalTime.of(18, 0), true),
                new ScheduleDayResponse(18L, 2, LocalTime.of(12, 0), LocalTime.of(17, 0), true),
                new ScheduleDayResponse(19L, 3, LocalTime.of(13, 0), LocalTime.of(17, 0), true),
                new ScheduleDayResponse(20L, 4, LocalTime.of(13, 0), LocalTime.of(16, 0), true),
                new ScheduleDayResponse(21L, 5, LocalTime.of(14, 0), LocalTime.of(15, 0), true)
            )
        );
    }

    @Test
    void emptySchedule() {
        mockServer.expect(requestTo(uri + URI))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json"))
            );

        List<ScheduleDayResponse> actualResponse = client.searchInboundSchedule(
            new LogisticSegmentInboundScheduleFilter()
        );

        softly.assertThat(actualResponse).isEmpty();
    }
}
