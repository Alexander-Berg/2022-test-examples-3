package ru.yandex.market.pvz.tms.command.migration;

import java.io.PrintWriter;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentRepository;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@TransactionlessEmbeddedDbTest
@Import(FillPickupPointIdInShipments.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FillPickupPointIdInShipmentsTest {

    private final FillPickupPointIdInShipments command;
    private final TestOrderFactory orderFactory;
    private final ShipmentRepository shipmentRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TestPickupPointFactory pickupPointFactory;

    @MockBean
    private Terminal terminal;

    @MockBean
    private PrintWriter printWriter;

    @Test
    @Disabled
    void setPickupPointId() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        PickupPointRequestData pickupPointAuthInfo = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());

        Order order = orderFactory.createReadyForReturnOrder(pickupPoint);
        Shipment shipment = orderFactory.createShipmentDispatch(
                pickupPointAuthInfo, order.getExternalId(), DispatchType.EXPIRED);
        long shipmentId = shipment.getId();
        assertThat(shipment.getPickupPointId()).isNotNull();

        jdbcTemplate.update("update shipment set pickup_point_id = null where id = :id",
                Map.of("id", shipmentId));
        shipment = shipmentRepository.findByIdOrThrow(shipmentId);

        assertThat(shipment.getPickupPointId()).isNull();

        when(terminal.getWriter()).thenReturn(printWriter);
        command.executeCommand(new CommandInvocation(FillPickupPointIdInShipments.COMMAND_NAME, new String[]{},
                Map.of()), terminal);

        shipment = shipmentRepository.findByIdOrThrow(shipmentId);
        assertThat(shipment.getPickupPointId()).isEqualTo(pickupPoint.getId());
    }
}
