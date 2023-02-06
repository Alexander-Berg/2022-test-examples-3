package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminShipmentType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationLifecycleState;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.PointPair;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationAdditionalData;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationByEntity;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationByEntityMethod;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationRoutingConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationLifecycleState;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitSendingStrategy;
import ru.yandex.market.delivery.transport_manager.domain.filter.InternalTransportationSearchFilter;
import ru.yandex.market.delivery.transport_manager.domain.filter.TransportationFilter;
import ru.yandex.market.delivery.transport_manager.queue.health.AggregatedTransportationStat;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.TransportationStatusService;
import ru.yandex.market.delivery.transport_manager.service.health.transportation.AggregatedTransportationStatusStat;
import ru.yandex.market.delivery.transport_manager.util.TransportationStatusUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class TransportationMapperTest extends AbstractContextualTest {
    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private TransportationStatusService transportationStatusService;

    private static final Supplier<Transportation> XML_TRANSPORTATION;

    static {
        XML_TRANSPORTATION = () -> new Transportation()
            .setId(1L)
            .setStatus(TransportationStatus.SCHEDULED)
            .setOutboundUnit(new TransportationUnit()
                .setId(2L)
                .setPartnerId(5L)
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.OUTBOUND)
                .setLogisticPointId(2L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 10, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 10, 20, 0, 0))
                .setSendingStrategy(UnitSendingStrategy.VIA_FFWF_TO_LGW)
            )
            .setInboundUnit(new TransportationUnit()
                .setId(3L)
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.INBOUND)
                .setPartnerId(6L)
                .setLogisticPointId(2L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 12, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 12, 20, 0, 0))
            )
            .setMovement(new Movement()
                .setId(4L)
                .setPartnerId(7L)
                .setStatus(MovementStatus.NEW)
                .setWeight(94)
                .setVolume(15)
            )
            .setScheme(TransportationScheme.UNKNOWN)
            .setDeleted(true)
            .setRegular(true)
            .setTargetPartnerId(172L)
            .setTargetLogisticsPointId(10000004403L)
            .setHash("hash1")
            .setTransportationSource(TransportationSource.LMS_TM_MOVEMENT)
            .setTransportationType(TransportationType.ORDERS_OPERATION)
            .setPlannedLaunchTime(LocalDateTime.of(2020, 7, 11, 12, 0, 0))
            .setMovementSegmentId(100L)
            .setAdditionalData(new TransportationAdditionalData().setRoutingConfig(
                new TransportationRoutingConfig(
                    true,
                    DimensionsClass.MEDIUM_SIZE_CARGO,
                    1.1D,
                    false,
                    "DEFAULT"
                )
            ));

    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    void getTransportationTest() {
        Transportation transportation = transportationMapper.getById(XML_TRANSPORTATION.get().getId());
        assertThatModelEquals(XML_TRANSPORTATION.get(), transportation);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_dependencies.xml")
    void createTransportationTest() {
        Long transportationId = transportationMapper.persist(XML_TRANSPORTATION.get());
        Transportation transportation = transportationMapper.getById(transportationId);
        assertThatModelEquals(XML_TRANSPORTATION.get(), transportation);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_dependencies.xml")
    void createTransportationMissingTargetPartnerTest() {
        Transportation transportation = XML_TRANSPORTATION.get();
        transportation.setTargetPartnerId(null);
        softly
            .assertThatThrownBy(() -> transportationMapper.persist(transportation))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_dependencies.xml")
    void createTransportationMissingTargetLogisticsPointTest() {
        Transportation transportation = XML_TRANSPORTATION.get();
        transportation.setTargetLogisticsPointId(null);
        softly
            .assertThatThrownBy(() -> transportationMapper.persist(transportation))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_dependencies.xml")
    void createTransportationMissingTargetPartnerAndLogisticsPointTest() {
        Transportation expected = XML_TRANSPORTATION.get();
        expected.setTargetPartnerId(null);
        expected.setTargetLogisticsPointId(null);
        Long transportationId = transportationMapper.persist(expected);
        Transportation actual = transportationMapper.getById(transportationId);
        assertThatModelEquals(expected, actual);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    void testFindIds() {
        var ids = transportationMapper.findIds(TransportationStatus.NEW, 2);
        softly.assertThat(ids).containsOnly(2L, 3L);

        ids = transportationMapper.findIds(TransportationStatus.NEW, 1);
        softly.assertThat(ids).hasSize(1);
        softly.assertThat(ids).containsAnyOf(2L, 3L);

        ids = transportationMapper.findIds(TransportationStatus.CHECK_PREPARED, 1);
        softly.assertThat(ids).containsOnly(5L);

        ids = transportationMapper.findIds(TransportationStatus.SCHEDULED, 100);
        softly.assertThat(ids).containsOnly(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    void testSetScheme() {
        transportationMapper.setScheme(2L, TransportationScheme.COMBINED);
        var transportation = transportationMapper.getById(2L).getScheme();

        softly.assertThat(transportation).isEqualTo(TransportationScheme.COMBINED);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml"
    })
    void findWithStatusTest() {
        List<Long> withStatus =
            transportationMapper.findIdsForDay(
                TransportationStatus.SCHEDULED,
                LocalDateTime.of(2020, 7, 10, 15, 0)
            );
        softly.assertThat(withStatus).isNotEmpty();
        softly.assertThat(withStatus.size()).isEqualTo(2);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml",
        "/repository/transportation/transportation_not_from_lms.xml"
    })
    void findUpcoming() {
        List<Transportation> transportations =
            transportationMapper.findUpcoming(
                LocalDateTime.of(2020, 7, 10, 14, 0),
                Set.of(3L),
                null
            );
        softly.assertThat(transportations.size()).isEqualTo(1);
        softly.assertThat(transportations.get(0).getId()).isEqualTo(3);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml"
    })
    void findNearestXDocTransport() {
        Transportation transportation = transportationMapper.findNearestByPartnersTypeStatus(
            LocalDateTime.of(2020, 7, 10, 14, 0),
            1L,
            2L,
            TransportationStatus.SCHEDULED,
            TransportationType.ORDERS_OPERATION
        );

        softly.assertThat(transportation).isNotNull();
        softly.assertThat(transportation.getId()).isEqualTo(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_test.xml"
    })
    @ExpectedDatabase(value = "/repository/transportation/after/set_removed.xml", assertionMode = NON_STRICT_UNORDERED)
    void setDeleted() {
        transportationMapper.setDeleted(1L, false);
        transportationMapper.setDeleted(2L, true);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup({
        "/repository/transportation/additional_transportations.xml"
    })
    @MethodSource("provideFilters")
    void findByFilterTest(String caseName, TransportationFilter filter, List<Long> transportationsToFind) {
        softly.assertThat(
            transportationMapper.find(filter, Pageable.unpaged())
                .stream()
                .map(Transportation::getId)
                .collect(Collectors.toSet()))
            .containsExactlyInAnyOrderElementsOf(transportationsToFind);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    void testFailedTransportationsHealth() {
        List<AggregatedTransportationStat> aggregatedTransportationStats = transportationMapper.countActiveByDays(
            TransportationStatus.ERROR,
            LocalDate.of(2020, 7, 14).atStartOfDay(),
            7
        );
        softly.assertThat(aggregatedTransportationStats).isEmpty();

        transportationStatusService.setTransportationStatus(List.of(2L), TransportationStatus.ERROR);
        aggregatedTransportationStats = transportationMapper.countActiveByDays(
            TransportationStatus.ERROR,
            LocalDate.of(2020, 7, 14).atStartOfDay(),
            7
        );

        softly.assertThat(aggregatedTransportationStats).isEqualTo(
            List.of(
                new AggregatedTransportationStat()
                    .setDate(LocalDate.of(2020, 7, 10))
                    .setStatus(TransportationStatus.ERROR)
                    .setAmount(1)
            )
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/additional_transportations.xml"
    })
    void paginationTest() {
        // 2 перемещения
        TransportationFilter filter = new TransportationFilter().setAdminTransportationStatus(
            AdminTransportationStatus.SCHEDULED
        );

        softly.assertThat(
            transportationMapper.find(filter, PageRequest.of(0, 1, Sort.by("id")))
                .stream()
                .map(Transportation::getId)
                .collect(Collectors.toSet()))
            .isEqualTo(Set.of(5L));

        softly.assertThat(
            transportationMapper.find(filter, PageRequest.of(1, 1, Sort.by("id")))
                .stream()
                .map(Transportation::getId)
                .collect(Collectors.toSet()))
            .isEqualTo(Set.of(6L));

        softly.assertThat(
            transportationMapper.find(filter, PageRequest.of(0, 10, Sort.by("id")))
                .stream()
                .map(Transportation::getId)
                .collect(Collectors.toSet()))
            .isEqualTo(Set.of(5L, 6L));

        softly.assertThat(transportationMapper.count(filter)).isEqualTo(2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    void filterByStateAndTypeTest() {
        TransportationFilter filter = new TransportationFilter()
            .setState(AdminTransportationLifecycleState.NEW)
            .setTransportationType(TransportationType.XDOC_TRANSPORT);

        softly.assertThat(
                transportationMapper.find(filter, PageRequest.of(0, 10, Sort.by("id")))
            )
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(5L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/check_for_outdated_transportations.xml")
    void findOutdatedTransportations() {
        clock.setFixed(Instant.parse("2021-01-20T21:00:00.00Z"), ZoneOffset.UTC);
        Set<Long> outdatedTransportations =
            new HashSet<>(transportationMapper.findOutdatedTransportationIds(clock.instant(), 3, 10));
        softly.assertThat(outdatedTransportations).containsExactly(1L, 4L, 5L, 6L, 7L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/check_for_outdated_transportations.xml")
    void findOutdatedTransportationByIds() {
        clock.setFixed(Instant.parse("2021-01-20T21:00:00.00Z"), ZoneOffset.UTC);
        Set<Long> outdatedTransportations =
            new HashSet<>(transportationMapper.findOutdatedTransportationIds(clock.instant(), 3, 10));
        Set<Transportation> outdatedTransportationsByIds =
            new HashSet<>(transportationMapper.getByIds(outdatedTransportations));
        softly.assertThat(outdatedTransportationsByIds).isNotEmpty();
        softly.assertThat(outdatedTransportationsByIds).allSatisfy(transportation -> {
            softly.assertThat(transportation.getInboundUnit()).isNotNull();
            softly.assertThat(transportation.getOutboundUnit()).isNotNull();
            softly.assertThat(transportation.getMovement()).isNotNull();
        });
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    void getByUnitIds() {
        Set<Transportation> transportations = transportationMapper.getByUnitIds(
            List.of(XML_TRANSPORTATION.get().getInboundUnit().getId())
        );
        softly.assertThat(transportations).containsExactlyInAnyOrder(XML_TRANSPORTATION.get());
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportations_with_planned_launch_time.xml")
    void findIdsWithLessLaunchTime() {
        clock.setFixed(Instant.parse("2021-01-20T21:00:00.00Z"), ZoneOffset.UTC);
        List<Long> idsWithLessLaunchTime = transportationMapper.findIdsWithLessLaunchTime(
            TransportationStatus.SCHEDULED,
            LocalDateTime.now(clock)
        );

        softly.assertThat(idsWithLessLaunchTime).containsExactlyInAnyOrder(2L, 10L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportations_with_planned_launch_time.xml")
    void countTransportationsWithStatusAndLaunchTimeBefore() {
        clock.setFixed(Instant.parse("2021-01-20T21:00:00.00Z"), ZoneOffset.UTC);
        Long count = transportationMapper.countTransportationsWithStatusAndLaunchTimeBefore(
            TransportationStatus.SCHEDULED,
            LocalDateTime.now(clock)
        );

        softly.assertThat(count).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportations_with_planned_launch_time.xml")
    void getTransportationIdsWithTypeStatusSubstatusAndStartAfterTime() {
        clock.setFixed(Instant.parse("2021-01-20T21:00:00.00Z"), ZoneOffset.UTC);
        List<Long> count = transportationMapper.getTransportationIdsWithTypeStatusSubstatusAndStartAfterTime(
            TransportationType.ORDERS_OPERATION,
            TransportationStatus.CANCELLED,
            List.of(TransportationSubstatus.NO_WAREHOUSE_SLOTS_AVAILABLE),
            LocalDateTime.now(clock)
        );

        softly.assertThat(count).containsExactlyInAnyOrder(4L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportations_with_planned_launch_time.xml")
    void dropSubstatus() {
        softly.assertThat(transportationMapper.getById(4L).getSubStatus()).isNotNull();
        transportationMapper.dropSubstatus(4L);
        softly.assertThat(transportationMapper.getById(4L).getSubStatus()).isNull();
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportations_with_full_metadata.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_transportation_with_meta_deleted.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deleteTransportationsAndMetaOnly() {
        transportationMapper.deleteTransportations(Set.of(1L, 2L, 3L));
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    void getTransportationStatisticsWithOnlyStatusTest() {
        LocalDateTime dateTimePeriodFrom = LocalDateTime.of(2020, 9, 7, 20, 0);
        LocalDateTime dateTimePeriodTo = LocalDateTime.of(2020, 9, 7, 20, 0);
        clock.setFixed(dateTimePeriodFrom.toInstant(ZoneOffset.UTC), ZoneId.ofOffset("UTC", ZoneOffset.UTC));

        List<AggregatedTransportationStatusStat> result =
            transportationMapper
                .countByStatusInPeriod(dateTimePeriodFrom.toLocalDate(), dateTimePeriodTo.toLocalDate());

        softly.assertThat(result.size()).isEqualTo(1);
        softly.assertThat(result.get(0).getAmount()).isEqualTo(4);
        softly.assertThat(result.get(0).getStatus()).isEqualTo(TransportationStatus.NEW);
    }

    @Test
    @DatabaseSetup("/repository/transportation/additional_transportations.xml")
    void getTransportationStatisticsWithManyStatusesTest() {
        LocalDateTime dateTimePeriodFrom = LocalDateTime.of(2020, 7, 10, 20, 0);
        LocalDateTime dateTimePeriodTo = LocalDateTime.of(2020, 9, 10, 20, 0);
        clock.setFixed(dateTimePeriodFrom.toInstant(ZoneOffset.UTC), ZoneId.ofOffset("UTC", ZoneOffset.UTC));

        Map<TransportationStatus, Integer> resultMap =
            transportationMapper.countByStatusInPeriod(dateTimePeriodFrom.toLocalDate(), dateTimePeriodTo.toLocalDate())
                .stream().collect(
                    Collectors.toMap(
                        AggregatedTransportationStatusStat::getStatus,
                        AggregatedTransportationStatusStat::getAmount
                    )
                );

        softly.assertThat(resultMap.size()).isEqualTo(2);
        softly.assertThat(resultMap.get(TransportationStatus.SCHEDULED)).isEqualTo(1);
        softly.assertThat(resultMap.get(TransportationStatus.CHECK_PREPARED)).isEqualTo(1);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/transportation/all_kinds_of_transportation.xml",
            "/repository/transportation_task/transportation_tasks.xml",
            "/repository/transportation_task/transportation_task_transportations.xml"
        }
    )
    void getByTransportationTaskId() {
        List<Transportation> byTransportationTaskId = transportationMapper.getByTransportationTaskId(2L);

        softly.assertThat(byTransportationTaskId).containsExactlyInAnyOrder(
            transportationMapper.getById(1L),
            transportationMapper.getById(2L),
            transportationMapper.getById(3L)
        );
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/order/orders_by_registers.xml"
        }
    )
    void getByBarcode() {
        List<Transportation> transportations = transportationMapper.findByOrderBarcode("B108324523");
        List<Long> transportationIds = transportations.stream().map(Transportation::getId).collect(Collectors.toList());
        softly.assertThat(transportationIds).containsExactlyInAnyOrder(15L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
    @SuppressWarnings("unchecked")
    void search() {
        List<Transportation> byOutboundUnit = transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(123L))
                .outboundDateFrom(LocalDate.of(2021, 3, 2))
                .outboundDateTo(LocalDate.of(2021, 3, 4))
                .outboundStatuses(Set.of(TransportationUnitStatus.SENT))
                .build(),
            Pageable.unpaged()
        );

        softly.assertThat(byOutboundUnit).hasSize(1);
        softly.assertThat(byOutboundUnit.get(0))
            .extracting(
                Transportation::getId,
                Transportation::getStatus,
                Transportation::getTransportationSource,
                Transportation::getTransportationType
            ).containsExactly(
                1L,
                TransportationStatus.RECEIVED,
                TransportationSource.LMS_TM_MOVEMENT,
                TransportationType.ORDERS_OPERATION
            );
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
    void searchWithIds() {
        List<Transportation> byOutboundUnitEmpty = transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(123L))
                .outboundDateFrom(LocalDate.of(2021, 3, 2))
                .outboundDateTo(LocalDate.of(2021, 3, 4))
                .outboundStatuses(Set.of(TransportationUnitStatus.SENT))
                .transportationIds(Set.of(5L))
                .build(),
            Pageable.unpaged()
        );

        List<Transportation> byOutboundUnitSingle = transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(123L))
                .outboundDateFrom(LocalDate.of(2021, 3, 2))
                .outboundDateTo(LocalDate.of(2021, 3, 4))
                .outboundStatuses(Set.of(TransportationUnitStatus.SENT))
                .transportationIds(Set.of(1L))
                .build(),
            Pageable.unpaged()
        );

        softly.assertThat(byOutboundUnitEmpty).isEmpty();
        softly.assertThat(byOutboundUnitSingle).hasSize(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
    void searchAndOutboundOrderIds() {
        List<Transportation> byOutboundUnitWithOrderIds = transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(123L))
                .outboundOrderIds(Set.of(12345L, 23456L))
                .build(),
            Pageable.unpaged()
        );

        List<Transportation> byOutboundUnitWithOrderIdsEmpty = transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(123L))
                .outboundOrderIds(Set.of(34567L))
                .build(),
            Pageable.unpaged()
        );

        softly.assertThat(byOutboundUnitWithOrderIdsEmpty).isEmpty();
        softly.assertThat(byOutboundUnitWithOrderIds).hasSize(2);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
    void getByMovementExcludePartnerIds() {
        softly.assertThat(transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(123L))
                .movementExcludePartnerIds(Set.of(123L))
                .build(),
            Pageable.unpaged()
        ))
            .extracting(Transportation::getId)
            .containsExactly(2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
    })
    void getByMultipleOutboundPartnerIds() {
        softly.assertThat(transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(10L, 15L))
                .build(),
            Pageable.unpaged()
        ))
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
    void getByOutboundLogisticPoints() {
        softly.assertThat(transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundLogisticPointIds(Set.of(3L))
                .build(),
            Pageable.unpaged()
        ))
            .extracting(Transportation::getId)
            .containsExactly(2L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
    void getByInboundLogisticPoints() {
        softly.assertThat(transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .inboundLogisticPointIds(Set.of(4L))
                .build(),
            Pageable.unpaged()
        ))
            .extracting(Transportation::getId)
            .containsExactly(2L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
    void getByInboundRequestIds() {
        softly.assertThat(transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .inboundRequestIds(Set.of(2L))
                .build(),
            Pageable.unpaged()
        ))
            .extracting(Transportation::getId)
            .containsExactly(1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
    void getByOutboundRequestIds() {
        softly.assertThat(transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundRequestIds(Set.of(3L))
                .build(),
            Pageable.unpaged()
        ))
            .extracting(Transportation::getId)
            .containsExactly(2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
    })
    void getByTransportationTypes() {
        softly.assertThat(transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .transportationTypes(Set.of(TransportationType.XDOC_TRANSPORT))
                .build(),
            Pageable.unpaged()
        ))
            .extracting(Transportation::getId)
            .containsExactly(6L);
    }

    @DatabaseSetup({
        "/repository/route/route.xml",
        "/repository/route_schedule/route_schedule.xml",
        "/repository/trip/trips.xml"
    })
    @Test
    void getByTrip() {
        softly.assertThat(transportationMapper.search(
                InternalTransportationSearchFilter.builder()
                    .tripIds(Set.of(10L))
                    .build(),
                Pageable.unpaged()
            ))
            .extracting(Transportation::getId)
            .containsExactly(100L);
    }

    @DatabaseSetup("/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml")
    @Test
    void getBySubtype() {
        softly.assertThat(transportationMapper.search(
                InternalTransportationSearchFilter.builder()
                    .transportationSubtypes(Set.of(TransportationSubtype.BREAK_BULK_XDOCK))
                    .build(),
                Pageable.unpaged()
            ))
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links.xml",
        }
    )
    @Test
    void getTransportationIdByOutboundUnitId() {
        softly.assertThat(transportationMapper.getTransportationIdByOutboundUnitId(2L)).isEqualTo(1L);
    }

    @Test
    void getTransportationIdByOutboundUnitIdReturnsNullOnMissing() {
        softly.assertThat(transportationMapper.getTransportationIdByOutboundUnitId(666L)).isEqualTo(null);
    }

    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links.xml",
        }
    )
    @Test
    void getTransportationIdByInboundUnitId() {
        softly.assertThat(transportationMapper.getTransportationIdByInboundUnitId(3L)).isEqualTo(1L);
    }

    @Test
    void getTransportationIdByInboundUnitIdReturnsNullOnMissing() {
        softly.assertThat(transportationMapper.getTransportationIdByInboundUnitId(666L)).isEqualTo(null);
    }

    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    @Test
    void getTransportationsByInboundUnitIds() {
        Transportation transportation = XML_TRANSPORTATION.get();

        softly
            .assertThat(transportationMapper.getTransportationsByInboundUnitIds(List.of(
                transportation.getInboundUnit().getId()
            )))
            .isEqualTo(List.of(transportation));
    }

    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    @Test
    void getTransportationsByOutboundUnitIds() {
        Transportation transportation = XML_TRANSPORTATION.get();

        softly
            .assertThat(transportationMapper.getTransportationsByOutboundUnitIds(List.of(
                transportation.getOutboundUnit().getId()
            )))
            .isEqualTo(List.of(transportation));
    }

    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml",
    })
    @Test
    void getTransportationIdsByMovement() {
        softly
            .assertThat(transportationMapper.getTransportationIdsByMovement(4L))
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/search_by_entity_id.xml"
    })
    void getByEntityId() {
        List<TransportationByEntity> transportations = transportationMapper.getByEntityId(null, "4");

        TransportationByEntity byId = new TransportationByEntity()
            .setTransportation(transportationMapper.getById(4))
            .setMethod(TransportationByEntityMethod.BY_TRANSPORTATION);

        TransportationByEntity byMovementId = new TransportationByEntity()
            .setTransportation(transportationMapper.getById(5))
            .setMethod(TransportationByEntityMethod.BY_MOVEMENT);

        softly.assertThat(transportations).containsExactlyInAnyOrder(byId, byMovementId);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/search_by_entity_id.xml"
    })
    void getByEntityWithPrefix() {
        List<TransportationByEntity> transportations = transportationMapper.getByEntityId("TMU", "7");
        softly.assertThat(transportations.size()).isEqualTo(1);

        TransportationByEntity byOutbound = new TransportationByEntity()
            .setTransportation(transportationMapper.getById(4))
            .setMethod(TransportationByEntityMethod.BY_OUTBOUND);

        softly.assertThat(transportations).containsExactlyInAnyOrder(byOutbound);

    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/search_by_entity_id.xml"
    })
    void getByEntityIdExternal() {
        List<TransportationByEntity> transportations = transportationMapper.getByEntityId(null, "EXT8");
        softly.assertThat(transportations.size()).isEqualTo(1);

        TransportationByEntity byInboundExt = new TransportationByEntity()
            .setTransportation(transportationMapper.getById(4))
            .setMethod(TransportationByEntityMethod.BY_INBOUND_EXT);

        softly.assertThat(transportations).containsExactlyInAnyOrder(byInboundExt);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/search_by_entity_id.xml"
    })
    void getByEntityIdTag() {
        List<TransportationByEntity> transportations = transportationMapper.getByEntityId(null, "Зп-111");
        softly.assertThat(transportations.size()).isEqualTo(1);

        TransportationByEntity byInboundExt = new TransportationByEntity()
            .setTransportation(transportationMapper.getById(4))
            .setMethod(TransportationByEntityMethod.BY_TAG);

        softly.assertThat(transportations).containsExactlyInAnyOrder(byInboundExt);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_for_states.xml"
    })
    void searchByState() {
        List<Transportation> finished = getByState(TransportationLifecycleState.FINISHED);
        softly.assertThat(finished.stream().map(Transportation::getId).collect(Collectors.toSet())).isEqualTo(Set.of(5L,
            6L, 7L));

        List<Transportation> transporting = getByState(TransportationLifecycleState.TRANSPORTING);
        softly.assertThat(transporting.stream().map(Transportation::getId).collect(Collectors.toSet()))
            .isEqualTo(Set.of(3L, 4L));

        List<Transportation> prepared = getByState(TransportationLifecycleState.PREPARED);
        softly.assertThat(prepared.stream().map(Transportation::getId).collect(Collectors.toSet()))
            .isEqualTo(Set.of(2L));

        List<Transportation> newTransportations = getByState(TransportationLifecycleState.NEW);
        softly.assertThat(newTransportations.stream().map(Transportation::getId).collect(Collectors.toSet())).isEqualTo(
            Set.of(1L));

        List<Transportation> requiredForTsup = transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .transportationLifecycleStates(Set.of(
                    TransportationLifecycleState.FINISHED,
                    TransportationLifecycleState.PREPARED
                ))
                .build(),
            Pageable.unpaged()
        );
        softly.assertThat(requiredForTsup.stream().map(Transportation::getId).collect(Collectors.toSet())).isEqualTo(Set
            .of(2L, 5L, 6L, 7L));

    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_for_states.xml"
    })
    void mapperConditionsConsistentWithUtilStateDetection() {
        List<Transportation> finished = getByState(TransportationLifecycleState.FINISHED);
        softly.assertThat(finished.stream()
            .map(TransportationStatusUtil::getLifecycleState)
            .collect(Collectors.toSet())).isEqualTo(Set.of(TransportationLifecycleState.FINISHED));

        List<Transportation> transporting = getByState(TransportationLifecycleState.TRANSPORTING);
        softly.assertThat(transporting.stream()
            .map(TransportationStatusUtil::getLifecycleState)
            .collect(Collectors.toSet())).isEqualTo(Set.of(TransportationLifecycleState.TRANSPORTING));

        List<Transportation> prepared = getByState(TransportationLifecycleState.PREPARED);
        softly.assertThat(prepared.stream()
            .map(TransportationStatusUtil::getLifecycleState)
            .collect(Collectors.toSet())).isEqualTo(Set.of(TransportationLifecycleState.PREPARED));

        List<Transportation> newTransportations = getByState(TransportationLifecycleState.NEW);
        softly.assertThat(newTransportations.stream()
            .map(TransportationStatusUtil::getLifecycleState)
            .collect(Collectors.toSet())).isEqualTo(Set.of(TransportationLifecycleState.NEW));

        List<Transportation> error = getByState(TransportationLifecycleState.ERROR);
        softly.assertThat(error.stream()
            .map(TransportationStatusUtil::getLifecycleState)
            .collect(Collectors.toSet())).isEqualTo(Set.of(TransportationLifecycleState.ERROR));
    }

    private List<Transportation> getByState(TransportationLifecycleState finished) {
        return transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .transportationLifecycleStates(Set.of(finished))
                .build(),
            Pageable.unpaged()
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/interwarehouse/regular_xdoc.xml")
    void getRegularInterwarehouseForEnrichment() {
        clock.setFixed(Instant.parse("2021-02-23T23:00:00.00Z"), ZoneOffset.UTC);
        List<Long> transportationIds = transportationMapper.findDraftByType(
            LocalDateTime.now(clock),
            Set.of(TransportationType.XDOC_TRANSPORT, TransportationType.LINEHAUL)
        );
        softly.assertThat(transportationIds).containsOnly(100L);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links.xml",
        }
    )
    void getByRegisterIdEmpty() {
        softly
            .assertThat(transportationMapper.getByRegisterId(2L, TransportationUnitType.INBOUND))
            .isNull();
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/fetch_registries.xml",
            "/repository/facade/register_facade/register_links.xml",
        }
    )
    void getByRegisterId() {
        softly
            .assertThat(transportationMapper.getByRegisterId(1L, TransportationUnitType.OUTBOUND).getId())
            .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    void getStatus() {
        softly.assertThat(transportationMapper.getStatus(1L)).isEqualTo(TransportationStatus.SCHEDULED);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/transportation_status_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCount() {
        softly.assertThat(transportationMapper.switchStatusReturningCount(
            1L,
            TransportationStatus.SCHEDULED,
            TransportationStatus.SCHEDULED_WAITING_REQUEST
        ))
            .isEqualTo(1);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/transportation_status_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountWithoutPreviousStatusCheck() {
        softly.assertThat(transportationMapper.switchStatusReturningCount(
                1L,
                null,
                TransportationStatus.SCHEDULED_WAITING_REQUEST
            ))
            .isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    void getByPointsAndOutboundDate() {
        List<Transportation> transportations = transportationMapper.getByPointsAndOutboundDate(
            List.of(new PointPair(10000004403L, 10000004555L), new PointPair(10001584835L, 10000029109L)),
            LocalDate.of(2020, 9, 7)
        );

        Set<Long> transportationIds = transportations.stream().map(Transportation::getId).collect(Collectors.toSet());
        softly.assertThat(transportationIds).containsExactlyInAnyOrder(1L, 2L, 3L, 6L);

    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_movement_change.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void setMovementIds() {
        transportationMapper.setMovementId(Set.of(1L, 2L), 4L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/upcoming_by_type.xml")
    void findUpcomingByType() {
        List<Transportation> found = transportationMapper.findUpcomingByType(
            TransportationType.RETURN_FROM_SC_TO_DROPOFF,
            LocalDate.of(2020, 9, 28)
        );
        softly.assertThat(found.stream().map(Transportation::getId).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder(2L);
    }

    @Test
    @DatabaseSetup("/repository/transportation/not_approved_in_time.xml")
    void findNotApprovedInTime() {
        List<Long> ids = transportationMapper.findNotApprovedInTime(
            TransportationType.LINEHAUL,
            10,
            LocalDateTime.of(2021, 11, 2, 22, 15)
        );
        softly.assertThat(ids).containsExactlyInAnyOrder(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/transportation_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateByIdAndStatus() {
        transportationMapper.updateByIdAndStatus(
            new Transportation()
                .setId(1L)
                .setPlannedLaunchTime(LocalDateTime.of(2021, 12, 1, 10, 0))
                .setMovementSegmentId(10000L)
                .setHash("12345")
                .setOutboundUnit(new TransportationUnit().setId(4L))
                .setInboundUnit(new TransportationUnit().setId(5L))
                .setMovement(new Movement())
                .setDeleted(true),
            TransportationStatus.SCHEDULED
        );
    }

    @Test
    @DatabaseSetup("/repository/interwarehouse/auto_approve/tasks_and_different_transportations.xml")
    void testGetUpcomingTransportationsByTypeWithMovementsInStatuses() {
        clock.setFixed(Instant.parse("2021-02-20T21:00:00.00Z"), ZoneOffset.UTC);
        var ids = transportationMapper.getUpcomingTransportationsByTypeWithMovementsInStatuses(
            Set.of(MovementStatus.LGW_CREATED, MovementStatus.PARTNER_CREATED),
            LocalDateTime.now(clock),
            List.of(TransportationType.INTERWAREHOUSE)
        );

        Assertions.assertThat(ids).containsExactlyInAnyOrder(5L, 500L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/transportation_test.xml"
    })
    void findExistingPlannedIntervalStartDates() {
        List<LocalDate> existingPlannedIntervalStartDates =
            transportationMapper.findExistingPlannedIntervalStartDates(
                TransportationUnitType.OUTBOUND,
                List.of(
                    LocalDate.of(2020, 7, 10),
                    LocalDate.of(2020, 7, 12)
                )
            );

        softly.assertThat(existingPlannedIntervalStartDates)
            .containsExactlyInAnyOrder(LocalDate.of(2020, 7, 10));
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_dependencies.xml")
    void testInsertAndSelectSubtype() {
        TransportationSubtype subtype = TransportationSubtype.SUPPLEMENTARY_1;
        var t = XML_TRANSPORTATION.get()
            .setTransportationType(TransportationType.LINEHAUL)
            .setSubtype(subtype);

        Long id = transportationMapper.insert(t).getId();

        softly.assertThat(transportationMapper.getById(id))
            .extracting(Transportation::getSubtype)
            .isEqualTo(subtype);
    }

    @Test
    @DatabaseSetup("/repository/transportation/transportation_wtih_tag.xml")
    void testFindTransportationByTag() {
        var res = transportationMapper.getTransportationsByTagCodeAndValue(TagCode.XDOC_PARENT_REQUEST_ID, "Зп-123456");

        var transportation = res.get(0);

        softly.assertThat(transportation.getId()).isEqualTo(1);
        softly.assertThat(transportation.getTransportationType()).isEqualTo(TransportationType.ORDERS_OPERATION);
    }


    static Stream<Arguments> provideFilters() {
        return Stream.of(
            Arguments.of(
                "Id",
                new TransportationFilter().setId(6L),
                List.of(6L)
            ),
            Arguments.of(
                "Удаленные",
                new TransportationFilter().setDeleted(true),
                List.of(6L)
            ),
            Arguments.of(
                "Тип отгрузки",
                new TransportationFilter().setAdminShipmentType(AdminShipmentType.INTAKE),
                List.of(6L)
            ),
            Arguments.of(
                "Статус",
                new TransportationFilter().setAdminTransportationStatus(AdminTransportationStatus.CHECK_PREPARED),
                List.of(4L)
            ),
            Arguments.of(
                "Дата создания",
                new TransportationFilter().setCreated(LocalDate.of(2020, 9, 10)),
                List.of(5L, 6L)
            ),
            Arguments.of(
                "Планируемая дата перемещения",
                new TransportationFilter().setPlanned(LocalDate.of(2020, 9, 11)),
                List.of(6L)
            ),
            Arguments.of(
                "Id отправляющего",
                new TransportationFilter().setOutboundPartnerIds(List.of(10L)),
                List.of(4L)
            ),
            Arguments.of(
                "Id отправляющего",
                new TransportationFilter().setOutboundPartnerId(10L),
                List.of(4L)
            ),
            Arguments.of(
                "Id точки отправляющего",
                new TransportationFilter().setOutboundLogisticPointId(30L),
                List.of(5L)
            ),
            Arguments.of(
                "Id перемещающего",
                new TransportationFilter().setMovingPartnerId(20L),
                List.of(5L)
            ),
            Arguments.of(
                "Id принимающего",
                new TransportationFilter().setInboundPartnerIds(List.of(30L)),
                List.of(6L)
            ),
            Arguments.of(
                "Id принимающего",
                new TransportationFilter().setInboundPartnerId(30L),
                List.of(6L)
            ),
            Arguments.of(
                "Id точки принимающего",
                new TransportationFilter().setInboundLogisticPointId(60L),
                List.of(6L)
            )
        );
    }
}
