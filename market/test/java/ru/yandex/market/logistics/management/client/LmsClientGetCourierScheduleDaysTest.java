package ru.yandex.market.logistics.management.client;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.schedule.CourierScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCourierDayScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientGetCourierScheduleDaysTest extends AbstractClientTest {

    @Test
    void getCourierScheduleDays() {
        searchCourierSchedule("data/controller/courier_schedule_day_entities.json");

        CourierScheduleFilter filter = CourierScheduleFilter.newBuilder()
            .partnerIds(ImmutableSet.of(10L, 14L))
            .locationIds(Set.of(213))
            .build();
        List<PartnerCourierDayScheduleResponse> scheduleDays = client.getCourierScheduleDays(filter);

        softly.assertThat(scheduleDays).isNotEmpty();
        softly.assertThat(scheduleDays).usingRecursiveFieldByFieldElementComparator().isEqualTo(
            ImmutableSet.of(
                PartnerCourierDayScheduleResponse.builder()
                    .partnerId(14L)
                    .locationId(213)
                    .calendarId(1L)
                    .schedule(ImmutableList.of(new ScheduleDayResponse(
                        1L,
                        1,
                        LocalTime.of(10, 0),
                        LocalTime.of(18, 0)
                    )))
                    .build(),
                PartnerCourierDayScheduleResponse.builder()
                    .partnerId(10L)
                    .locationId(213)
                    .calendarId(2L)
                    .schedule(ImmutableList.of(new ScheduleDayResponse(
                        6L,
                        1,
                        LocalTime.of(14, 0),
                        LocalTime.of(18, 0)
                    )))
                    .build()
            ));
    }

    @Test
    void getScheduleDaysEmpty() {
        searchCourierSchedule("data/controller/empty_entities.json");

        CourierScheduleFilter filter = CourierScheduleFilter.newBuilder()
            .partnerIds(ImmutableSet.of(10L, 14L))
            .locationIds(Set.of(213))
            .build();
        softly.assertThat(client.getCourierScheduleDays(filter)).isEmpty();
    }

    private void searchCourierSchedule(String responsePath) {
        mockServer.expect(requestTo(uri + "/externalApi/schedule/courier"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/courier_schedule_day_request.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource(responsePath)));
    }
}
