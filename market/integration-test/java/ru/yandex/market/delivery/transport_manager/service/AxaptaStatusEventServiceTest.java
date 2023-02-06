package ru.yandex.market.delivery.transport_manager.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.AxaptaEvent;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class AxaptaStatusEventServiceTest extends AbstractContextualTest {
    @Autowired
    private AxaptaStatusEventService axaptaStatusEventService;

    @Test
    @DisplayName("Создать для аксапты событие скачивания фактического реестра отгрузки")
    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/fact_regiser_axapta_event_outbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaFactOutboundRegisterEvent() {
        axaptaStatusEventService.createRegisterEvent(1L, TransportationUnitType.OUTBOUND, RegisterType.FACT);
    }

    @Test
    @DisplayName("Создать для аксапты событие скачивания фактического реестра приёмки")
    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/fact_regiser_axapta_event_inbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaFactInboundRegisterEvent() {
        axaptaStatusEventService.createRegisterEvent(2L, TransportationUnitType.INBOUND, RegisterType.FACT);
    }

    @Test
    @DisplayName("Создать для аксапты событие скачивания планового реестра отгрузки")
    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/plan_regiser_axapta_event_outbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaPlanOutboundRegisterEvent() {
        axaptaStatusEventService.createRegisterEvent(1L, TransportationUnitType.OUTBOUND, RegisterType.PLAN);
    }

    @Test
    @DisplayName("Создать для аксапты событие скачивания планового реестра приёмки")
    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/plan_regiser_axapta_event_inbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaPlannboundRegisterEvent() {
        axaptaStatusEventService.createRegisterEvent(2L, TransportationUnitType.INBOUND, RegisterType.PLAN);
    }

    @Test
    @DisplayName(
        "Не создавать для аксапты событие скачивания фактического реестра: id реестра отгрузки, а тип - приёмка"
    )
    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links_interwarehouse.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/axapta_event_empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaFactRegisterEventInconsistentIdAndType() {
        axaptaStatusEventService.createRegisterEvent(1L, TransportationUnitType.INBOUND, RegisterType.FACT);
    }

    @Test
    @DisplayName("Создать для аксапты событие завершения перевозки")
    @ExpectedDatabase(
        value = "/repository/movement/after/movement_completed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaMovementCompletedEvent() {
        axaptaStatusEventService.createMovementCompletedEvent(
            transportationWithCompletedMovement(),
            TransportationUnitType.INBOUND
        );
    }

    @Test
    @DisplayName("Создать для аксапты событие завершения отгрузки")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/outbound_completed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaOutboundCompletedEvent() {
        TransportationUnit outbound = new TransportationUnit().setId(2L).setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inbound = new TransportationUnit().setId(3L).setType(TransportationUnitType.INBOUND);
        axaptaStatusEventService.createTransportationUnitCompletedEvent(
            new Transportation()
                .setId(1L)
                .setTransportationType(TransportationType.INTERWAREHOUSE)
                .setOutboundUnit(outbound)
                .setInboundUnit(inbound),
            outbound
        );
    }

    @Test
    @DisplayName("Создать для аксапты событие завершения отгрузки: неподдерживаемый тип перемещения")
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/axapta_event_empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaOutboundCompletedEventUnsupportedType() {
        TransportationUnit outbound = new TransportationUnit().setId(2L).setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inbound = new TransportationUnit().setId(3L).setType(TransportationUnitType.INBOUND);
        axaptaStatusEventService.createTransportationUnitCompletedEvent(
            new Transportation()
                .setId(1L)
                .setTransportationType(TransportationType.ORDERS_OPERATION)
                .setOutboundUnit(outbound)
                .setInboundUnit(inbound),
            outbound
        );
    }

    @Test
    @DisplayName("Создать для аксапты событие завершения приёмки")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/inbound_completed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaInboundCompletedEvent() {
        TransportationUnit outbound = new TransportationUnit().setId(2L).setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inbound = new TransportationUnit().setId(3L).setType(TransportationUnitType.INBOUND);
        axaptaStatusEventService.createTransportationUnitCompletedEvent(
            new Transportation()
                .setId(1L)
                .setTransportationType(TransportationType.INTERWAREHOUSE)
                .setOutboundUnit(outbound)
                .setInboundUnit(inbound),
            inbound
        );
    }

    @Test
    @DisplayName("Создать для аксапты событие завершения приёмки: неподдерживаемый тип перемещения")
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/axapta_event_empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaInboundCompletedEventUnsupportedType() {
        TransportationUnit outbound = new TransportationUnit().setId(2L).setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inbound = new TransportationUnit().setId(3L).setType(TransportationUnitType.INBOUND);
        axaptaStatusEventService.createTransportationUnitCompletedEvent(
            new Transportation()
                .setId(1L)
                .setTransportationType(TransportationType.ORDERS_OPERATION)
                .setOutboundUnit(outbound)
                .setInboundUnit(inbound),
            inbound
        );
    }

    @Test
    @DisplayName("Создать для аксапты событие появления данных о курьере")
    @ExpectedDatabase(
        value = "/repository/movement/after/courier_found.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAxaptaCourierFoundEvent() {
        axaptaStatusEventService.createCourierFoundEvent(
            transportationWithCompletedMovement()
        );
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event.xml")
    @DisplayName("Поиск по ID перемещения и типу события")
    @Test
    void exists() {
        softly.assertThat(axaptaStatusEventService.exists(AxaptaEvent.Type.NEW_TRANSPORTATION, 1L))
            .isTrue();
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event.xml")
    @DisplayName("Поиск по ID перемещения и типу события: нет с таким ID перемещения")
    @Test
    void notExistsById() {
        softly.assertThat(axaptaStatusEventService.exists(AxaptaEvent.Type.NEW_TRANSPORTATION, 2L))
            .isFalse();
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event.xml")
    @DisplayName("Поиск по ID перемещения и типу события: нет с таким типом события")
    @Test
    void notExistsByType() {
        softly.assertThat(axaptaStatusEventService.exists(AxaptaEvent.Type.MOVEMENT_COMPLETED, 1L))
            .isFalse();
    }

    private Transportation transportationWithCompletedMovement() {
        return new Transportation()
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setMovement(new Movement().setId(2L))
            .setId(1L);
    }
}
