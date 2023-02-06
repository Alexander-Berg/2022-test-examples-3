package ru.yandex.market.delivery.transport_manager.facade.shipment;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.CreateRegisterDto;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.CreateRegisterProducer;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class ShipmentFacadeTest extends AbstractContextualTest {
    @Autowired
    private ShipmentFacade shipmentFacade;
    @Autowired
    private DeliveryClient lgwDeliveryClient;
    @Autowired
    private FulfillmentClient fulfillmentClient;
    @Autowired
    private CreateRegisterProducer createRegisterProducer;

    @BeforeEach
    void before() {
        clock.setFixed(Instant.parse("2020-07-09T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/shipment_facade/transportation/transportation_scheduled_intake.xml",
        "/repository/facade/shipment_facade/meta/transportation_legal_info.xml",
        "/repository/facade/shipment_facade/meta/transportation_partner_info_sorting_center.xml",
        "/repository/facade/shipment_facade/meta/logistics_point_metadata.xml",
        "/repository/facade/shipment_facade/meta/intake_method_meta.xml"
    })
    @ExpectedDatabase(value = "/repository/facade/shipment_facade/expected/transportations_after_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createShipmentTest_intake_sc() throws GatewayApiException {
        shipmentFacade.createShipment(1L);
        verify(fulfillmentClient, times(1)).createIntake(any(), any());
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/shipment_facade/transportation/transportation_scheduled_intake.xml",
        "/repository/facade/shipment_facade/meta/transportation_legal_info.xml",
        "/repository/facade/shipment_facade/meta/transportation_partner_info_delivery.xml",
        "/repository/facade/shipment_facade/meta/logistics_point_metadata.xml",
        "/repository/facade/shipment_facade/meta/intake_method_meta.xml"
    })
    @ExpectedDatabase(value = "/repository/facade/shipment_facade/expected/transportations_after_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createShipmentTest_intake_delivery() throws GatewayApiException {
        shipmentFacade.createShipment(1L);
        verify(lgwDeliveryClient, times(1)).createIntake(any(), any());
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/shipment_facade/transportation/transportation_scheduled_self_export.xml",
        "/repository/facade/shipment_facade/meta/transportation_legal_info.xml",
        "/repository/facade/shipment_facade/meta/transportation_partner_info_sorting_center.xml",
        "/repository/facade/shipment_facade/meta/logistics_point_metadata.xml",
        "/repository/facade/shipment_facade/meta/selfexport_method_meta.xml"
    })
    @ExpectedDatabase(value = "/repository/facade/shipment_facade/expected/transportations_after_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createShipmentTest_selfExport_sc() throws GatewayApiException {
        shipmentFacade.createShipment(1L);
        verify(fulfillmentClient, times(1)).createSelfExport(any(), any());
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/shipment_facade/transportation/transportation_scheduled_self_export.xml",
        "/repository/facade/shipment_facade/meta/transportation_legal_info.xml",
        "/repository/facade/shipment_facade/meta/transportation_partner_info_delivery.xml",
        "/repository/facade/shipment_facade/meta/logistics_point_metadata.xml",
        "/repository/facade/shipment_facade/meta/selfexport_method_meta.xml"
    })
    @ExpectedDatabase(value = "/repository/facade/shipment_facade/expected/transportations_after_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createShipmentTest_selfExport_delivery() throws GatewayApiException {
        shipmentFacade.createShipment(1L);
        verify(lgwDeliveryClient, times(1)).createSelfExport(any(), any());
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/shipment_facade/transportation/transportation_scheduled_self_export.xml",
        "/repository/facade/shipment_facade/meta/transportation_legal_info.xml",
        "/repository/facade/shipment_facade/meta/transportation_partner_info_delivery.xml",
        "/repository/facade/shipment_facade/meta/logistics_point_metadata.xml"
    })
    @ExpectedDatabase(value = "/repository/facade/shipment_facade/expected/transportation_could_not_be_matched.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createShipmentTest_selfExport_noMethod() throws GatewayApiException {
        shipmentFacade.createShipment(1L);
        verify(lgwDeliveryClient, never()).createSelfExport(any(), any());
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml",
        "/repository/register/setup/register_plan_for_unit_2.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register/after/do_not_need_to_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createRegisterTaskTest_noMethod() {
        shipmentFacade.createRegisterTask(1L);
        verify(createRegisterProducer, never()).produce(eq(1L), any(), anyBoolean());
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml",
        "/repository/facade/create_register_method_meta.xml"
    })
    void createRegisterTaskTest_createRegisterMethodExists() {
        shipmentFacade.createRegisterTask(1L);
        verifyProducingRegisterTask(54000, false);
        verifyProducingRegisterTask(61200, true);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml",
        "/repository/facade/create_register_method_meta.xml"
    })
    @DatabaseSetup(value = "/repository/transportation/unit_with_cutoff.xml", type = DatabaseOperation.UPDATE)
    void createAtCutoff() {
        shipmentFacade.createRegisterTask(1L);
        verifyProducingRegisterTask(54000, false);
        verifyProducingRegisterTask(64800, true);
    }

    private void verifyProducingRegisterTask(int durationInSeconds, boolean finalSending) {
        verify(createRegisterProducer).enqueue(eq(
            EnqueueParams.create(new CreateRegisterDto(
                1L,
                finalSending
            ))
                .withExecutionDelay(Duration.ofSeconds(durationInSeconds))
        ));
    }
}
