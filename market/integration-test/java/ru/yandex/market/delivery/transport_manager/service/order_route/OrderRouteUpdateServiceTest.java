package ru.yandex.market.delivery.transport_manager.service.order_route;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.LocationDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.OrderDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.ShipmentDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.WaybillSegmentDto;
import ru.yandex.market.delivery.transport_manager.domain.enums.SegmentType;
import ru.yandex.market.delivery.transport_manager.domain.enums.ShipmentType;
import ru.yandex.market.delivery.transport_manager.service.event.lom.OrderBarcode;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup("/repository/order_route/orders.xml")
public class OrderRouteUpdateServiceTest extends AbstractContextualTest {

    @Autowired
    private OrderRouteUpdateService updateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DatabaseSetup("/repository/order_route/existing_routes_for_update.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after_order_route_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateAndUpdate() throws IOException {
        updateSequence("order_route", 2);

        OrderDto first = new OrderDto().setBarcode("barcode1").setWaybill(List.of(
            new WaybillSegmentDto()
                .setPartnerId(49691L)
                .setExternalId("70562798")
                .setSegmentType(SegmentType.FULFILLMENT)
                .setShipment(
                    new ShipmentDto()
                        .setType(null)
                        .setDate(LocalDate.of(2021, 10, 18))
                        .setLocationFrom(new LocationDto().setWarehouseId(10002464677L))
                        .setLocationTo(new LocationDto().setWarehouseId(10001781848L))
                ),
            new WaybillSegmentDto()
                .setPartnerId(98851L)
                .setExternalId("32098076")
                .setSegmentType(SegmentType.SORTING_CENTER)
                .setShipment(
                    new ShipmentDto()
                        .setType(ShipmentType.IMPORT)
                        .setDate(LocalDate.of(2021, 10, 19))
                        .setLocationFrom(new LocationDto().setWarehouseId(10002464677L))
                        .setLocationTo(new LocationDto().setWarehouseId(10001781848L))
                )
        ));

        String content = extractFileContent("converter/lom/combined-route.json");
        CombinatorRoute combinatorRoute = objectMapper.readValue(content, CombinatorRoute.class);

        OrderDto second = new OrderDto().setBarcode("barcode2").setWaybill(List.of(
            new WaybillSegmentDto().setPartnerId(172L).setExternalId("ext-1"),
            new WaybillSegmentDto().setPartnerId(60473L).setExternalId("ext-2")
        ));

        OrderRoutesForBinding routesForBinding = updateService.processOrders(
            Map.of(new OrderBarcode("barcode1"), first, new OrderBarcode("barcode2"), second),
            Set.of(new OrderBarcode("barcode2")),
            Map.of(new OrderBarcode("barcode2"), combinatorRoute)
        );

        softly.assertThat(routesForBinding.getForBinding()).containsExactlyInAnyOrder(2L, 3L);
        softly.assertThat(routesForBinding.getForRebinding()).containsExactlyInAnyOrder(1L);

    }

    @Test
    @DatabaseSetup("/repository/order_route/existing_routes_for_update.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after_origin_point_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void originPointUpdated() throws IOException {
        updateSequence("order_route", 2);

        String content = extractFileContent("converter/lom/combined-route.json");
        CombinatorRoute combinatorRoute = objectMapper.readValue(content, CombinatorRoute.class);

        combinatorRoute.getRoute().getPoints().get(0).getIds().setLogisticPointId(123);

        OrderDto second = new OrderDto().setBarcode("barcode2").setWaybill(List.of(
            new WaybillSegmentDto().setPartnerId(172L).setExternalId("ext-1")
        ));

        OrderRoutesForBinding routesForBinding = updateService.processOrders(
            Map.of(new OrderBarcode("barcode2"), second),
            Set.of(new OrderBarcode("barcode2")),
            Map.of(new OrderBarcode("barcode2"), combinatorRoute)
        );

        softly.assertThat(routesForBinding.getForBinding()).isEmpty();
        softly.assertThat(routesForBinding.getForRebinding()).containsExactlyInAnyOrder(1L);

    }
}
