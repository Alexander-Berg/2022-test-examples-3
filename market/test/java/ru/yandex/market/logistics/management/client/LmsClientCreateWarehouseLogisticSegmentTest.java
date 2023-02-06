package ru.yandex.market.logistics.management.client;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateWarehouseSegmentRequest;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("Создание логистического сегмента с типом warehouse")
class LmsClientCreateWarehouseLogisticSegmentTest extends AbstractClientTest {
    @Test
    @DisplayName("Создание логистического сегмента с типом warehouse")
    void createWarehouse() {
        mockServer.expect(requestTo(uri + "/externalApi/logistic-segments/warehouse"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonContent("data/controller/logisticSegment/create_warehouse_request.json", false, false))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/create_warehouse_response.json"))
            );

        softly.assertThat(client.createWarehouseLogisticSegment(
            CreateWarehouseSegmentRequest.builder()
                .logisticPointId(10L)
                .returnWarehousePartnerId(2L)
                .cargoTypes(Map.of(
                    ServiceCodeName.INBOUND, Set.of(301, 302, 303),
                    ServiceCodeName.MOVEMENT, Set.of(304, 305, 306)
                ))
                .build()
        ))
            .usingRecursiveComparison()
            .isEqualTo(
                new LogisticSegmentDto()
                    .setId(100L)
                    .setType(LogisticSegmentType.WAREHOUSE)
                    .setLocationId(213)
                    .setPartnerId(1L)
                    .setLogisticsPointId(10L)
            );
    }
}
