package ru.yandex.market.delivery.transport_manager.service;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.CancellationService;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.xdoc.CancelXDocOutboundPlanProducer;
import ru.yandex.market.delivery.transport_manager.service.transportation_unit.TransportationUnitService;

@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class CancellationServiceTest extends AbstractContextualTest {
    @Autowired
    private CancellationService cancellationService;

    @Autowired
    private TransportationService transportationService;

    @Autowired
    private MovementService movementService;

    @Autowired
    private TransportationStatusService statusService;

    @Autowired
    private CancelXDocOutboundPlanProducer cancelXDocOutboundPlanProducer;

    @Autowired
    private TransportationUnitService transportationUnitService;

    @Autowired
    private TmPropertyService propertyService;

    @BeforeEach
    void init() {
        Mockito.doReturn(true).when(propertyService).getBoolean(Mockito.any());
    }

    @Test
    @DatabaseSetup("/repository/transportation/cancelled_transportation.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/cancelled_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void alreadyCancelled() {
        cancellationService.cancelTransportation(
            transportationService.getById(2L),
            TransportationSubstatus.MANUAL_CANCELLATION
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation/for_cancellation.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_cancellation_movement_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelNotSent() {
        cancellationService.cancelTransportation(
            transportationService.getById(1L),
            TransportationSubstatus.MANUAL_CANCELLATION
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation/for_cancellation.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_cancellation_movement_sent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/cancel_movement_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void cancelMovement() {
        movementService.setExternalIdAndStatus(101L, MovementStatus.PARTNER_CREATED, "10");
        cancellationService.cancelTransportation(
            transportationService.getById(1L),
            TransportationSubstatus.MANUAL_CANCELLATION
        );
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/service/movement/with_several_transportations_cancelled.xml",
            "/repository/service/movement/methods.xml"
        })
    @ExpectedDatabase(
        value = "/repository/service/movement/dbqueue/cancel_movement_interwarehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/service/movement/after/transportation_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelMovementWithSlotsTest() {
        clock.setFixed(Instant.parse("2021-08-30T11:15:25.00Z"), ZoneId.of("Europe/Moscow"));
        statusService.setTransportationStatus(1L, TransportationStatus.SCHEDULED);
        cancellationService.cancelTransportation(
            transportationService.getById(1L),
            TransportationSubstatus.OUTBOUND_CANCELLED
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation/for_cancellation.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_cancellation_movement_sent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/cancel_movement_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void checkUnfreezePalletsInvocation() {
        movementService.setExternalIdAndStatus(101L, MovementStatus.PARTNER_CREATED, "10");
        cancellationService.cancelTransportation(
            transportationService.getById(1L),
            TransportationSubstatus.MANUAL_CANCELLATION
        );
        Mockito.verify(cancelXDocOutboundPlanProducer).enqueue(1L, false);
    }

    @Test
    @DisplayName("Отменять перемещение с forceCancel = true, даже если тип перемещения этого не позволяет")
    @DatabaseSetup("/repository/transportation/for_cancellation.xml")
    @DatabaseSetup(value = "/repository/transportation/update_type.xml", type = DatabaseOperation.UPDATE)
    void checkForceCancel() {
        cancellationService.cancelTransportation(
            transportationService.getById(1L),
            null,
            true
        );
        Transportation transportation = transportationService.getById(1L);
        Assertions.assertEquals(transportation.getStatus(), TransportationStatus.CANCELLED);
        Assertions.assertEquals(transportation.getOutboundUnit().getStatus(), TransportationUnitStatus.CANCELLED);
        Assertions.assertEquals(transportation.getMovement().getStatus(), MovementStatus.CANCELLED);
        Assertions.assertEquals(transportation.getInboundUnit().getStatus(), TransportationUnitStatus.CANCELLED);
    }

    @Test
    @DatabaseSetup("/repository/transportation/for_cancellation.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/cancel_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void checkCancelUnits() {
        transportationUnitService.setStatus(101, TransportationUnitStatus.ACCEPTED);
        transportationUnitService.setStatus(102, TransportationUnitStatus.ARRIVED);

        cancellationService.cancelTransportation(
            transportationService.getById(1L),
            null,
            false
        );
    }
}
