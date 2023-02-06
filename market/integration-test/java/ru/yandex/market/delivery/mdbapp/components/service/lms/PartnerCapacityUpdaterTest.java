package ru.yandex.market.delivery.mdbapp.components.service.lms;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;
import steps.utils.TestableClock;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.queue.order.to.ship.dto.OrderToShipDto;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.CapacityCountersUpdater;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.PartnerCapacityUpdater;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.CapacityCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.DeletedPartnerCapacity;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShip;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShipId;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShipValue;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.PartnerCapacity;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.CapacityCountingType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.CapacityType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.CapacityCounterRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.DeletedPartnerCapacityRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderToShipRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PartnerCapacityRepository;
import ru.yandex.market.delivery.mdbapp.enums.PlatformClient;
import ru.yandex.market.delivery.mdbapp.util.GeoTestUtils;
import ru.yandex.market.delivery.mdbapp.util.NumberUtils;
import ru.yandex.market.delivery.mdbclient.model.dto.CapacityServiceType;
import ru.yandex.market.delivery.mdbclient.model.dto.DeliveryType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.type.CountingType;

import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DirtiesContext
public class PartnerCapacityUpdaterTest extends AllMockContextualTest {

    @MockBean
    private LMSClient lmsClient;

    @Autowired
    private GeoInfo geoInfo;

    @Autowired
    private HealthManager healthManager;

    @SpyBean
    private PartnerCapacityUpdater updater;

    @Autowired
    private PartnerCapacityRepository partnerCapacityRepository;

    @Autowired
    private DeletedPartnerCapacityRepository deletedPartnerCapacityRepository;

    @Autowired
    private CapacityCounterRepository counterRepository;

    @Autowired
    private OrderToShipRepository orderToShipRepository;

    @Autowired
    private TestableClock clock;

