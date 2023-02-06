package ru.yandex.market.delivery.transport_manager.service;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.event.movement.status.MovementStatusReceivedEvent;
import ru.yandex.market.delivery.transport_manager.event.movement.status.listener.CancelBookedSlotsListener;
import ru.yandex.market.delivery.transport_manager.event.movement.status.listener.CancelInterwarehouseBookedSlotListener;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.TransportationCancellationProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.NewSchemeTransportationCallbackService;
import ru.yandex.market.delivery.transport_manager.repository.mappers.MovementMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class NewSchemeTransportationCallbackServiceTest extends AbstractContextualTest {

    @Autowired
    private NewSchemeTransportationCallbackService callbackService;

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private MovementMapper movementMapper;

    @Autowired
    private TransportationCancellationProducer cancellationProducer;

    @Autowired
    private CancelBookedSlotsListener cancelBookedSlotsListener;

    @Autowired
    private CancelInterwarehouseBookedSlotListener cancelInterwarehouseBookedSlotListener;

    @Test
    @DatabaseSetup(
        value = {
            "/repository/service/movement/with_several_transportations_cancelled.xml",
            "/repository/service/movement/methods.xml"
        })
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/service/movement/dbqueue/cancel_booked_slot_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/service/movement/with_several_transportations_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelBookedTimeSlotsForInboundTransportation() {
        Transportation transportation = transportationMapper.getById(1L);
        cancelInterwarehouseBookedSlotListener.listen(new MovementStatusReceivedEvent(
            this,
            movementMapper.getMovement(transportation.getId()),
            List.of(transportation),
            MovementStatus.COURIER_FOUND,
            MovementStatus.CANCELLED_BY_PARTNER,
            LocalDateTime.of(2022, 2, 11, 12, 0),
            true
        ));
    }
}
