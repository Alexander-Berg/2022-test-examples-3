package ru.yandex.market.delivery.transport_manager.service.order_route;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.WaybillSegmentDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderRoute;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderRouteType;
import ru.yandex.market.delivery.transport_manager.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class CombinatorOrderRouteStrategyTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CombinatorOrderRouteStrategy strategy;

    @Test
    void testConversion() throws IOException {
        String content = extractFileContent("converter/lom/combined-route.json");
        CombinatorRoute combinatorRoute = objectMapper.readValue(content, CombinatorRoute.class);

        List<WaybillSegmentDto> segments = List.of(new WaybillSegmentDto().setPartnerId(172L).setExternalId("ext-1"));
        OrderRouteCreationData data = new OrderRouteCreationData(1L, segments, combinatorRoute);
        List<OrderRoute> routes = strategy.convert(data);

        // No specified timezone offset, fallback to default
        LocalDateTime shipmentDate1 = LocalDateTime.of(2021, 4, 21, 16, 0, 1, 0);

        // Berlin timezone offset
        LocalDateTime shipmentDate2 = LocalDateTime.of(2021, 4, 22, 15, 0, 0, 0);


        softly.assertThat(routes).isEqualTo(List.of(
            new OrderRoute()
                .setOutboundPartnerId(48027L)
                .setInboundPartnerId(172L)
                .setMovingPartnerId(48027L)
                .setInboundPointId(10000010736L)
                .setOriginInboundPointId(10000010736L)
                .setOutboundPointId(10001661260L)
                .setOriginOutboundPointId(10001661260L)
                .setIndex(0)
                .setInboundExternalId("ext-1")
                .setShipmentDate(shipmentDate1)
                .setOutboundPartnerType(PartnerType.SUPPLIER)
                .setInboundPartnerType(PartnerType.FULFILLMENT)
                .setOrderId(1L)
                .setType(OrderRouteType.COMBINATOR),
            new OrderRoute()
                .setOutboundPartnerId(172L)
                .setInboundPartnerId(60473L)
                .setMovingPartnerId(60473L)
                .setInboundPointId(null)
                .setOutboundPointId(10000010736L)
                .setOriginOutboundPointId(10000010736L)
                .setIndex(1)
                .setInboundExternalId(null)
                .setShipmentDate(shipmentDate2)
                .setOutboundPartnerType(PartnerType.FULFILLMENT)
                .setInboundPartnerType(PartnerType.DELIVERY)
                .setOrderId(1L)
                .setType(OrderRouteType.COMBINATOR)

        ));
    }
}