    @SpyBean
    private CapacityCountersUpdater capacityCountersUpdater;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Before
    public void beforeTest() {
        clock.setFixed(LocalDate.of(2019, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
        when(healthManager.isHealthyEnough()).thenReturn(true);
        when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());
    }

    @After
    public void tearDown() {
        clock.clearFixed();
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/partner-capacity-updater.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testAddToEmpty() {
        Pair<List<PartnerCapacity>, List<PartnerCapacityDto>> capacities = generateEqualsLists(getOne());
        when(lmsClient.getPartnerCapacities()).thenReturn(capacities.getSecond());
        updater.updatePartnerCapacities();
        softly.assertThat(partnerCapacityRepository.findAllWithCounters()).hasSameElementsAs(capacities.getFirst());
        List<DeletedPartnerCapacity> deletedPartnerCapacities = deletedPartnerCapacityRepository.findAll();
        softly.assertThat(deletedPartnerCapacities).extracting(DeletedPartnerCapacity::getEntityId)
            .hasSize(2)
            .containsOnly(1L, 3L);
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/partner-capacity-updater-platforms.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testUpdateIntersection() {
        Pair<List<PartnerCapacity>, List<PartnerCapacityDto>> capacities = generateEqualsLists(getNewIntersection());
        when(lmsClient.getPartnerCapacities()).thenReturn(capacities.getSecond());
        updater.updatePartnerCapacities();
        softly.assertThat(partnerCapacityRepository.findAllWithCounters()).hasSameElementsAs(capacities.getFirst());
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/partner-capacity-updater.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testAddNewDeleteOld() {
        Pair<List<PartnerCapacity>, List<PartnerCapacityDto>> capacities = generateEqualsLists(getNewAndOld());
        when(lmsClient.getPartnerCapacities()).thenReturn(capacities.getSecond());
        updater.updatePartnerCapacities();
        softly.assertThat(partnerCapacityRepository.findAllWithCounters()).hasSameElementsAs(capacities.getFirst());
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/partner-capacity-updater.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testAddNewDeleteOldSamePartner() {
        Pair<List<PartnerCapacity>, List<PartnerCapacityDto>> capacities = generateEqualsLists(getNewSamePartner());
        when(lmsClient.getPartnerCapacities()).thenReturn(capacities.getSecond());
        updater.updatePartnerCapacities();
        softly.assertThat(partnerCapacityRepository.findAllWithCounters()).hasSameElementsAs(capacities.getFirst());
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/partner-capacity-updater.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testUpdateSameData() {
        Pair<List<PartnerCapacity>, List<PartnerCapacityDto>> capacities = generateEqualsLists(getSame());
        when(lmsClient.getPartnerCapacities()).thenReturn(capacities.getSecond());
        updater.updatePartnerCapacities();
        softly.assertThat(partnerCapacityRepository.findAllWithCounters()).hasSameElementsAs(capacities.getFirst());
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/partner-capacity-updater-with-counting-type.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testChangeCountingType() {
        Pair<List<PartnerCapacity>, List<PartnerCapacityDto>> capacities = generateEqualsLists(getOne());
        when(lmsClient.getPartnerCapacities()).thenReturn(capacities.getSecond());
        updater.updatePartnerCapacities();
        softly.assertThat(partnerCapacityRepository.findAllWithCounters()).hasSameElementsAs(capacities.getFirst());
        List<DeletedPartnerCapacity> deletedPartnerCapacities = deletedPartnerCapacityRepository.findAll();
        softly.assertThat(deletedPartnerCapacities).extracting(DeletedPartnerCapacity::getEntityId)
            .hasSize(2)
            .containsOnly(1L, 3L);
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/capacity-counters_1.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testOneDayOffCreated() {
        when(lmsClient.getPartnerCapacities()).thenReturn(Collections.singletonList(
            PartnerCapacityDto.newBuilder()
                .id(5L)
                .partnerId(2L)
                .locationFrom(1)
                .locationTo(20482)
                .type(ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR)
                .platformClientId(1L)
                .day(LocalDate.of(2019, 1, 1))
                .value(2L)
                .build()
        ));
        updater.updatePartnerCapacities();
        verify(lmsClient).getPartnerCapacities();
        verify(lmsClient).createPartnerCapacityDayOff(5L, LocalDate.of(2019, 1, 1));
        verifyNoMoreInteractions(lmsClient);

        List<PartnerCapacity> partnerCapacities = partnerCapacityRepository.findAll();
        softly.assertThat(partnerCapacities).extracting(PartnerCapacity::getCapacityId)
            .hasSize(1)
            .containsOnly(5L);
        List<CapacityCounter> capacityCounters = counterRepository.findAll();
        softly.assertThat(capacityCounters)
            .hasSize(1)
            .extracting(CapacityCounter::getIsDayOffCreated)
            .containsOnly(true);
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/capacity-counters_1_item.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testOneDayOffCreatedUsingCountingTypeItem() {
        when(lmsClient.getPartnerCapacities()).thenReturn(Collections.singletonList(
            PartnerCapacityDto.newBuilder()
                .id(5L)
                .partnerId(2L)
                .locationFrom(1)
                .locationTo(20482)
                .type(ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR)
                .platformClientId(1L)
                .day(LocalDate.of(2019, 1, 1))
                .value(36L)
                .countingType(CountingType.ITEM)
                .build()
        ));
        updater.updatePartnerCapacities();
        verify(lmsClient).getPartnerCapacities();
        verify(lmsClient).createPartnerCapacityDayOff(5L, LocalDate.of(2019, 1, 1));
        verifyNoMoreInteractions(lmsClient);

        List<PartnerCapacity> partnerCapacities = partnerCapacityRepository.findAllWithCounters();
        softly.assertThat(partnerCapacities).extracting(PartnerCapacity::getCapacityId)
            .hasSize(1)
            .containsOnly(5L);

        softly
            .assertThat(partnerCapacities.get(0).getCapacityCounters().iterator().next().getMaxAllowedParcelCount())
            .isEqualTo(36);
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/capacity-counters_2.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testOneDayOffCreatedCancelledOrdersNotProcessed() {
        when(lmsClient.getPartnerCapacities()).thenReturn(Collections.singletonList(
            PartnerCapacityDto.newBuilder()
                .id(5L)
                .partnerId(2L)
                .locationFrom(1)
                .locationTo(20482)
                .type(ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR)
                .platformClientId(1L)
                .day(LocalDate.of(2019, 1, 1))
                .value(2L)
                .build()
        ));
        updater.updatePartnerCapacities();
        verify(lmsClient).getPartnerCapacities();
        verify(lmsClient).createPartnerCapacityDayOff(5L, LocalDate.of(2019, 1, 1));
        verifyNoMoreInteractions(lmsClient);

        List<PartnerCapacity> partnerCapacities = partnerCapacityRepository.findAll();
        softly.assertThat(partnerCapacities).extracting(PartnerCapacity::getCapacityId)
            .hasSize(1)
            .containsOnly(5L);
        List<CapacityCounter> capacityCounters = counterRepository.findAll();
        softly.assertThat(capacityCounters)
            .hasSize(1)
            .extracting(CapacityCounter::getParcelCount)
            .containsOnly(2L);
        softly.assertThat(capacityCounters)
            .extracting(CapacityCounter::getMaxAllowedParcelCount)
            .containsOnly(2L);
        softly.assertThat(capacityCounters)
            .extracting(CapacityCounter::getIsDayOffCreated)
            .containsOnly(true);
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/capacity-counters_2_item.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testOneDayOffCreatedCancelledOrdersNotProcessedUsingCountingTypeItem() {
        when(lmsClient.getPartnerCapacities()).thenReturn(Collections.singletonList(
            PartnerCapacityDto.newBuilder()
                .id(5L)
                .partnerId(2L)
                .locationFrom(1)
                .locationTo(20482)
                .type(ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR)
                .platformClientId(1L)
                .day(LocalDate.of(2019, 1, 1))
                .value(36L)
                .countingType(CountingType.ITEM)
                .build()
        ));
        updater.updatePartnerCapacities();
        verify(lmsClient).getPartnerCapacities();
        verify(lmsClient).createPartnerCapacityDayOff(5L, LocalDate.of(2019, 1, 1));
        verifyNoMoreInteractions(lmsClient);

        List<PartnerCapacity> partnerCapacities = partnerCapacityRepository.findAll();
        softly.assertThat(partnerCapacities).extracting(PartnerCapacity::getCapacityId)
            .hasSize(1)
            .containsOnly(5L);
        List<DeletedPartnerCapacity> deletedPartnerCapacities = deletedPartnerCapacityRepository.findAll();
        softly.assertThat(deletedPartnerCapacities).extracting(DeletedPartnerCapacity::getEntityId)
            .hasSize(4)
            .containsOnly(1L, 2L, 3L, 4L);
        List<CapacityCounter> capacityCounters = counterRepository.findAll();
        softly.assertThat(capacityCounters)
            .hasSize(1)
            .extracting(CapacityCounter::getParcelCount)
            .containsOnly(36L);
        softly.assertThat(capacityCounters)
            .extracting(CapacityCounter::getMaxAllowedParcelCount)
            .containsOnly(36L);
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/capacity-counters_1.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testReserveCreated() {
        PartnerCapacityDto dto = PartnerCapacityDto.newBuilder()
            .id(5L)
            .partnerId(2L)
            .locationFrom(1)
            .locationTo(20482)
            .type(ru.yandex.market.logistics.management.entity.type.CapacityType.RESERVE)
            .platformClientId(1L)
            .day(LocalDate.of(2019, 1, 1))
            .value(2L)
            .build();

        List<PartnerCapacityDto> dtos = generateEqualsLists(capacitiesForCounters()).getSecond();

        dtos.add(dto);
        when(lmsClient.getPartnerCapacities()).thenReturn(dtos);

        updater.updatePartnerCapacities();
        verify(lmsClient).getPartnerCapacities();
        verify(lmsClient).createPartnerCapacityDayOff(5L, LocalDate.of(2019, 1, 1));
        verifyNoMoreInteractions(lmsClient);

        List<PartnerCapacity> partnerCapacities = partnerCapacityRepository.findAll();
        softly.assertThat(partnerCapacities).extracting(PartnerCapacity::getCapacityId)
            .hasSize(5)
            .containsOnly(1L, 2L, 3L, 4L, 5L);
        List<DeletedPartnerCapacity> deletedPartnerCapacities = deletedPartnerCapacityRepository.findAll();
        softly.assertThat(deletedPartnerCapacities).hasSize(0);

        List<CapacityCounter> counters = counterRepository.findAll();
        softly.assertThat(counters)
            .extracting(cc -> cc.getPartnerCapacity().getCapacityId(), CapacityCounter::getParcelCount)
            .hasSize(1)
            .containsOnly(tuple(5L, 2L));
        softly.assertThat(counters)
            .extracting(CapacityCounter::getMaxAllowedParcelCount)
            .containsOnly(2L);
        softly.assertThat(counters)
            .filteredOn(c -> c.getPartnerCapacity().getCapacityId().equals(5L))
            .extracting(CapacityCounter::getIsDayOffCreated)
            .containsOnly(true);
        softly.assertThat(counters)
            .filteredOn(c -> !c.getPartnerCapacity().getCapacityId().equals(5L))
            .extracting(CapacityCounter::getIsDayOffCreated)
            .doesNotContain(true);
    }

    @Test
    @Sql(scripts = "/data/repository/partnerCapacity/capacity-counters_1.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testReserveCreatedOthersProperlyIncremented() {
        PartnerCapacityDto dto = PartnerCapacityDto.newBuilder()
            .id(5L)
            .partnerId(2L)
            .locationFrom(1)
            .locationTo(20482)
            .type(ru.yandex.market.logistics.management.entity.type.CapacityType.RESERVE)
            .platformClientId(1L)
            .day(LocalDate.of(2019, 1, 1))
            .value(1L)
            .build();
        List<PartnerCapacityDto> dtos = generateEqualsLists(capacitiesForCounters()).getSecond();
        dtos.add(dto);
        when(lmsClient.getPartnerCapacities()).thenReturn(dtos);

        updater.updatePartnerCapacities();
        verify(lmsClient).getPartnerCapacities();
        verify(lmsClient).createPartnerCapacityDayOff(5L, LocalDate.of(2019, 1, 1));
        verifyNoMoreInteractions(lmsClient);

        List<PartnerCapacity> partnerCapacities = partnerCapacityRepository.findAll();
        softly.assertThat(partnerCapacities).extracting(PartnerCapacity::getCapacityId)
            .hasSize(5)
            .containsOnly(1L, 2L, 3L, 4L, 5L);
        List<DeletedPartnerCapacity> deletedPartnerCapacities = deletedPartnerCapacityRepository.findAll();
        softly.assertThat(deletedPartnerCapacities).hasSize(0);

        List<CapacityCounter> counters = counterRepository.findAll();
        softly.assertThat(counters)
            .extracting(
                cc -> cc.getPartnerCapacity().getCapacityId(),
                CapacityCounter::getParcelCount,
                CapacityCounter::getIsDayOffCreated
            )
            .hasSize(5)
            .containsExactlyInAnyOrder(
                tuple(5L, 1L, true),
                tuple(1L, 1L, false),
                tuple(2L, 1L, false),
                tuple(3L, 1L, false),
                tuple(4L, 1L, false)
            );
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner-capacity-cancel.sql")
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testReservedCapacityCounterDecreased() {
        Mockito.when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());
        OrderToShip cancelled = createOrder("3", 133L, OrderToShipStatus.CANCELLED,
            117065L, LocalDate.of(2019, 6, 20), CapacityServiceType.DELIVERY
        );
        orderToShipRepository.save(cancelled);
        processNewEventsSync();
        verify(lmsClient, times(0)).deletePartnerCapacityDayOff(anyLong(), any(LocalDate.class));
        verifyNoMoreInteractions(lmsClient);
        List<OrderToShip> orders = orderToShipRepository.findAll();
        softly.assertThat(orders).hasSize(5)
            .extracting(OrderToShip::getProcessed)
            .containsOnly(true);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner-capacity-cancel.sql")
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testRegularCapacityCounterDecreased() {
        Mockito.when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());
        OrderToShip cancelled1 = createOrder("1", 133L, OrderToShipStatus.CANCELLED, 117065L,
            LocalDate.of(2019, 6, 20), CapacityServiceType.DELIVERY
        );
        OrderToShip cancelled2 = createOrder("1", 145L, OrderToShipStatus.CANCELLED, 216L,
            LocalDate.of(2019, 6, 20), CapacityServiceType.SHIPMENT
        );
        orderToShipRepository.saveAll(Arrays.asList(
            cancelled1, cancelled2
        ));

        processNewEventsSync();

        List<Long> updatedCapacitiesIds = Arrays.asList(1L, 2L, 5L, 7L);

        List<CapacityCounter> counters = counterRepository.findAll().stream()
            .filter(counter -> updatedCapacitiesIds.contains(counter.getPartnerCapacity().getCapacityId()))
            .collect(Collectors.toList());

        softly.assertThat(counters).extracting(CapacityCounter::getParcelCount)
            .containsExactlyInAnyOrder(1L, 1L, 2L, 9L);

        List<OrderToShip> orders = orderToShipRepository.findAll();
        softly.assertThat(orders).hasSize(6)
            .extracting(OrderToShip::getProcessed)
            .containsOnly(true);

        verify(lmsClient).deletePartnerCapacityDayOff(7L, LocalDate.parse("2019-06-20"));
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner-capacity-cancel-item.sql")
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testRegularCapacityCounterDecreasedWithCountingTypeItem() {
        Mockito.when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());

        transactionTemplate.execute(status -> {
            List<OrderToShip> orderToShips = orderToShipRepository.findAll();

            OrderToShip firstCreated = orderToShips.stream()
                .filter(orderToShip -> orderToShip.getId().equals(new OrderToShipId()
                    .setId("1")
                    .setPartnerId(133L)
                    .setPlatformClientId(1L)
                    .setStatus(OrderToShipStatus.CREATED)))
                .findFirst()
                .get();

            OrderToShip secondCreated = orderToShips.stream()
                .filter(orderToShip -> orderToShip.getId().equals(new OrderToShipId()
                    .setId("1")
                    .setPartnerId(145L)
                    .setPlatformClientId(1L)
                    .setStatus(OrderToShipStatus.CREATED)))
                .findFirst()
                .get();

            OrderToShip firstCancelled = OrderToShip.getByOldWithValues(firstCreated, OrderToShipStatus.CANCELLED);
            OrderToShip secondCancelled = OrderToShip.getByOldWithValues(secondCreated, OrderToShipStatus.CANCELLED);

            return orderToShipRepository.saveAll(Arrays.asList(firstCancelled, secondCancelled));
        });

        processNewEventsSync();

        List<Long> updatedCapacitiesIds = Arrays.asList(1L, 2L, 5L, 7L);

        List<CapacityCounter> counters = counterRepository.findAll().stream()
            .filter(counter -> updatedCapacitiesIds.contains(counter.getPartnerCapacity().getCapacityId()))
            .collect(Collectors.toList());

        softly.assertThat(counters).extracting(CapacityCounter::getParcelCount)
            .containsExactlyInAnyOrder(33L, 3L, 15L, 0L);

        List<OrderToShip> orders = orderToShipRepository.findAll();
        softly.assertThat(orders).hasSize(6)
            .extracting(OrderToShip::getProcessed)
            .containsOnly(true);

        verify(lmsClient).deletePartnerCapacityDayOff(7L, LocalDate.parse("2019-06-20"));
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner-capacity-cancel-mixed.sql")
    @Sql(scripts = "/data/repository/partnerCapacity/truncate-capacity.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testRegularCapacityCounterDecreasedWithMixedCountingTypes() {
        Mockito.when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());

        transactionTemplate.execute(status -> {
            List<OrderToShip> orderToShips = orderToShipRepository.findAll();

            OrderToShip firstCreated = orderToShips.stream()
                .filter(orderToShip -> orderToShip.getId().equals(new OrderToShipId()
                    .setId("1")
                    .setPartnerId(133L)
                    .setPlatformClientId(1L)
                    .setStatus(OrderToShipStatus.CREATED)))
                .findFirst()
                .get();

            OrderToShip secondCreated = orderToShips.stream()
                .filter(orderToShip -> orderToShip.getId().equals(new OrderToShipId()
                    .setId("1")
                    .setPartnerId(145L)
                    .setPlatformClientId(1L)
                    .setStatus(OrderToShipStatus.CREATED)))
                .findFirst()
                .get();

            OrderToShip firstCancelled = OrderToShip.getByOldWithValues(firstCreated, OrderToShipStatus.CANCELLED);
            OrderToShip secondCancelled = OrderToShip.getByOldWithValues(secondCreated, OrderToShipStatus.CANCELLED);

            return orderToShipRepository.saveAll(Arrays.asList(firstCancelled, secondCancelled));
        });

        processNewEventsSync();

        List<Long> updatedCapacitiesIds = Arrays.asList(1L, 2L, 5L, 7L);

        List<CapacityCounter> counters = counterRepository.findAll().stream()
            .filter(counter -> updatedCapacitiesIds.contains(counter.getPartnerCapacity().getCapacityId()))
            .collect(Collectors.toList());

        softly.assertThat(counters).extracting(CapacityCounter::getParcelCount)
            .containsExactlyInAnyOrder(33L, 12L, 13L, 0L);

        List<OrderToShip> orders = orderToShipRepository.findAll();
        softly.assertThat(orders).hasSize(6)
            .extracting(OrderToShip::getProcessed)
            .containsOnly(true);

        verify(lmsClient).deletePartnerCapacityDayOff(7L, LocalDate.parse("2019-06-20"));
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @Sql(value = "/data/repository/partnerCapacity/partner-capacity.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testCapacityCounterIncreasedFF() {
        when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());
        OrderToShip created = createOrder("193630", 133L, OrderToShipStatus.CREATED, 213L,
            LocalDate.of(2019, 2, 20), CapacityServiceType.DELIVERY
        );
        orderToShipRepository.save(created);

        processNewEventsSync();

        verify(lmsClient).createPartnerCapacityDayOff(1L, LocalDate.parse("2019-02-20"));
        verify(lmsClient, times(2)).getPartner(133L);
        verifyNoMoreInteractions(lmsClient);

        List<CapacityCounter> all = counterRepository.findAll();
        softly.assertThat(all).extracting(CapacityCounter::getParcelCount)
            .hasSize(2)
            .containsOnly(1L);
    }

    @Test
    @Sql(value = "/data/repository/partnerCapacity/partner-capacity.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testCapacityCounterIncreasedAndDecreased() {
        when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());
        OrderToShip created = createOrder("193630", 133L, OrderToShipStatus.CREATED, 213L,
            LocalDate.of(2019, 2, 20), CapacityServiceType.DELIVERY
        );
        OrderToShip cancelled = createOrder("193630", 133L, OrderToShipStatus.CANCELLED, 213L,
            LocalDate.of(2019, 2, 20), CapacityServiceType.DELIVERY
        );
        orderToShipRepository.saveAll(Arrays.asList(created, cancelled));

        processNewEventsSync();

        verifyNoMoreInteractions(lmsClient);
        List<CapacityCounter> all = counterRepository.findAll();
        softly.assertThat(all).extracting(CapacityCounter::getParcelCount)
            .isEmpty();
    }

    @Test
    @Sql(value = "/data/repository/partnerCapacity/partner-capacity.sql", executionPhase =
        Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testIncreaseInboundCapacity() {
        OrderToShip crossdock = createOrder("3", 47778L, OrderToShipStatus.CREATED,
            1L, LocalDate.of(2019, 6, 18), CapacityServiceType.SHIPMENT
        );
        OrderToShip indound = createOrder("3", 105L, OrderToShipStatus.CREATED,
            1L, LocalDate.of(2019, 6, 20), CapacityServiceType.INBOUND
        );
        OrderToShip shipment = createOrder("3", 105L, OrderToShipStatus.CREATED,
            213L, LocalDate.of(2019, 6, 20), CapacityServiceType.SHIPMENT
        );
        OrderToShip delivery = createOrder("3", 133L, OrderToShipStatus.CREATED,
            213L, LocalDate.of(2019, 6, 20), CapacityServiceType.DELIVERY
        );

        orderToShipRepository.saveAll(Arrays.asList(crossdock, indound, shipment, delivery));

        processNewEventsSync();

        verify(lmsClient).createPartnerCapacityDayOff(1L, LocalDate.parse("2019-06-20"));
        verify(lmsClient).createPartnerCapacityDayOff(3L, LocalDate.parse("2019-06-20"));
        verify(lmsClient).createPartnerCapacityDayOff(5L, LocalDate.parse("2019-06-20"));
        verify(lmsClient).createPartnerCapacityDayOff(6L, LocalDate.parse("2019-06-18"));
        verify(lmsClient, times(2)).getPartner(133L);
        verify(lmsClient, times(4)).getPartner(105L);
        verify(lmsClient, times(2)).getPartner(47778L);
        verifyNoMoreInteractions(lmsClient);

        List<CapacityCounter> counters = counterRepository.findAll();
        softly.assertThat(counters).extracting(CapacityCounter::getParcelCount)
            .hasSize(6)
            .containsOnly(1L);

        softly.assertThat(counters).extracting(CapacityCounter::getPartnerCapacity)
            .extracting(PartnerCapacity::getCapacityId)
            .containsOnly(1L, 2L, 3L, 4L, 5L, 6L);

        List<OrderToShip> ships = orderToShipRepository.findAll();
        softly.assertThat(ships)
            .extracting(OrderToShip::getProcessed)
            .containsOnly(true);
    }

    private void processNewEventsSync() {
        transactionTemplate.execute(tx -> {
            orderToShipRepository
                .findAllNotProcessedWithOnlyOneStatus(LocalDate.now(clock))
                .stream()
                .map(OrderToShipDto::of)
                .forEach(capacityCountersUpdater::update);
            return null;
        });
    }

    // TODO: DELIVERY-44112
    @Ignore("Падает по неизвестной причине в транке")
    @Test
    @Sql("/data/repository/partnerCapacity/capacity_for_concurrency_with_orders.sql")
    @Sql(value = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testIncrementWhileUpdating() {
        when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());

        when(lmsClient.getPartnerCapacities()).thenReturn(
            Collections.singletonList(
                PartnerCapacityDto.newBuilder()
                    .id(1L)
                    .partnerId(145L)
                    .locationFrom(1)
                    .locationTo(213)
                    .type(ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR)
                    .platformClientId(1L)
                    .value(4L)
                    .build()
            ));

        AtomicInteger invocationsCounter = new AtomicInteger(0);
        doAnswer(invocation -> {
                invocationsCounter.getAndIncrement();
                await().atMost(1, TimeUnit.SECONDS).until(() -> invocationsCounter.get() == 2);
                return null;
            }
        ).when(lmsClient).getPartnerCapacities();

        doAnswer(invocation -> {
                invocationsCounter.getAndIncrement();
                invocation.callRealMethod();
                invocationsCounter.getAndIncrement();
                return null;
            }
        ).when(capacityCountersUpdater).update(any());

        new Thread(() -> updater.updatePartnerCapacities()).start();
        await().until(() -> invocationsCounter.get() == 1);

        new Thread(this::processNewEventsSync).start();
        await().atMost(1, TimeUnit.SECONDS).until(() -> invocationsCounter.get() == 3);

        verify(lmsClient, times(1)).getPartnerCapacities();
        verify(lmsClient, times(2)).getPartner(145L);
        verifyNoMoreInteractions(lmsClient);

        List<PartnerCapacity> capacities = partnerCapacityRepository.findAll();
        softly.assertThat(capacities).hasSize(2);

        List<CapacityCounter> counters = counterRepository.findAll();
        softly.assertThat(counters)
            .hasSize(2)
            .extracting(CapacityCounter::getParcelCount)
            .containsOnly(4L);
        softly.assertThat(counters)
            .extracting(CapacityCounter::getIsDayOffCreated)
            .containsOnly(true);

        List<OrderToShip> orders = orderToShipRepository.findAll();
        softly.assertThat(orders).hasSize(6).extracting(OrderToShip::getProcessed)
            .containsOnly(true);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/capacity_for_concurrency_with_orders_1.sql")
    @Sql(value = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testUpdateWhileIncrementing() {
        when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());

        when(lmsClient.getPartnerCapacities()).thenReturn(
            Collections.singletonList(
                PartnerCapacityDto.newBuilder()
                    .id(1L)
                    .partnerId(145L)
                    .locationFrom(1)
                    .locationTo(213)
                    .type(ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR)
                    .platformClientId(1L)
                    .value(4L)
                    .build()
            )
        );

        AtomicInteger invocationsCounter = new AtomicInteger(0);

        doAnswer(invocation -> {
                invocationsCounter.getAndIncrement();
                await().atMost(1, TimeUnit.SECONDS).until(() -> invocationsCounter.get() == 2);
                return null;
            }
        ).when(lmsClient).createPartnerCapacityDayOff(1L, LocalDate.of(2019, 1, 1));

        doAnswer(invocation -> {
                invocationsCounter.getAndIncrement();
                invocation.callRealMethod();
                invocationsCounter.getAndIncrement();
                return null;
            }
        ).when(updater).updatePartnerCapacities();

        new Thread(this::processNewEventsSync).start();
        await().until(() -> invocationsCounter.get() == 1);

        new Thread(() -> updater.updatePartnerCapacities()).start();
        await().atMost(1, TimeUnit.SECONDS).until(() -> invocationsCounter.get() == 3);

        verify(lmsClient, times(1)).getPartnerCapacities();
        verify(lmsClient, atLeastOnce()).createPartnerCapacityDayOff(1L, LocalDate.of(2019, 1, 1));
        verify(lmsClient, times(1)).createPartnerCapacityDayOff(2L, LocalDate.of(2019, 1, 1));
        verify(lmsClient, atMost(2)).deletePartnerCapacityDayOff(anyLong(), any(LocalDate.class));
        verifyNoMoreInteractions(lmsClient);

        List<PartnerCapacity> capacities = partnerCapacityRepository.findAll();
        softly.assertThat(capacities).hasSize(2);

        List<CapacityCounter> counters = counterRepository.findAll();
        softly.assertThat(counters).hasSize(2).extracting(CapacityCounter::getParcelCount)
            .containsOnly(4L);

        List<OrderToShip> orders = orderToShipRepository.findAll();
        softly.assertThat(orders).hasSize(4).extracting(OrderToShip::getProcessed)
            .containsOnly(true);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/capacity-for-monitoring.sql")
    @Sql(value = "/data/repository/partnerCapacity/truncate-capacity.sql", executionPhase =
        Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void testCapacityMaxForMonitoringAfetrUpdate() {
        when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());

        // capacity 1 removed
        // capacity 2 value changed
        // capacity 3 added
        when(lmsClient.getPartnerCapacities()).thenReturn(
            List.of(
                PartnerCapacityDto.newBuilder()
                    .id(2L)
                    .partnerId(2L)
                    .locationFrom(1)
                    .locationTo(20279)
                    .type(ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR)
                    .platformClientId(1L)
                    .value(2L)
                    .build(),
                PartnerCapacityDto.newBuilder()
                    .id(3L)
                    .partnerId(2L)
                    .locationFrom(1)
                    .locationTo(216)
                    .type(ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR)
                    .platformClientId(1L)
                    .value(2L)
                    .build()
            )
        );

        updater.updatePartnerCapacities();

        List<CapacityCounter> counters = counterRepository.findAll();
        softly.assertThat(counters)
            .filteredOn(x -> x.getPartnerCapacity().getCapacityId() == 2L)
            .hasSize(3)
            .extracting(CapacityCounter::getDay, CapacityCounter::getMaxAllowedParcelCount)
            .containsExactlyInAnyOrder(
                // old: count=2, capacity = 3 : green
                // new: count=2, capacity = 2 : green
                tuple(LocalDate.of(2019, 1, 1), 2L),
                // old: count=3, capacity = 3 : green
                // new: count=3, capacity = 2 : monitoring shold stay green, set value_for_monitoring to 3
                tuple(LocalDate.of(2019, 1, 2), 3L),
                // old: count=4, capacity = 3 : red
                // new: count=4, capacity = 2 : red
                tuple(LocalDate.of(2019, 1, 3), 2L)
            );
        softly.assertThat(counters)
            .filteredOn(x -> x.getPartnerCapacity().getCapacityId() == 3L)
            .hasSize(1)
            .extracting(CapacityCounter::getDay, CapacityCounter::getMaxAllowedParcelCount)
            .containsExactlyInAnyOrder(
                // capacity 3
                // old: count 3, capacity - not existing
                // new: count 3, capacuty=2, monitoting value = 3 (should be green)
                tuple(LocalDate.of(2019, 1, 1), 3L)
            );
    }

    private PartnerCapacity[] capacitiesForCounters() {
        return new PartnerCapacity[]{
            getCapacity(1, 2, 1, 117065, null, 1, 2, CapacityType.REGULAR, null),
            getCapacity(2, 2, 1, 213, null, 1, 2, CapacityType.REGULAR, null),
            getCapacity(3, 2, 1, 1, null, 1, 2, CapacityType.REGULAR, null),
            getCapacity(4, 2, 1, 225, null, 1, 2, CapacityType.REGULAR, null)
        };
    }

    private PartnerCapacity[] getNewIntersection() {
        return new PartnerCapacity[]{
            getCapacity(1L, 1L, 1L, 1L, DeliveryType.DELIVERY,
                PlatformClient.BERU.getId(), 1001L, CapacityType.REGULAR,
                CapacityCountingType.ORDER, null
            ),
            getCapacity(2L, 1L, 1L, 1L, DeliveryType.DELIVERY,
                PlatformClient.YANDEX_MARKET.getId(), 1001L, CapacityType.REGULAR, CapacityCountingType.ORDER, null
            ),
            getCapacity(3L, 222L, 222L, 222L, DeliveryType.POST,
                222L, 2222L, CapacityType.REGULAR, CapacityCountingType.ORDER, LocalDate.of(2020, 6, 13)
            ),
            getCapacity(4L, 3L, 3L, 3L, DeliveryType.PICKUP,
                PlatformClient.YANDEX_DELIVERY.getId(), 3001L, CapacityType.RESERVE, CapacityCountingType.ITEM, null
            )
        };
    }

    private PartnerCapacity[] getOne() {
        return new PartnerCapacity[]{
            getCapacity(2L, 2L, 2L, 2L, DeliveryType.POST,
                222L, 2000L, CapacityType.REGULAR, CapacityCountingType.ITEM, LocalDate.of(2020, 6, 13)
            )
        };
    }

    private PartnerCapacity[] getNewAndOld() {
        return new PartnerCapacity[]{
            getCapacity(1L, 1L, 1L, 1L, DeliveryType.DELIVERY,
                1L, 1000L, CapacityType.REGULAR, CapacityCountingType.ORDER, null
            ),
            getCapacity(3L, 3L, 3L, 3L, DeliveryType.POST,
                1L, 1000L, CapacityType.REGULAR, CapacityCountingType.ITEM, null
            ),
            getCapacity(4L, 4L, 4L, 4L, null,
                4L, 4000L, CapacityType.REGULAR, CapacityCountingType.ITEM, null
            ),
        };
    }

    private PartnerCapacity[] getNewSamePartner() {
        return new PartnerCapacity[]{
            getCapacity(10L, 1L, 1L, 1L, DeliveryType.DELIVERY,
                1L, 1000L, CapacityType.REGULAR, CapacityCountingType.ITEM, null
            ),
            getCapacity(30L, 3L, 3L, 3L, DeliveryType.POST,
                1L, 1000L, CapacityType.REGULAR, CapacityCountingType.ORDER, null
            )
        };
    }

    private PartnerCapacity[] getSame() {
        return new PartnerCapacity[]{
            getCapacity(1, 1, 1, 1, DeliveryType.DELIVERY,
                1, 1000, CapacityType.REGULAR, CapacityCountingType.ITEM, null
            ),
            getCapacity(2, 2, 2, 2, DeliveryType.PICKUP,
                2, 2000, CapacityType.RESERVE, CapacityCountingType.ORDER, LocalDate.of(2019, 6, 13)
            ),
            getCapacity(3, 3, 3, 3, DeliveryType.POST,
                3, 3000, CapacityType.REGULAR, CapacityCountingType.ORDER, LocalDate.of(2020, 1, 1)
            ),
        };
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private PartnerCapacity getCapacity(
        long id,
        long partnerId,
        long locationFromId,
        long locationToId,
        DeliveryType delType,
        long platformId,
        long value,
        CapacityType type,
        LocalDate day
    ) {
        return getCapacity(id, partnerId, locationFromId, locationToId, delType, platformId, value, type, null, day);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private PartnerCapacity getCapacity(
        long id,
        long partnerId,
        long locationFromId,
        long locationToId,
        DeliveryType delType,
        long platformId,
        long value,
        CapacityType type,
        CapacityCountingType countingType,
        LocalDate day
    ) {
        return new PartnerCapacity()
            .setCapacityId(id)
            .setPartnerId(partnerId)
            .setLocationFromId(locationFromId)
            .setLocationToId(locationToId)
            .setDeliveryType(delType)
            .setPlatformClientId(platformId)
            .setValue(value)
            .setCapacityType(type)
            .setCountingType(countingType)
            .setDay(day);
    }

    private Pair<List<PartnerCapacity>, List<PartnerCapacityDto>> generateEqualsLists(PartnerCapacity... capacities) {
        List<PartnerCapacityDto> dtos = Stream.of(capacities)
            .map(capacity -> PartnerCapacityDto.newBuilder()
                .id(capacity.getCapacityId())
                .partnerId(capacity.getPartnerId())
                .locationFrom(NumberUtils.convertAnyNumberToIntegerNullSafely(capacity.getLocationFromId())
                    .orElse(null))
                .locationTo(NumberUtils.convertAnyNumberToIntegerNullSafely(capacity.getLocationToId()).orElse(null))
                .deliveryType(convert(capacity.getDeliveryType()))
                .type(convert(capacity.getCapacityType()))
                .countingType(Optional.ofNullable(capacity.getCountingType())
                    .map(CapacityCountingType::name)
                    .map(CountingType::valueOf)
                    .orElse(CountingType.ORDER))
                .platformClientId(capacity.getPlatformClientId())
                .day(capacity.getDay())
                .value(capacity.getValue())
                .build()
            )
            .collect(Collectors.toList());

        return Pair.of(Arrays.asList(capacities), dtos);
    }

    private ru.yandex.market.logistics.management.entity.type.DeliveryType convert(DeliveryType type) {
        if (Objects.isNull(type)) {
            return null;
        }
        switch (type) {
            case PICKUP:
                return ru.yandex.market.logistics.management.entity.type.DeliveryType.PICKUP;
            case POST:
                return ru.yandex.market.logistics.management.entity.type.DeliveryType.POST;
            case DELIVERY:
                return ru.yandex.market.logistics.management.entity.type.DeliveryType.COURIER;
            default:
                return null;
        }
    }

    private ru.yandex.market.logistics.management.entity.type.CapacityType convert(CapacityType type) {
        if (type == CapacityType.RESERVE) {
            return ru.yandex.market.logistics.management.entity.type.CapacityType.RESERVE;
        }
        return ru.yandex.market.logistics.management.entity.type.CapacityType.REGULAR;
    }

    private OrderToShip createOrder(
        String parcelId,
        Long partnerId,
        OrderToShipStatus status,
        Long locationTo,
        LocalDate shipmentDay,
        CapacityServiceType capacityServiceType
    ) {
        return new OrderToShip()
            .setId(new OrderToShipId()
                .setId(parcelId)
                .setPartnerId(partnerId)
                .setPlatformClientId(1L)
                .setServiceType(capacityServiceType)
                .setStatus(status)
            )
            .addOrderToShipValue(new OrderToShipValue()
                .setCountingType(CapacityCountingType.ORDER)
                .setValue(1L))
            .addOrderToShipValue(new OrderToShipValue()
                .setCountingType(CapacityCountingType.ITEM)
                .setValue(1L))
            .setProcessed(false)
            .setDeliveryType(DeliveryType.POST)
            .setLocationFromId(1L)
            .setLocationToId(locationTo)
            .setShipmentDay(shipmentDay);
    }
}
