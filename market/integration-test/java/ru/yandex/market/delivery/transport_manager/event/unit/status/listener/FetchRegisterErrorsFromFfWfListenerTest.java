package ru.yandex.market.delivery.transport_manager.event.unit.status.listener;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitSendingStrategy;
import ru.yandex.market.delivery.transport_manager.event.unit.status.UnitStatusReceivedEvent;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.ffwf_error.GetFfwfRegisterErrorProducer;

@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
@DatabaseSetup("/repository/service/status_flow/consistency/before.xml")
class FetchRegisterErrorsFromFfWfListenerTest extends AbstractContextualTest {

    @Autowired
    private FetchRegisterErrorsFromFfWfListener listener;

    @Autowired
    private GetFfwfRegisterErrorProducer producer;

    @Test
    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/dbqueue_after_register_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void testInvoke() {
        listener.listen(event(
            1,
            TransportationUnitType.OUTBOUND,
            TransportationUnitStatus.PLAN_REGISTRY_SENT,
            TransportationUnitStatus.ERROR,
            UnitSendingStrategy.VIA_FFWF_TO_LGW
        ));
    }

    @Test
    void testIgnore() {
        listener.listen(event(
            1,
            TransportationUnitType.OUTBOUND,
            TransportationUnitStatus.PLAN_REGISTRY_SENT,
            TransportationUnitStatus.ERROR,
            UnitSendingStrategy.DIRECTLY_TO_LGW
        ));
        listener.listen(event(
            1,
            TransportationUnitType.OUTBOUND,
            TransportationUnitStatus.SENT,
            TransportationUnitStatus.ERROR,
            UnitSendingStrategy.VIA_FFWF_TO_LGW
        ));
        listener.listen(event(
            1,
            TransportationUnitType.OUTBOUND,
            TransportationUnitStatus.PLAN_REGISTRY_CREATED,
            TransportationUnitStatus.PLAN_REGISTRY_SENT,
            UnitSendingStrategy.VIA_FFWF_TO_LGW
        ));
        listener.listen(event(
            4,
            TransportationUnitType.INBOUND,
            TransportationUnitStatus.PLAN_REGISTRY_SENT,
            TransportationUnitStatus.ERROR,
            UnitSendingStrategy.VIA_FFWF_TO_LGW
        ));

        Mockito.verifyNoInteractions(producer);
    }

    private UnitStatusReceivedEvent event(
        long unitId,
        TransportationUnitType type,
        TransportationUnitStatus oldStatus,
        TransportationUnitStatus newStatus,
        UnitSendingStrategy strategy
    ) {
        return new UnitStatusReceivedEvent(
            this,
            new TransportationUnit()
                .setId(unitId)
                .setType(type)
                .setStatus(oldStatus)
                .setSendingStrategy(strategy),
            new Transportation(),
            oldStatus,
            newStatus,
            LocalDateTime.now(),
            true
        );
    }
}
