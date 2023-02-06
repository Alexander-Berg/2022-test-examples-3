package ru.yandex.market.delivery.transport_manager.facade;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorTicketDto;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.TransportationCancellationProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.TransportationStatusService;
import ru.yandex.market.delivery.transport_manager.service.checker.TransportationExternalInfoSaver;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.delivery.transport_manager.task.dto.TransportationWithPreviousStatus;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;

@DatabaseSetup("/repository/interwarehouse/regular_xdoc_for_check.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class TransportationUpdateFacadeTest extends AbstractContextualTest {
    @Autowired
    private TransportationUpdateFacade transportationUpdateFacade;

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private TransportationExternalInfoSaver saver;

    @Autowired
    private TransportationStatusService transportationStatusService;

    @Autowired
    private TransportationCancellationProducer cancellationProducer;

    @BeforeEach
    void init() {
        Mockito.doNothing().when(saver).save(Mockito.any());

        clock.setFixed(
            LocalDateTime.of(2021, 2, 24, 0, 0).toInstant(TimeUtil.DEFAULT_ZONE_OFFSET),
            TimeUtil.DEFAULT_ZONE_OFFSET
        );
    }

    @Test
    @DisplayName("Need to cancel deleted xdoc immediately")
    void markDeletedForXDoc() {
        Transportation xdoc = transportationMapper.getById(100L);
        Transportation orders = transportationMapper.getById(105L);

        transportationUpdateFacade.markAsDeletedIfPossible(List.of(xdoc, orders));
        Mockito.verify(cancellationProducer)
            .enqueue(xdoc.getId(), TransportationSubstatus.INTERWAREHOUSE_DELETED);
    }

    @Test
    void orderOperationCheckSuccess() {
        Transportation orders = transportationMapper.getById(105L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(orders),
            TransportationStatus.CHECK_FAILED,
            ""
        );
        Transportation updated = transportationMapper.getById(105L);
        softly.assertThat(updated.getStatus()).isEqualTo(TransportationStatus.SCHEDULED);
    }

    @Test
    void orderOperationCheckFailed() {
        Transportation orders = transportationMapper.getById(104L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation()
                .setTransportation(orders)
                .setError(new StartrekErrorTicketDto()),
            TransportationStatus.SCHEDULED,
            ""
        );
        Transportation updated = transportationMapper.getById(104L);
        softly.assertThat(updated.getStatus()).isEqualTo(TransportationStatus.CHECK_FAILED);
    }

    @Test
    @DisplayName("If from failed state the check succeeds, change status to draft")
    void updateExistingXDocSuccess() {
        Transportation t = transportationMapper.getById(101L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(t),
            TransportationStatus.CHECK_FAILED,
            ""
        );
        Transportation updated = transportationMapper.getById(101L);
        softly.assertThat(updated.getStatus()).isEqualTo(TransportationStatus.DRAFT);

        t = transportationMapper.getById(102L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(t),
            TransportationStatus.CANCELLED,
            ""
        );
        updated = transportationMapper.getById(102L);
        softly.assertThat(updated.getStatus()).isEqualTo(TransportationStatus.DRAFT);

        t = transportationMapper.getById(103L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(t),
            TransportationStatus.CANCELLED,
            ""
        );
        updated = transportationMapper.getById(103L);
        softly.assertThat(updated.getStatus()).isEqualTo(TransportationStatus.DRAFT);
    }

    @Test
    @DisplayName("If fail from MOVER_FOUND, need to cancel transportation")
    void updateExistingXDocFail() {
        Transportation t = transportationMapper.getById(100L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(t).setError(new StartrekErrorTicketDto()),
            TransportationStatus.MOVER_FOUND,
            ""
        );
        Transportation updated = transportationMapper.getById(100L);
        softly.assertThat(updated.getStatus()).isEqualTo(TransportationStatus.CHECK_FAILED);

        Mockito.verify(cancellationProducer)
            .enqueue(t.getId(), TransportationSubstatus.INTERWAREHOUSE_CHECK_FAILED);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/interwarehouse/regular_xdoc_for_check.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sameStatusNoUpdate() {
        Transportation t = transportationMapper.getById(100L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(t),
            TransportationStatus.MOVER_FOUND,
            ""
        );

        t = transportationMapper.getById(101L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(t).setError(new StartrekErrorTicketDto()),
            TransportationStatus.CHECK_FAILED,
            ""
        );

        t = transportationMapper.getById(102L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(t).setError(new StartrekErrorTicketDto()),
            TransportationStatus.CANCELLED,
            ""
        );

        t = transportationMapper.getById(103L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            new EnrichedTransportation().setTransportation(t).setError(new StartrekErrorTicketDto()),
            TransportationStatus.CANCELLED,
            ""
        );

        Mockito.verify(transportationStatusService, Mockito.times(0)).setStatus(Mockito.any(), Mockito.any());
        Mockito.verify(transportationStatusService, Mockito.times(0))
            .setTransportationErrorStatus(Mockito.anyLong(), Mockito.any(), Mockito.any());

    }

    @ExpectedDatabase(
        value = "/repository/transportation/after/checker_dbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    void runChecker() {
        clock.setFixed(
            LocalDateTime.of(2021, 2, 22, 0, 0).toInstant(TimeUtil.DEFAULT_ZONE_OFFSET),
            TimeUtil.DEFAULT_ZONE_OFFSET
        );
        Transportation t = transportationMapper.getById(104L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            List.of(new TransportationWithPreviousStatus(
                t,
                TransportationStatus.SCHEDULED,
                false
            ))
        );
    }


    @ExpectedDatabase(
        value = "/repository/health/dbqueue/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    void skipChecker() {
        clock.setFixed(
            LocalDateTime.of(2021, 2, 23, 0, 0).toInstant(TimeUtil.DEFAULT_ZONE_OFFSET),
            TimeUtil.DEFAULT_ZONE_OFFSET
        );
        Transportation t = transportationMapper.getById(104L);
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            List.of(new TransportationWithPreviousStatus(
                t,
                TransportationStatus.SCHEDULED,
                false
            ))
        );
    }
}
