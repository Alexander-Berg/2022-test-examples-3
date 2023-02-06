package ru.yandex.market.logistics.management.client;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientGetPartnerIntakeScheduleTest extends AbstractClientTest {
    @Test
    void getPartnerIntakeSchedule() {
        mockServer.expect(requestTo(
            getBuilder(uri, "/externalApi/partners/1/intakeSchedule")
                .queryParam("date", LocalDate.of(2018, 1, 1))
                .toUriString()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_intake_schedule.json")));

        List<ScheduleDayResponse> partnerIntakeScheduleDtos =
            client.getPartnerIntakeSchedule(1L, LocalDate.of(2018, 1, 1));

        softly.assertThat(partnerIntakeScheduleDtos).isNotEmpty();
        softly.assertThat(partnerIntakeScheduleDtos).usingRecursiveFieldByFieldElementComparator().containsExactly(
            new ScheduleDayResponse(
                11L,
                1,
                LocalTime.of(0, 0),
                LocalTime.of(12, 0)
            ),
            new ScheduleDayResponse(
                10L,
                2,
                LocalTime.of(12, 0),
                LocalTime.of(16, 0)
            )
        );
    }

    @Test
    void getPartnerIntakeScheduleEmpty() {
        mockServer.expect(requestTo(
            getBuilder(uri, "/externalApi/partners/1/intakeSchedule")
                .queryParam("date", LocalDate.of(2018, 1, 1))
                .toUriString()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body("{}"));

        List<ScheduleDayResponse> partnerIntakeScheduleDtos = client.getPartnerIntakeSchedule(
            1L,
            LocalDate.of(2018, 1, 1)
        );

        softly.assertThat(partnerIntakeScheduleDtos)
            .as("Should return empty list")
            .isEmpty();
    }
}
