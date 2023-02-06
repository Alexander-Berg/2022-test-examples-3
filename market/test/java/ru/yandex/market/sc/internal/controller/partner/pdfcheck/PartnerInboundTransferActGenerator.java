package ru.yandex.market.sc.internal.controller.partner.pdfcheck;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.inbound.InboundQueryService;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.route.model.TransferActDto;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinish;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.inbound.InboundFacade.PI_ALLOWED_INBOUND_TYPES;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@SuppressWarnings("checkstyle:RegexpSingleline")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ScIntControllerTest
public class PartnerInboundTransferActGenerator {

    private final TestFactory testFactory;
    private final MockMvc mockMvc;
    private SortingCenter sortingCenter;
    private final InboundQueryService inboundQueryService;
    private final Clock clock;
    private Inbound storedInbound;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }


    void oneOrderTransferActGenerated() {
        String warehouseFromId = "warehouse-from-id";
        String orderExternalId = "test-order-external-id";

        OffsetDateTime inboundDate = OffsetDateTime.now(clock);

        Cell cell = testFactory.storedCell(sortingCenter);

        long courierUid = 300L;

        var movementCourier = testFactory.storedMovementCourier(courierUid);
        var inbound = testFactory.createInbound(inboundParams(warehouseFromId, inboundDate, movementCourier));

        storedInbound = inbound; //todo: remove

        Courier courier = testFactory.storedCourier(courierUid);
        Route route = testFactory.storedIncomingCourierDropOffRoute(inboundDate.toLocalDate(), sortingCenter, courier);


        var scOrderWithPlaces =
                testFactory.createOrder(order(sortingCenter).externalId(orderExternalId).build()).accept()
                        .getOrderWithPlaces();
        var place = scOrderWithPlaces.place(orderExternalId);
        RouteFinish routeFinish = testFactory.storedEmptyRouteFinish(
                route, testFactory.storedUser(sortingCenter, 123L));

        testFactory.storedRouteFinishPlace(
                routeFinish,
                place.getId(),
                "external_order_id",
                place.getOrderId(),
                PlaceStatus.ACCEPTED,
                SortableStatus.ARRIVED_DIRECT,
                null,
                null,
                null
        );

        TransferActDto transferAct = inboundQueryService.getTransferAct(inbound.getExternalId(), sortingCenter,
                PI_ALLOWED_INBOUND_TYPES, false);

        assertThat(transferAct).isNotNull();
        List<TransferActDto.Order> transferActOrders = transferAct.getOrders();
        assertThat(transferActOrders).isNotNull();
        assertThat(transferActOrders.size()).isEqualTo(1);


        BigDecimal magicSumFromXml = BigDecimal.valueOf(33600, 2);

        assertThat(transferActOrders.get(0)).isEqualTo(
                TransferActDto.Order.builder()
                        .externalId(orderExternalId)
                        .items(1)
                        .totalSum(magicSumFromXml)
                        .places(1L)
                        .build()
        );

    }


    @Test
    @SneakyThrows
    @Disabled(value = "use to generate pdf file")
    public void generatePdf() {
        oneOrderTransferActGenerated();

        var response = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/inbounds/" + storedInbound.getExternalId() + "/transferAct")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        Files.write(Path.of("testTransferAct.pdf"), response);
    }

    private TestFactory.CreateInboundParams inboundParams(String warehouseFromId, OffsetDateTime inboundDate,
                                                          MovementCourier courier1) {
        return TestFactory.CreateInboundParams
                .builder()
                .sortingCenter(sortingCenter)
                .movementCourier(courier1)
                .fromDate(inboundDate)
                .warehouseFromExternalId(warehouseFromId)
                .toDate(inboundDate)
                .sortingCenter(this.sortingCenter)
                .inboundType(InboundType.DS_SC)
                .build();
    }

}
