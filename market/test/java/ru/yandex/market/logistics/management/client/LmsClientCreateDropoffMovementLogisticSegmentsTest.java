package ru.yandex.market.logistics.management.client;

import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateMovementSegmentRequest;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("Создание логистического сегмента с типом movement")
class LmsClientCreateDropoffMovementLogisticSegmentsTest extends AbstractClientTest {
    @Test
    @DisplayName("Создание логистического сегмента с типом movement")
    void createMovement() {
        mockServer.expect(requestTo(uri + "/externalApi/logistic-segments/dropoff-movements"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/logisticSegment/create_movement_request.json")))
            .andRespond(withStatus(OK));

        client.createDropoffMovementLogisticSegments(
            CreateMovementSegmentRequest.builder()
                .logisticsPointFromId(1L)
                .logisticsPointToId(2L)
                .deliveryServiceId(10L)
                .tmMovementSchedule(Set.of(new ScheduleDayResponse(null, 1, LocalTime.of(15, 0), LocalTime.of(18, 0))))
                .build()
        );
    }
}
