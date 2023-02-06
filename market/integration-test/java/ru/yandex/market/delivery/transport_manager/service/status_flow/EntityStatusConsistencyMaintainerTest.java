package ru.yandex.market.delivery.transport_manager.service.status_flow;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
@DatabaseSetup("/repository/service/status_flow/consistency/before.xml")
public class EntityStatusConsistencyMaintainerTest extends AbstractContextualTest {

    @Autowired
    private EntityStatusConsistencyMaintainer statusConsistencyMaintainer;

    @Test
    @DisplayName("Обновить success статус у планового реестра")
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/after_register.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    public void testUpdateRegister() {
        statusConsistencyMaintainer.updateForTransportationUnit(
            unit(1L, TransportationUnitType.OUTBOUND, TransportationUnitStatus.PLAN_REGISTRY_SENT),
            TransportationUnitStatus.PLAN_REGISTRY_CREATED,
            1L
        );
    }

    @Test
    @DisplayName("Обновить error статус у планового реестра")
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/after_register_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/health/dbqueue/empty.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    public void testUpdateRegisterError() {
        statusConsistencyMaintainer.updateForTransportationUnit(
            unit(1L, TransportationUnitType.OUTBOUND, TransportationUnitStatus.PLAN_REGISTRY_SENT),
            TransportationUnitStatus.ERROR,
            1L
        );
    }

    @Test
    @DisplayName("Обновить error статус у планового реестра с запросом ошибок из FFWF")
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/after_register_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    public void testUpdateRegisterErrorFfwf() {
        statusConsistencyMaintainer.updateForTransportationUnit(
            unit(1L, TransportationUnitType.OUTBOUND, TransportationUnitStatus.PLAN_REGISTRY_SENT).setRequestId(1L),
            TransportationUnitStatus.ERROR,
            1L
        );
    }

    @Test
    @DisplayName("Обновить статус у перемещения и задачи на перемещение")
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/after_transportation_and_task.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    public void testUpdateTransportationAndTask() {
        statusConsistencyMaintainer.updateForTransportationUnit(
            unit(2L, TransportationUnitType.INBOUND, TransportationUnitStatus.IN_PROGRESS),
            TransportationUnitStatus.PROCESSED,
            1L
        );
    }

    @Test
    @DatabaseSetup(
        value = "/repository/service/status_flow/consistency/one_more_transportation.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Обновить статус у перемещения без задачи (потому что у нее есть еще перемещения)")
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/after_transportation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    public void testUpdateTransportationWithoutTask() {
        statusConsistencyMaintainer.updateForTransportationUnit(
            unit(2L, TransportationUnitType.INBOUND, TransportationUnitStatus.IN_PROGRESS),
            TransportationUnitStatus.PROCESSED,
            1L
        );
    }

    @Test
    @DatabaseSetup(
        value = "/repository/service/status_flow/consistency/before_processed.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/after_processed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @DisplayName("Не обновлять статус у юнита, если он уже PROCESSED")
    public void testNoStatusUpdatesAfterProcessedStatus() {
        statusConsistencyMaintainer.updateForTransportationUnit(
            unit(20L, TransportationUnitType.INBOUND, TransportationUnitStatus.PROCESSED),
            TransportationUnitStatus.PLAN_REGISTRY_SENT,
            10L
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/movement_processed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @DisplayName("Обновить статус мувмента")
    public void testUpdateMovementStatusForMovement() {
        statusConsistencyMaintainer.updateForMovement(
                movement(2L, MovementStatus.NEW),
                MovementStatus.PARTNER_CREATED,
                List.of(),
                false
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/after_confirmed_movement.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @DisplayName("Обновить статус перемещения на SCHEDULED, после CONFIRMED статуса мувмента")
    public void testCarSelectingStatusUpdate() {
        statusConsistencyMaintainer.updateForMovement(
            movement(1L, MovementStatus.NEW),
            MovementStatus.CONFIRMED,
            List.of(transportation(1L, TransportationType.INTERWAREHOUSE)),
            false
        );
    }

    @Test
    @DatabaseSetup(
        value = "/repository/service/status_flow/consistency/before_orders_operation.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/after_orders_operation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @DisplayName("Обновить статус перемещения на MOVER_FOUND, после CONFIRMED статуса мувмента")
    public void testMoverFoundStatusUpdate() {
        statusConsistencyMaintainer.updateForMovement(
            movement(6L, MovementStatus.NEW),
            MovementStatus.CONFIRMED,
            List.of(transportation(7L, TransportationType.ORDERS_OPERATION)),
            false
        );
    }

    private TransportationUnit unit(long id, TransportationUnitType type, TransportationUnitStatus oldStatus) {
        return new TransportationUnit()
            .setId(id)
            .setType(type)
            .setStatus(oldStatus);
    }

    private Movement movement(long id, MovementStatus status) {
        return new Movement()
                .setId(id)
                .setStatus(status);
    }

    private Transportation transportation(long id, TransportationType type) {
        return new Transportation()
                .setId(id)
                .setTransportationType(type)
                .setScheme(TransportationScheme.NEW);
    }
}
