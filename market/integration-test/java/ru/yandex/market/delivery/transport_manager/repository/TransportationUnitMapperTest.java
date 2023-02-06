package ru.yandex.market.delivery.transport_manager.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.RegisterUnitBarcodeAndType;
import ru.yandex.market.delivery.transport_manager.domain.entity.StatusHolder;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;

public class TransportationUnitMapperTest extends AbstractContextualTest {
    @Autowired
    private TransportationUnitMapper transportationUnitMapper;

    private static final TransportationUnit XML_TRANSPORTATION_UNIT;

    @Autowired
    private TransactionTemplate transactionTemplate;

    static {
        XML_TRANSPORTATION_UNIT = new TransportationUnit()
            .setRequestId(12345L)
            .setStatus(TransportationUnitStatus.NEW)
            .setType(TransportationUnitType.INBOUND)
            .setLogisticPointId(2L)
            .setPartnerId(3L)
            .setMarketId(33L)
            .setPlannedIntervalStart(LocalDateTime.of(2020, 3, 1, 0, 0))
            .setPlannedIntervalEnd(LocalDateTime.of(2020, 3, 1, 0, 0))
            .setCutoffTime(LocalTime.of(10, 0))
            .setWarehouseOffsetSeconds(3600)
            .setActualDateTime(LocalDateTime.of(2020, 3, 3, 0, 0))
            .setSelectedCalendaringServiceId(1002L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void getTransportUnitTest() {
        TransportationUnit transportationUnit = transportationUnitMapper.getById(1);
        assertThatModelEquals(XML_TRANSPORTATION_UNIT, transportationUnit);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void getTransportUnitsTest() {
        List<TransportationUnit> units = transportationUnitMapper.getByIds(List.of(1L));
        Assertions.assertThat(units)
            .usingRecursiveFieldByFieldElementComparator()
            .usingElementComparatorIgnoringFields("id", "created", "updated")
            .containsExactlyInAnyOrder(XML_TRANSPORTATION_UNIT);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void getTransportUnitsTest0() {
        List<TransportationUnit> units = transportationUnitMapper.getByIds(List.of());
        Assertions.assertThat(units).isEmpty();
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void getTransportUnitsTestNull() {
        List<TransportationUnit> units = transportationUnitMapper.getByIds(null);
        Assertions.assertThat(units).isEmpty();
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void getTransportUnitsTestWrongId() {
        List<TransportationUnit> units = transportationUnitMapper.getByIds(List.of(100000000000L));
        Assertions.assertThat(units).isEmpty();
    }

    @Test
    void createTransactionUnitTest() {
        Long transportationUnitId =
            transportationUnitMapper.persist(XML_TRANSPORTATION_UNIT);
        TransportationUnit transportationUnit = transportationUnitMapper.getById(transportationUnitId);
        assertThatModelEquals(XML_TRANSPORTATION_UNIT, transportationUnit);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void saveExternalIdTest() {
        String externalId = "externalId1";
        transportationUnitMapper.saveExternalId(1, externalId);
        TransportationUnit unit = transportationUnitMapper.getById(1);
        softly.assertThat(unit.getExternalId()).isEqualTo(externalId);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_with_external_id.xml")
    void saveExternalIdNullTest() {
        String externalId = "externalId1";
        transportationUnitMapper.saveExternalId(1, null);
        TransportationUnit unit = transportationUnitMapper.getById(1);
        softly.assertThat(unit.getExternalId()).isNull();
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_without_actual_date_time.xml")
    void saveActualDateTime() {
        LocalDateTime actualDateTime = LocalDate.of(2020, 3, 2).atTime(0, 0);
        transportationUnitMapper.saveActualDateTimeIfNull(1, actualDateTime);
        softly.assertThat(transportationUnitMapper.getById(1L).getActualDateTime()).isEqualTo(actualDateTime);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void changeActualDateTime() {
        LocalDateTime actualDateTime = LocalDate.of(2020, 3, 2).atTime(0, 0);
        transportationUnitMapper.saveActualDateTimeIfNull(1, actualDateTime);
        softly
            .assertThat(transportationUnitMapper.getById(1L).getActualDateTime())
            .isEqualTo(LocalDate.of(2020, 3, 3).atTime(0, 0));
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/transportation_unit_sent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void setStatus() {
        softly
            .assertThat(transportationUnitMapper.setStatus(1L, TransportationUnitStatus.SENT))
            .containsExactly(
                new StatusHolder<>(1L, "INBOUND", TransportationUnitStatus.NEW, TransportationUnitStatus.SENT)
            );
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_dependencies.xml")
    @DatabaseSetup("/repository/transportation/single_new_transportation.xml")
    void getOutboundByTransportation() {
        TransportationUnit outbound = transportationUnitMapper.getOutbound(1L);
        softly.assertThat(outbound.getId()).isEqualTo(2L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_dependencies.xml")
    @DatabaseSetup("/repository/transportation/single_new_transportation.xml")
    void getInboundByTransportation() {
        TransportationUnit inbound = transportationUnitMapper.getInbound(1L);
        softly.assertThat(inbound.getId()).isEqualTo(3L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/multiple_transportation_units.xml")
    void getAllExternalIdForStatusesTest() {
        Set<Long> allRequestIdsForStatus =
            transportationUnitMapper.getAllRequestIdsForStatus(TransportationUnitStatus.SENT);

        softly.assertThat(allRequestIdsForStatus).containsAll(Set.of(777L));
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_new.xml")
    void saveRequestIdIfNullTest() {
        long requestId = 876;
        Integer count = transactionTemplate.execute(tx -> {
            transportationUnitMapper.saveRequestIdIfNull(1, requestId);
            return transportationUnitMapper.updatedCount();
        });
        TransportationUnit unit = transportationUnitMapper.getById(1);
        softly.assertThat(unit.getRequestId()).isEqualTo(requestId);
        softly.assertThat(count).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_sent.xml")
    void saveRequestIdIfNullAlreadyDefinedTest() {
        long requestId = 876;
        Integer count = transactionTemplate.execute(tx -> {
            transportationUnitMapper.saveRequestIdIfNull(1, requestId);
            return transportationUnitMapper.updatedCount();
        });
        TransportationUnit unit = transportationUnitMapper.getById(1);
        softly.assertThat(unit.getRequestId()).isEqualTo(12345);
        softly.assertThat(count).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_new.xml")
    void saveRequestIdIfGreaterOrNullTest() {
        long requestId = 876;
        Integer count = transactionTemplate.execute(tx -> {
            transportationUnitMapper.saveRequestIdIfGreaterOrNull(1, requestId);
            return transportationUnitMapper.updatedCount();
        });
        TransportationUnit unit = transportationUnitMapper.getById(1);
        softly.assertThat(unit.getRequestId()).isEqualTo(requestId);
        softly.assertThat(count).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_sent.xml")
    void saveRequestIdIfGreaterOrNullAlreadyDefinedGreaterTest() {
        long requestId = 12346;
        Integer count = transactionTemplate.execute(tx -> {
            transportationUnitMapper.saveRequestIdIfGreaterOrNull(1, requestId);
            return transportationUnitMapper.updatedCount();
        });
        TransportationUnit unit = transportationUnitMapper.getById(1);
        softly.assertThat(unit.getRequestId()).isEqualTo(12346);
        softly.assertThat(count).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_sent.xml")
    void clearRequestIdTest() {
        Integer count = transactionTemplate.execute(tx -> {
            transportationUnitMapper.clearRequestId(1);
            return transportationUnitMapper.updatedCount();
        });
        TransportationUnit unit = transportationUnitMapper.getById(1);
        softly.assertThat(unit.getRequestId()).isNull();
        softly.assertThat(count).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_sent.xml")
    void saveRequestIdIfGreaterOrNullAlreadyDefinedLessTest() {
        long requestId = 876;
        Integer count = transactionTemplate.execute(tx -> {
            transportationUnitMapper.saveRequestIdIfGreaterOrNull(1, requestId);
            return transportationUnitMapper.updatedCount();
        });
        TransportationUnit unit = transportationUnitMapper.getById(1);
        softly.assertThat(unit.getRequestId()).isEqualTo(12345);
        softly.assertThat(count).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/multiple_transportation_units.xml")
    void getByRequestIdTest() {
        TransportationUnit unit = transportationUnitMapper.getByRequestId(777L);

        softly.assertThat(unit.getId()).isEqualTo(2L);
        softly.assertThat(unit.getStatus()).isEqualTo(TransportationUnitStatus.SENT);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/multiple_transportation_units.xml")
    void countByRequestIdTest() {
        int count = transportationUnitMapper.countByRequestId(777L);

        softly.assertThat(count).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/multiple_transportation_units.xml")
    void countByRequestId0Test() {
        int count = transportationUnitMapper.countByRequestId(778L);

        softly.assertThat(count).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/inbound_transportation_unit_with_outbound_register.xml")
    void getInboundTransportationUnitByOutboundRegisterId() {
        TransportationUnit unit = transportationUnitMapper.getInboundTransportationUnitByOutboundRegisterId(27L);

        softly.assertThat(unit.getId()).isEqualTo(1L);
        softly.assertThat(unit.getType()).isEqualTo(TransportationUnitType.INBOUND);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/inbound_transportation_unit_with_outbound_register.xml")
    void getIdByRegisterId() {
        softly.assertThat(transportationUnitMapper.getIdByRegisterId(27L)).isEqualTo(2L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/inbound_transportation_unit_with_outbound_register.xml")
    void getIdByRegisterIdNull() {
        softly.assertThat(transportationUnitMapper.getIdByRegisterId(28L)).isNull();
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    void getStatus() {
        softly.assertThat(transportationUnitMapper.getStatus(1L)).isEqualTo(TransportationUnitStatus.NEW);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/transportation_unit_status_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCount() {
        softly.assertThat(transportationUnitMapper.switchStatusReturningCount(
            1L,
            TransportationUnitStatus.NEW,
            TransportationUnitStatus.SENT
        ))
            .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/transportation_unit_status_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountWithoutPreviousStatusCheck() {
        softly.assertThat(transportationUnitMapper.switchStatusReturningCount(
            1L,
            null,
            TransportationUnitStatus.SENT
        ))
            .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/transportation_unit_test.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountSkip() {
        softly.assertThat(transportationUnitMapper.switchStatusReturningCount(
            2L,
            TransportationUnitStatus.NEW,
            TransportationUnitStatus.SENT
        ))
            .isEqualTo(0L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_unit_test.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit/after/transportation_unit_planned_interval_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updatePlannedInterval() {
        transportationUnitMapper.updatePlannedInterval(
            1L,
            LocalDateTime.of(2021, 5, 1, 0, 0),
            null
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/inbound_transportation_unit_with_outbound_register.xml")
    void getByRegisterId() {
        TransportationUnit unit = transportationUnitMapper.getByRegisterId(27L);

        TransportationUnit expected =
            new TransportationUnit()
                .setId(2L)
                .setType(TransportationUnitType.OUTBOUND)
                .setStatus(TransportationUnitStatus.NEW)
                .setLogisticPointId(3L)
                .setPartnerId(4L)
                .setPlannedIntervalStart(LocalDateTime.parse("2020-03-01T00:00:00"))
                .setPlannedIntervalEnd(LocalDateTime.parse("2020-03-01T00:00:00"))
                .setActualDateTime(LocalDateTime.parse("2020-03-03T00:00:00"));

        assertThatModelEquals(expected, unit);
    }

    @DisplayName("Получить id реквестов по типу, штрихкоду, типу перемещения и реестра")
    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml")
    void getRequestIdsByBarcodesTransportationTypeAndRegisterType1() {
        softly
            .assertThat(transportationUnitMapper.getXDockRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(new RegisterUnitBarcodeAndType("abc", UnitType.PALLET)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER,
                RegisterType.FACT,
                TagCode.FFWF_ROOT_REQUEST_ID,
                2L
            ))
            .containsExactly(123456L);
    }

    @DisplayName("Получить id реквестов по типу, штрихкоду, типу перемещения и реестра: неправильный тип")
    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml")
    void getRequestIdsByBarcodesTransportationTypeAndRegisterTypeWrongUnitType() {
        softly
            .assertThat(transportationUnitMapper.getXDockRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(new RegisterUnitBarcodeAndType("abc", UnitType.BOX)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER,
                RegisterType.FACT,
                TagCode.FFWF_ROOT_REQUEST_ID,
                2L
            ))
            .isEmpty();
    }

    @DisplayName("Получить id реквестов по типу, штрихкоду, типу перемещения и реестра")
    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml")
    void getRequestIdsByBarcodesTransportationTypeAndRegisterType2() {
        softly
            .assertThat(transportationUnitMapper.getXDockRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(
                    new RegisterUnitBarcodeAndType("abc", UnitType.PALLET),
                    new RegisterUnitBarcodeAndType("def", UnitType.PALLET)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER,
                RegisterType.FACT,
                TagCode.FFWF_ROOT_REQUEST_ID,
                2L
            ))
            .containsExactlyInAnyOrder(123456L, 654321L);
    }

    @DisplayName("Получить id реквестов по типу, штрихкоду, типу перемещения и реестра: частично неправильные типы")
    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml")
    void getRequestIdsByBarcodesTransportationTypeAndRegisterType2PartialWrongType() {
        softly
            .assertThat(transportationUnitMapper.getXDockRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(
                    new RegisterUnitBarcodeAndType("abc", UnitType.PALLET),
                    new RegisterUnitBarcodeAndType("def", UnitType.BOX)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER,
                RegisterType.FACT,
                TagCode.FFWF_ROOT_REQUEST_ID,
                2L
            ))
            .containsExactlyInAnyOrder(123456L);
    }

    @DisplayName("Получить id реквестов по типу, штрихкоду, типу перемещения и реестра: неправильная лог. точка")
    @Test
    @DatabaseSetup("/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml")
    void getRequestIdsByBarcodesTransportationTypeAndRegisterType2WrongLogisticsPoint() {
        softly
            .assertThat(transportationUnitMapper.getXDockRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(
                    new RegisterUnitBarcodeAndType("abc", UnitType.PALLET),
                    new RegisterUnitBarcodeAndType("def", UnitType.PALLET)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER,
                RegisterType.FACT,
                TagCode.FFWF_ROOT_REQUEST_ID,
                3L
            ))
            .isEmpty();
    }

    @DisplayName("Получить id реквестов по типу, штрихкоду, типу перемещения и реестра для Break Bulk XDock")
    @Test
    @DatabaseSetup(
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units_break_bulk_xdock.xml"
    )
    void getBreakBulkXdockRequestIdsByBarcodesTransportationTypeAndRegister() {
        softly
            .assertThat(transportationUnitMapper.getAssemblageRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(new RegisterUnitBarcodeAndType("abc", UnitType.PALLET)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                RegisterType.PREPARED,
                40L,
                30L
            ))
            .containsExactly(123456L);
    }

    @DisplayName(
        "Получить id реквестов по типу, штрихкоду, типу перемещения и реестра для Break Bulk XDock: неправильный тип"
    )
    @Test
    @DatabaseSetup(
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units_break_bulk_xdock.xml"
    )
    void getBreakBulkXdockRequestIdsByBarcodesTransportationTypeAndRegisterTypeWrongUnitType() {
        softly
            .assertThat(transportationUnitMapper.getAssemblageRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(new RegisterUnitBarcodeAndType("abc", UnitType.BOX)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                RegisterType.FACT,
                40L,
                30L
            ))
            .isEmpty();
    }

    @DisplayName("Получить id реквестов по типу, штрихкоду, типу перемещения и реестра для Break Bulk XDock")
    @Test
    @DatabaseSetup(
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units_break_bulk_xdock.xml"
    )
    void getBreakBulkXdockRequestIdsByBarcodesTransportationTypeAndRegisterType2() {
        softly
            .assertThat(transportationUnitMapper.getAssemblageRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(
                    new RegisterUnitBarcodeAndType("abc", UnitType.PALLET),
                    new RegisterUnitBarcodeAndType("def", UnitType.PALLET)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                RegisterType.PREPARED,
                40L,
                30L
            ))
            .containsExactlyInAnyOrder(123456L);
    }

    @DisplayName(
        "Получить id реквестов по типу, штрихкоду, типу перемещения и реестра для Break Bulk XDock: "
            + "частично неправильные типы"
    )
    @Test
    @DatabaseSetup(
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units_break_bulk_xdock.xml"
    )
    void getBreakBulkXdockRequestIdsByBarcodesTransportationTypeAndRegisterType2PartialWrongType() {
        softly
            .assertThat(transportationUnitMapper.getAssemblageRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(
                    new RegisterUnitBarcodeAndType("abc", UnitType.PALLET),
                    new RegisterUnitBarcodeAndType("def", UnitType.BOX)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                RegisterType.PREPARED,
                40L,
                30L
            ))
            .containsExactlyInAnyOrder(123456L);
    }

    @DisplayName(
        "Получить id реквестов по типу, штрихкоду, типу перемещения и реестра  для Break Bulk XDock: "
            + "неправильная лог. точка РЦ"
    )
    @Test
    @DatabaseSetup(
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units_break_bulk_xdock.xml"
    )
    void getBreakBulkXdockRequestIdsByBarcodesTransportationTypeAndRegisterType2WrongLogisticsPointFrom() {
        softly
            .assertThat(transportationUnitMapper.getAssemblageRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(
                    new RegisterUnitBarcodeAndType("abc", UnitType.PALLET),
                    new RegisterUnitBarcodeAndType("def", UnitType.PALLET)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                RegisterType.FACT,
                400L,
                30L
            ))
            .isEmpty();
    }

    @DisplayName(
        "Получить id реквестов по типу, штрихкоду, типу перемещения и реестра  для Break Bulk XDock: "
            + "неправильная лог. точка ФФЦ"
    )
    @Test
    @DatabaseSetup(
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units_break_bulk_xdock.xml"
    )
    void getBreakBulkXdockRequestIdsByBarcodesTransportationTypeAndRegisterType2WrongLogisticsPointTo() {
        softly
            .assertThat(transportationUnitMapper.getAssemblageRequestIdsByBarcodesTransportationTypeAndRegisterType(
                List.of(
                    new RegisterUnitBarcodeAndType("abc", UnitType.PALLET),
                    new RegisterUnitBarcodeAndType("def", UnitType.PALLET)),
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                RegisterType.FACT,
                40L,
                300L
            ))
            .isEmpty();
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit/before/units_get_by_partner_and_external_id.xml")
    void getByPartnerIdExternalIdAndType() {
        softly.assertThat(transportationUnitMapper
            .getByPartnerIdExternalIdAndType(2L, "missing", TransportationUnitType.OUTBOUND))
            .isEmpty();

        softly.assertThat(transportationUnitMapper
                .getByPartnerIdExternalIdAndType(2L, "missing", TransportationUnitType.INBOUND))
            .isEmpty();

        softly.assertThat(transportationUnitMapper
                .getByPartnerIdExternalIdAndType(2L, "duplicate", TransportationUnitType.INBOUND))
            .extracting(TransportationUnit::getId)
            .containsExactlyInAnyOrder(2L);

        softly.assertThat(transportationUnitMapper
                .getByPartnerIdExternalIdAndType(2L, "duplicate", TransportationUnitType.OUTBOUND))
            .extracting(TransportationUnit::getId)
            .containsExactlyInAnyOrder(3L);

        softly.assertThat(transportationUnitMapper
            .getByPartnerIdExternalIdAndType(2L, "unique", TransportationUnitType.INBOUND))
            .extracting(TransportationUnit::getId)
            .containsExactlyInAnyOrder(1L);
    }
}
