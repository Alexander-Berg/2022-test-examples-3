package ru.yandex.market.logistics.lrm.internal;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.v0.CombinatorOuterClass.PointIds;
import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteResponse;
import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteResponse.Point;
import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteResponse.Point.Service;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.service.route.model.Route;
import ru.yandex.market.logistics.lrm.service.route.model.Route.ServiceCode;
import ru.yandex.market.logistics.lrm.utils.ProtobufMessagesUtils;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DisplayName("Конвертация маршрута из protobuf в json")
class RouteConversionTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2022-07-08T09:10:11.00Z");

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Успех")
    void allFields() throws Exception {
        ReturnRouteResponse proto = ReturnRouteResponse.newBuilder()
            .addPoints(
                Point.newBuilder()
                    .setIds(PointIds.newBuilder().setPartnerId(1).setLogisticPointId(2).build())
                    .setSegmentType("BACKWARD_WAREHOUSE")
                    .setSegmentId(3)
                    .setPartnerType("SORTING_CENTER")
                    .setPartnerName("Partner name")
                    .addServices(
                        Service.newBuilder()
                            .setCode("INBOUND")
                            .setStartTime(Timestamp.newBuilder().setSeconds(NOW.getEpochSecond()).build())
                            .setEndTime(Timestamp.newBuilder().setSeconds(NOW.getEpochSecond() + 1).build())
                            .build()
                    )
                    .build()
            )
            .build();
        Route expected = Route.builder()
            .points(List.of(
                Route.Point.builder()
                    .ids(Route.PointIds.builder().partnerId(1).logisticPointId(2).build())
                    .segmentType(LogisticSegmentType.BACKWARD_WAREHOUSE)
                    .segmentId(3)
                    .partnerType(PartnerType.SORTING_CENTER)
                    .partnerName("Partner name")
                    .services(List.of(
                        Route.Service.builder()
                            .code(ServiceCode.INBOUND)
                            .startTime(NOW)
                            .endTime(NOW.plusSeconds(1))
                            .build()
                    ))
                    .build()
            ))
            .build();
        String json = ProtobufMessagesUtils.getJson(proto);
        softly.assertThat(objectMapper.readValue(json, Route.class)).isEqualTo(expected);
    }

}
