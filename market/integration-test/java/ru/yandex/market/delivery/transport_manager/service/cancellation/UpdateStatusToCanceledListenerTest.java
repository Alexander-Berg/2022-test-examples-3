package ru.yandex.market.delivery.transport_manager.service.cancellation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.UpdateStatusToCanceledListener;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.TransportationCancellationProducer;
import ru.yandex.market.delivery.transport_manager.util.UnitStatusReceivedUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
@DatabaseSetup("/repository/service/status_flow/consistency/before.xml")
public class UpdateStatusToCanceledListenerTest extends AbstractContextualTest {

    @Autowired
    private TransportationCancellationProducer cancellationProducer;

    private UpdateStatusToCanceledListener updateStatusToCanceledListener;

    @BeforeEach
    void init() {
        updateStatusToCanceledListener = new UpdateStatusToCanceledListener(cancellationProducer);
    }

    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/cancel_transportation_task_inbound_cancelled.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void testInboundCancelled() {
        Transportation transportation =
            UnitStatusReceivedUtils.transportation(TransportationType.INTERWAREHOUSE);
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.INBOUND, "not null");

        updateStatusToCanceledListener.listen(UnitStatusReceivedUtils.event(
            transportation,
            unit,
            TransportationUnitStatus.CANCELLED,
            true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/service/status_flow/consistency/cancel_transportation_task_outbound_cancelled.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void testOutboundCancelled() {
        Transportation transportation =
            UnitStatusReceivedUtils.transportation(TransportationType.INTERWAREHOUSE);
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.OUTBOUND, "not null");

        updateStatusToCanceledListener.listen(UnitStatusReceivedUtils.event(
            transportation,
            unit,
            TransportationUnitStatus.CANCELLED,
            true
        ));
    }

}
