package ru.yandex.market.logistics.management.client;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.schedule.ScheduleDayFilter;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientGetScheduleDayTest extends AbstractClientTest {
    @Test
    void getScheduleDay() {
        mockServer.expect(requestTo(uri + "/externalApi/schedule/11"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/schedule_day.json")));

        Optional<ScheduleDayResponse> partnerIntakeScheduleDto = client.getScheduleDay(11L);

        softly.assertThat(partnerIntakeScheduleDto).isPresent();
        partnerIntakeScheduleDto.ifPresent(v ->
            softly.assertThat(v).isEqualToComparingFieldByField(
                new ScheduleDayResponse(
                    11L,
                    3,
                    LocalTime.of(0, 0),
                    LocalTime.of(12, 0)
                )
            ));
    }

    @Test
    void getScheduleDayEmpty() {
        mockServer.expect(requestTo(uri + "/externalApi/schedule/19"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body("null"));

        softly.assertThat(client.getScheduleDay(19L)).isNotPresent();
    }

    @Test
    void getScheduleDays() {
        mockServer.expect(requestTo(uri + "/externalApi/schedule"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/schedule_day_request.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/schedule_day_entities.json")));

        List<ScheduleDayResponse> scheduleDays = client.getScheduleDays(new ScheduleDayFilter(
            ImmutableSet.of(11L, 2L, 3L)
        ));

        softly.assertThat(scheduleDays).isNotEmpty();
        softly.assertThat(scheduleDays).usingRecursiveFieldByFieldElementComparator().containsExactly(
            new ScheduleDayResponse(
                11L,
                3,
                LocalTime.of(0, 0),
                LocalTime.of(12, 0)
            )
        );
    }

    @Test
    void getScheduleDaysEmpty() {
        mockServer.expect(requestTo(uri + "/externalApi/schedule"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/schedule_day_request.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json")));

        softly.assertThat(client.getScheduleDays(new ScheduleDayFilter(ImmutableSet.of(11L, 2L, 3L)))).isEmpty();
    }
}
