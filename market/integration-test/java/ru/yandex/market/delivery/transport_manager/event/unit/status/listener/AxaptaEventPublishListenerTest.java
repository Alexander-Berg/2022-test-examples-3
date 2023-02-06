package ru.yandex.market.delivery.transport_manager.event.unit.status.listener;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.unit.status.UnitStatusReceivedEvent;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class AxaptaEventPublishListenerTest extends AbstractContextualTest {
    private Transportation transportation;

    @Autowired
    private AxaptaEventPublishListener listener;

    @BeforeEach
    void setUp() {
        TransportationUnit outbound = new TransportationUnit().setId(2L).setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inbound = new TransportationUnit().setId(3L).setType(TransportationUnitType.INBOUND);
        Movement movement = new Movement().setId(2L);

        transportation = new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setOutboundUnit(outbound)
            .setInboundUnit(inbound)
            .setMovement(movement);
    }

    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/outbound_completed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void outboundCompleted() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getOutboundUnit(),
            transportation,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            TransportationUnitStatus.PROCESSED,
            LocalDateTime.now(clock),
            true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/axapta_event_empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void outboundCompletedTwice() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getOutboundUnit(),
            transportation,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            TransportationUnitStatus.PROCESSED,
            LocalDateTime.now(clock),
            false
        ));
    }

    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/inbound_completed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void inboundCompleted() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getInboundUnit(),
            transportation,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            TransportationUnitStatus.PROCESSED,
            LocalDateTime.now(clock),
            true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/axapta_event_empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void inboundCompletedTwice() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getInboundUnit(),
            transportation,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            TransportationUnitStatus.PROCESSED,
            LocalDateTime.now(clock),
            false
        ));
    }

    @ExpectedDatabase(
        value = "/repository/movement/after/movement_completed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void movementCompleted() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getInboundUnit(),
            transportation,
            TransportationUnitStatus.ACCEPTED,
            TransportationUnitStatus.IN_PROGRESS,
            LocalDateTime.now(clock),
            true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/axapta_event_empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void movementNotCompletedOnOutboundStarted() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getOutboundUnit(),
            transportation,
            TransportationUnitStatus.ACCEPTED,
            TransportationUnitStatus.IN_PROGRESS,
            LocalDateTime.now(clock),
            true
        ));
    }

    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/axapta_event_empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void movementCompletedTwice() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getInboundUnit(),
            transportation,
            TransportationUnitStatus.ACCEPTED,
            TransportationUnitStatus.IN_PROGRESS,
            LocalDateTime.now(clock),
            false
        ));
    }

    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/plan_registers.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/plan_regiser_axapta_event_outbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void planOutboundRegistryAccepted() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getOutboundUnit(),
            transportation,
            TransportationUnitStatus.ACCEPTED,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            LocalDateTime.now(clock),
            true
        ));
    }

    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/plan_registers.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/plan_regiser_axapta_event_outbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void planOutboundRegistryAcceptedTwice() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getOutboundUnit(),
            transportation,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            LocalDateTime.now(clock),
            false
        ));
    }

    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/plan_registers.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/plan_regiser_axapta_event_inbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void planInboundRegistryAccepted() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getInboundUnit(),
            transportation,
            TransportationUnitStatus.ACCEPTED,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            LocalDateTime.now(clock),
            true
        ));
    }

    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/plan_registers.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/plan_regiser_axapta_event_inbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    public void planInboundRegistryAcceptedTwice() {
        listener.listen(new UnitStatusReceivedEvent(
            transportation,
            transportation.getInboundUnit(),
            transportation,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED,
            LocalDateTime.now(clock),
            false
        ));
    }

}
