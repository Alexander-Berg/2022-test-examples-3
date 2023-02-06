package ru.yandex.market.pvz.internal.controller.pi.shipment;

import java.time.Instant;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.core.util.File;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.tpl.common.transferact.client.api.DocumentApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ShipmentActDocumentServiceTest {

    private PickupPoint pickupPointForDispatchAct;

    private final ShipmentActDocumentService shipmentActDocumentService;

    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final TestReturnRequestFactory returnRequestFactory;
    private final TestOrderFactory testOrderFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final TestableClock clock;

    @MockBean
    private DocumentApi documentApi;

    @BeforeEach
    void setup() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        pickupPointForDispatchAct = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build());

        clock.setFixed(Instant.now(), ZoneOffset.ofHours(pickupPointForDispatchAct.getTimeOffset()));
    }

    @Test
    void getReceiveActByShipmentId() {
        int expectedReportSize = 100;
        String transferId = "1234";
        when(documentApi.documentTransferTransferIdGet(Long.parseLong(transferId)))
                .thenReturn(new ByteArrayResource(new byte[expectedReportSize]));

        PickupPoint pickupPoint = pickupPointForDispatchAct;
        var pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 123L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );

        Order order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint).build());
        Shipment shipment = testOrderFactory.createShipment(pickupPointRequestData, ShipmentType.RECEIVE,
                new ShipmentCreateItemParams(order.getExternalId(), null), transferId);

        File electronicAct = shipmentActDocumentService.getElectronicAct(shipment.getId()).orElseThrow();

        assertThat(electronicAct.getType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(electronicAct.getContent().length).isEqualTo(expectedReportSize);
    }

    @Test
    @SneakyThrows
    void getDispatchActByShipmentId() {
        int expectedReportSize = 200;
        String transferId = "1234";
        when(documentApi.documentTransferTransferIdGet(Long.parseLong(transferId)))
                .thenReturn(new ByteArrayResource(new byte[expectedReportSize]));

        PickupPoint pickupPoint = pickupPointForDispatchAct;
        var pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 123L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );

        ReturnRequestParams returnRequest = returnRequestFactory.createReceivedReturn(pickupPoint);
        Shipment shipment = testOrderFactory.createShipment(pickupPointRequestData, ShipmentType.DISPATCH,
                new ShipmentCreateItemParams(returnRequest.getReturnId(), DispatchType.RETURN), transferId);

        File electronicAct = shipmentActDocumentService.getElectronicAct(shipment.getId()).orElseThrow();

        assertThat(electronicAct.getType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(electronicAct.getContent().length).isEqualTo(expectedReportSize);
    }
}
