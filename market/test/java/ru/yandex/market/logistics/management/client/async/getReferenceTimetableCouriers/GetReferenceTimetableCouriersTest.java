package ru.yandex.market.logistics.management.client.async.getReferenceTimetableCouriers;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimetableCourier;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.management.client.async.AbstractClientTest;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class GetReferenceTimetableCouriersTest extends AbstractClientTest {
    @Test
    void successRequest() {
        mockServer.expect(requestTo(uri + "/lgw_callback/get_reference_timetable_couriers_success"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonRequestContent(
                "data/getReferenceTimetableCouriers/getReferenceTimetableCouriersSuccess.json"
            ))
            .andRespond(withStatus(OK));

        lmsAsyncClient.getReferenceTimetableCouriersSuccess(
            1L,
            Collections.singletonList(
                new TimetableCourier.TimetableCourierBuilder(
                    new Location.LocationBuilder("Россия", "Новосибирск", "Новосибирская область").build(),
                    Collections.singletonList(
                        new WorkTime(1, Collections.singletonList(new TimeInterval("14:30/15:30")))
                    )
                )
                    .setHolidays(Collections.singletonList(
                        new DateTime("2020-10-12")
                    ))
                    .build()
            )
        );
    }

    @Test
    void errorRequest() {
        mockServer.expect(requestTo(uri + "/lgw_callback/get_reference_timetable_couriers_error"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json("{ \"partnerId\": 1 }"))
            .andRespond(withStatus(OK));

        lmsAsyncClient.getReferenceTimetableCouriersError(1L, null, null);
    }
}
