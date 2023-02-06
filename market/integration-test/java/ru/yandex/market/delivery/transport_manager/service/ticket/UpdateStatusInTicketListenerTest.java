package ru.yandex.market.delivery.transport_manager.service.ticket;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.UpdateStatusInTicketListener;
import ru.yandex.market.delivery.transport_manager.util.UnitStatusReceivedUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
@DatabaseSetup("/repository/service/status_flow/consistency/before.xml")
public class UpdateStatusInTicketListenerTest extends AbstractContextualTest {

    @Autowired
    private UpdateStatusInTicketListener updateStatusInTicketListener;

    @ExpectedDatabase(
        value = "/repository/service/ticket/change_ticket_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void changeTicketStatus() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE)).thenReturn(true);

        Transportation transportation =
            UnitStatusReceivedUtils.transportation(TransportationType.INTERWAREHOUSE);
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.OUTBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(transportation, unit, true));
    }

    @ExpectedDatabase(
        value = "/repository/service/ticket/change_ticket_task_to_processed.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void changeTicketStatusToProcessed() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE)).thenReturn(true);

        Transportation transportation =
            UnitStatusReceivedUtils.transportation(
                TransportationType.INTERWAREHOUSE,
                TransportationUnitStatus.PROCESSED
            );
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.OUTBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(
            transportation,
            unit,
            TransportationUnitStatus.PROCESSED,
            true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/service/ticket/change_ticket_task_to_withdraw.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void changeTicketStatusToWithdraw() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE)).thenReturn(true);

        Transportation transportation =
            UnitStatusReceivedUtils.transportation(
                TransportationType.INTERWAREHOUSE,
                TransportationUnitStatus.PROCESSED
            );
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.OUTBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(
            transportation,
            unit,
            TransportationUnitStatus.READY_TO_WITHDRAW,
            true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/service/ticket/change_ticket_task_to_acceptance.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void changeTicketStatusToAcceptance() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE)).thenReturn(true);

        Transportation transportation =
            UnitStatusReceivedUtils.transportation(TransportationType.INTERWAREHOUSE, TransportationUnitStatus.NEW);
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.INBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(
            transportation,
            unit,
            TransportationUnitStatus.PROCESSED,
            true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/health/dbqueue/empty.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void changeTicketStatusWithoutPermission() {
        Transportation transportation =
            UnitStatusReceivedUtils.transportation(TransportationType.INTERWAREHOUSE);
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.OUTBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(transportation, unit, true));
    }

    @ExpectedDatabase(
        value = "/repository/health/dbqueue/empty.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void changeTicketStatusWithWrongType() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE)).thenReturn(true);

        Transportation transportation =
            UnitStatusReceivedUtils.transportation(TransportationType.INTERWAREHOUSE, TransportationUnitStatus.NEW);
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.OUTBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(transportation,
            unit, TransportationUnitStatus.NEW, true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/health/dbqueue/empty.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void changeTicketStatusWithWrongTransportation() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE)).thenReturn(true);

        Transportation transportation =
            UnitStatusReceivedUtils.transportation(
                TransportationType.INTERWAREHOUSE,
                TransportationUnitStatus.ACCEPTED,
                10L
            );
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.OUTBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(transportation, unit, true));
    }

    @ExpectedDatabase(
        value = "/repository/health/dbqueue/empty.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void changeTicketStatusWithoutTransportation() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE)).thenReturn(true);

        Transportation transportation =
            UnitStatusReceivedUtils.transportation(
                TransportationType.INTERWAREHOUSE,
                TransportationUnitStatus.ACCEPTED,
                10000L
            );
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.OUTBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(transportation, unit, true));
    }

    @DatabaseSetup(
        value = "/repository/service/status_flow/consistency/interwarehouse_transport_transportation.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/service/ticket/change_ticket_task_to_acceptance.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    void changeTicketStatusForInterwarehouseTransport() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE)).thenReturn(true);

        Transportation transportation = UnitStatusReceivedUtils
            .transportation(TransportationType.INTERWAREHOUSE, TransportationUnitStatus.NEW)
            .setId(3L)
            .setSubtype(TransportationSubtype.INTERWAREHOUSE_FIT);
        TransportationUnit unit = UnitStatusReceivedUtils.unit(TransportationUnitType.INBOUND, "not null");

        updateStatusInTicketListener.listen(UnitStatusReceivedUtils.event(
            transportation,
            unit,
            TransportationUnitStatus.PROCESSED,
            true
        ));
    }

}
