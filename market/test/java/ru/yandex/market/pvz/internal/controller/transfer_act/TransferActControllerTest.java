package ru.yandex.market.pvz.internal.controller.transfer_act;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentRepository;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestShipmentsFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.tpl.common.transferact.client.model.TransferStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.OPTIMIZE_SHIPMENT_RECEIVE_ENABLED;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TransferActControllerTest extends BaseShallowTest {

    private final TestOrderFactory orderFactory;
    private final ShipmentRepository shipmentRepository;
    private final TestShipmentsFactory shipmentsFactory;
    private final OrderQueryService orderQueryService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @BeforeEach
    void setup() {
        configurationGlobalCommandService.setValue(OPTIMIZE_SHIPMENT_RECEIVE_ENABLED, true);
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource({"CLOSED,FINISHED,ARRIVED_TO_PICKUP_POINT",
            "CANCELLED,CANCELLED,CREATED",
            "CREATED,PENDING,CREATED"})
    public void testUpdateShipmentState(
            TransferStatus transferStatus, ShipmentStatus shipmentStatus, PvzOrderStatus orderStatus) {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        Shipment shipment = shipmentsFactory.createPendingShipment(order);

        mockMvc.perform(
                        post("/transfer-act/update/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sf(getFileContent("transfer_act/request_update_shipment.json"),
                                        shipment.getTransferId(), transferStatus)))
                .andExpect(status().is2xxSuccessful());

        Shipment finishedShipment = shipmentRepository.findByIdOrThrow(shipment.getId());
        assertThat(finishedShipment.getStatus()).isEqualTo(shipmentStatus);
        var orderParams = orderQueryService.get(order.getId());
        assertThat(orderParams.getStatus()).isEqualTo(orderStatus);
    }

    @SneakyThrows
    @Test
    public void testUpdateShipmentStateOfDispatchIgnored() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        orderFactory.readyForReturn(order.getId());

        mockMvc.perform(
                        post("/transfer-act/update/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("transfer_act/request_update_shipment_dispatch.json")))
                .andExpect(status().is2xxSuccessful());

        var orderParams = orderQueryService.get(order.getId());
        assertThat(orderParams.getStatus()).isEqualTo(PvzOrderStatus.READY_FOR_RETURN);
    }
}
