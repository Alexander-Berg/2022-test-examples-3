package ru.yandex.market.delivery.mdbapp.integration.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.queue.order.to.ship.dto.OrderToShipDto;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.CapacityService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.CapacityCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShipValue;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.PartnerCapacity;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.CapacityCountingType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.CapacityType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus;
import ru.yandex.market.delivery.mdbapp.integration.converter.OrderToShipConverter;
import ru.yandex.market.delivery.mdbapp.util.GeoTestUtils;
import ru.yandex.market.delivery.mdbclient.model.dto.CapacityServiceType;
import ru.yandex.market.delivery.mdbclient.model.dto.DeliveryType;

public class CapacityServiceTest {

    private static final long PARTNER_ID = 1000L;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private static GeoInfo geoInfo = GeoTestUtils.prepareGeoInfo();

    private CapacityService capacityService;

    @Before
    public void setUp() {
        capacityService = new CapacityService(geoInfo);

    }

    @Test
    public void testCapacitiesTreeBuildInProperOrder() {
        List<PartnerCapacity> capacities = getPartnerCapacities();

        List<PartnerCapacity> partnerCapacities = capacityService.buildCapacityTree(1L, 20482L, capacities);
        softly.assertThat(partnerCapacities).extracting(PartnerCapacity::getCapacityId)
            .containsExactly(5L, 6L, 3L, 4L, 7L, 10L, 1L, 11L, 2L)
            .doesNotContain(8L, 9L);
    }

    @Test
    public void testCapacitiesSortedInPorperOrder() {
        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacity(1L, CapacityType.REGULAR, null, 1L, 20279L)
                .setDay(LocalDate.of(2019, 1, 1)),
            getCapacity(2L, CapacityType.REGULAR, DeliveryType.DELIVERY, 1L, 20279L)
                .setDay(LocalDate.of(2019, 1, 1)),
            getCapacity(3L, CapacityType.REGULAR, DeliveryType.POST, 1L, 20279L),
            getCapacity(4L, CapacityType.REGULAR, null, 1L, 20279L)
        );

        List<PartnerCapacity> partnerCapacities = capacityService.buildCapacityTree(1L, 20482L, capacities);
        softly.assertThat(partnerCapacities).extracting(PartnerCapacity::getCapacityId)
            .containsExactly(2L, 1L, 3L, 4L);
    }

    @Test
    public void testCapacityCountersOneReserved() {
        List<PartnerCapacity> capacities = getPartnerCapacities();

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(1L)
            .setLocationToId(20482L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(7L);
    }

    @Test
    public void testCapacityCountersTwoReserved() {
        List<PartnerCapacity> capacities = getPartnerCapacities();
        capacities.add(getCapacity(10L, CapacityType.RESERVE, null, 1L, 117065L));

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(1L)
            .setLocationToId(20482L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(10L);
    }

    @Test
    public void testCapacityCountersNoReserved() {
        List<PartnerCapacity> capacities = getPartnerCapacities().stream()
            .filter(c -> !c.getCapacityType().equals(CapacityType.RESERVE))
            .collect(Collectors.toList());

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(1L)
            .setLocationToId(20482L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(6L, 4L, 1L, 11L, 2L);
    }

    @Test
    public void testCapacityCountersNotFoundByType() {
        List<PartnerCapacity> capacities = getPartnerCapacities();

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123");
        orderToShip.getId().setServiceType(CapacityServiceType.SHIPMENT);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).isEmpty();
    }

    @Test
    public void testCapacityCountersFilteredByType() {
        List<CapacityServiceType> serviceTypes = Stream.of(CapacityServiceType.values())
            .filter(capacityServiceType -> capacityServiceType != CapacityServiceType.UNKNOWN)
            .collect(Collectors.toList());

        AtomicInteger counter = new AtomicInteger();
        List<PartnerCapacity> capacities = getPartnerCapacities().stream()
            .filter(partnerCapacity -> partnerCapacity.getCapacityType() != CapacityType.RESERVE)
            .peek(partnerCapacity -> {
                partnerCapacity.setDeliveryType(null);
                partnerCapacity.setLocationFromId(225L);
                partnerCapacity.setLocationToId(225L);

                partnerCapacity.getCapacityCounters().clear();
                partnerCapacity.addCapacityCounter(
                    new CapacityCounter()
                        .setParcelCount(10L)
                        .setDay(LocalDate.of(2019, 1, 1)));

            })
            .collect(Collectors.groupingBy(it ->
                counter.getAndIncrement() % serviceTypes.size()))
            .entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(partnerCapacity -> partnerCapacity.setServiceType(serviceTypes.get(entry.getKey()))))
            .collect(Collectors.toList());

        for (CapacityServiceType serviceType : serviceTypes) {
            OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123");
            orderToShip.getId().setServiceType(serviceType);

            List<CapacityCounter> countersForDecrement = capacityService
                .getCapacityCountersForDecrement(orderToShip, capacities);

            softly.assertThat(countersForDecrement).hasSizeGreaterThan(0)
                .allMatch(capacityCounter ->
                    capacityCounter.getPartnerCapacity().getServiceType() == serviceType);

            List<CapacityCounter> countersForIncrement = capacityService
                .getCapacityCountersForIncrement(orderToShip, capacities);

            softly.assertThat(countersForIncrement).hasSizeGreaterThan(0)
                .allMatch(capacityCounter ->
                    capacityCounter.getPartnerCapacity().getServiceType() == serviceType);
        }
    }

    @Test
    public void testChangeCounters() {
        List<PartnerCapacity> capacities = getPartnerCapacities().stream()
            .filter(c -> !c.getCapacityType().equals(CapacityType.RESERVE))
            .collect(Collectors.toList());

        OrderToShipDto orderToShip = prepareOrderToShipWithValues();

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            capacities
        );

        List<CapacityCounter> capacityCountersChanged =
            capacityService.incrementCounters(capacityCounters, orderToShip);
        softly.assertThat(capacityCountersChanged).extracting(CapacityCounter::getParcelCount)
            .hasSize(5)
            .containsExactlyInAnyOrder(1L, 1L, 1L, 3L, 1L);
    }

    @Test
    public void testChangeCountersWithoutExplicitOrderToShipValues() {
        List<PartnerCapacity> capacities = getPartnerCapacities().stream()
            .filter(c -> !c.getCapacityType().equals(CapacityType.RESERVE))
            .collect(Collectors.toList());

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(1L)
            .setLocationToId(20482L);

        List<CapacityCounter> capacityCounters =
            capacityService.getCapacityCountersForIncrement(orderToShip, capacities);

        List<CapacityCounter> capacityCountersChanged =
            capacityService.incrementCounters(capacityCounters, orderToShip);
        softly.assertThat(capacityCountersChanged).extracting(CapacityCounter::getParcelCount)
            .hasSize(5)
            .containsExactlyInAnyOrder(1L, 1L, 1L, 0L, 1L);
    }

    @Test
    public void testExceededReserveNotAppear() {
        List<PartnerCapacity> partnerCapacities = getPartnerCapacities();
        PartnerCapacity capacity = partnerCapacities.stream()
            .filter(c -> c.getCapacityType().equals(CapacityType.RESERVE))
            .findFirst().get();
        capacity.addCapacityCounter(new CapacityCounter()
            .setDay(LocalDate.of(2019, 1, 1))
            .setParcelCount(1L));

        OrderToShipDto orderToShip = prepareOrderToShipWithValues();

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            partnerCapacities
        );

        List<CapacityCounter> capacityCountersChanged =
            capacityService.incrementCounters(capacityCounters, orderToShip);
        softly.assertThat(capacityCountersChanged).extracting(CapacityCounter::getParcelCount)
            .hasSize(5)
            .containsExactlyInAnyOrder(1L, 1L, 1L, 3L, 1L);
    }

    @Test
    public void testReserveWithWrongDayNotAppear() {
        List<PartnerCapacity> partnerCapacities = getPartnerCapacities();
        PartnerCapacity capacity = partnerCapacities.stream()
            .filter(c -> c.getCapacityType().equals(CapacityType.RESERVE))
            .findFirst().get();
        capacity.setDay(LocalDate.of(2019, 1, 2));

        OrderToShipDto orderToShip = prepareOrderToShipWithValues();

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            partnerCapacities
        );

        List<CapacityCounter> capacityCountersChanged =
            capacityService.incrementCounters(capacityCounters, orderToShip);
        softly.assertThat(capacityCountersChanged).extracting(CapacityCounter::getParcelCount)
            .hasSize(5)
            .containsExactlyInAnyOrder(1L, 1L, 1L, 3L, 1L);
    }

    @Test
    public void testReserveWithProperDayAppear() {
        List<PartnerCapacity> partnerCapacities = getPartnerCapacities();
        PartnerCapacity capacity = partnerCapacities.stream()
            .filter(c -> c.getCapacityType().equals(CapacityType.RESERVE))
            .findFirst().get();
        capacity.setDay(LocalDate.of(2019, 1, 1));

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(1L)
            .setLocationToId(20482L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            partnerCapacities
        );

        List<CapacityCounter> capacityCountersChanged =
            capacityService.incrementCounters(capacityCounters, orderToShip);
        softly.assertThat(capacityCountersChanged).extracting(CapacityCounter::getParcelCount)
            .hasSize(1)
            .containsOnly(1L);
    }

    @Test
    public void testCapacityCountersForDecrementEmptyList() {
        List<PartnerCapacity> capacities = Collections.emptyList();
        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(3L)
            .setLocationToId(3L);
        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).hasSize(0);
    }

    @Test
    public void testCapacityCountersForDecrementBothRegular() {
        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 10, 15, 225, CapacityType.REGULAR),
            getCapacityWithCounter(2L, 11, 12, 3, CapacityType.REGULAR)
        );

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(3L)
            .setLocationToId(3L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(2L, 1L);
    }

    @Test
    public void testCapacityCountersForDecrementAllRegularAndOneWithCountingType() {
        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 10, 15, 225, CapacityType.REGULAR),
            getCapacityWithCounter(2L, 11, 12, 3, CapacityType.REGULAR),
            getCapacityWithCounter(3L, 11, 12, 3, CapacityType.REGULAR,
                CapacityServiceType.DELIVERY, CapacityCountingType.ITEM, 1
            )
        );

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(3L)
            .setLocationToId(3L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(2L, 3L, 1L);
    }

    @Test
    public void testOnlyReserveSelectedForDecrement() {
        List<PartnerCapacity> capacities = Arrays.asList(
            new PartnerCapacity().setCapacityId(1L)
                .setPartnerId(1000L)
                .setCapacityType(CapacityType.REGULAR)
                .setDeliveryType(DeliveryType.DELIVERY)
                .setLocationFromId(1L)
                .setLocationToId(225L)
                .setPlatformClientId(1L)
                .setValue(10L),
            getCapacityWithCounter(2L, 12, 12, 1, CapacityType.RESERVE)
        );
        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(1L)
            .setLocationToId(213L);
        List<CapacityCounter> counters = capacityService.getCapacityCountersForDecrement(orderToShip, capacities);
        softly.assertThat(counters).hasSize(1).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(2L);
    }

    @Test
    public void testCapacityCountersForDecrementFilledReserve() {
        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 10, 15, 225, CapacityType.REGULAR),
            getCapacityWithCounter(2L, 12, 12, 3, CapacityType.RESERVE)

        );

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(3L)
            .setLocationToId(3L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(1L);
    }

    @Test
    public void testCapacityCountersForDecrementZeroRegular() {
        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 0, 15, 225, CapacityType.REGULAR),
            getCapacityWithCounter(2L, 12, 12, 3, CapacityType.RESERVE)

        );

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(3L)
            .setLocationToId(3L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(2L);
    }

    @Test
    public void testCapacityCountersForDecrementOnlyRegular() {
        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 2, 15, 225, CapacityType.REGULAR),
            getCapacityWithCounter(2L, 12, 12, 3, CapacityType.RESERVE),
            getCapacityWithCounter(3L, 2, 12, 1, CapacityType.REGULAR)
        );

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(1L)
            .setLocationToId(1L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(3L, 1L);
    }

    @Test
    public void testCapacityCountersForDecrementTwoReserves() {
        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 3, 5, 225, CapacityType.RESERVE),
            getCapacityWithCounter(2L, 4, 5, 3, CapacityType.RESERVE)

        );

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(3L)
            .setLocationToId(3L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(2L);
    }

    @Test
    public void testCapacityCountersForDecrementTwoFilledReserves() {
        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 5, 5, 225, CapacityType.RESERVE),
            getCapacityWithCounter(2L, 5, 5, 3, CapacityType.RESERVE)

        );

        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(3L)
            .setLocationToId(3L);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(1L);
    }

    @Test
    public void testCapacityCountersForIncrementMiddleMile() {
        /*  example of real data
        +----------+-------------+-----------+-------------+-----+------------+
        |partner_id|location_from|location_to|delivery_type|value|service_type|
        +----------+-------------+-----------+-------------+-----+------------+
        |1005546   |120565       |225        |courier      |400  |delivery    |
        |1005546   |10747        |225        |courier      |200  |delivery    |
        |1005546   |117530       |225        |courier      |200  |delivery    |
        |1005546   |218621       |225        |courier      |200  |delivery    |
        |1005546   |10747        |225        |courier      |50   |delivery    |
        |1005546   |218621       |225        |courier      |40   |delivery    |
        |1005546   |117530       |225        |courier      |40   |delivery    |
        |1005546   |120565       |225        |courier      |120  |delivery    |
        |1005546   |1            |225        |courier      |1100 |delivery    |
        +----------+-------------+-----------+-------------+-----+------------+
         */

        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 0, 5, 120565, CapacityType.REGULAR, 1),
            getCapacityWithCounter(2L, 0, 5, 1, CapacityType.REGULAR, 1),
            getCapacityWithCounter(3L, 0, 5, 255, CapacityType.REGULAR, 1),
            getCapacityWithCounter(4L, 0, 5, 213, CapacityType.REGULAR, 1)
        );

        final Long moscowRegion = 1L;
        final Long moscow = 213L;
        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(moscowRegion)
            .setLocationToId(moscow);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForIncrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(2L);
    }

    @Test
    public void testCapacityCountersForDecrementMiddleMile() {
        /*  example of real data
        +----------+-------------+-----------+-------------+-----+------------+
        |partner_id|location_from|location_to|delivery_type|value|service_type|
        +----------+-------------+-----------+-------------+-----+------------+
        |1005546   |120565       |225        |courier      |400  |delivery    |
        |1005546   |10747        |225        |courier      |200  |delivery    |
        |1005546   |117530       |225        |courier      |200  |delivery    |
        |1005546   |218621       |225        |courier      |200  |delivery    |
        |1005546   |10747        |225        |courier      |50   |delivery    |
        |1005546   |218621       |225        |courier      |40   |delivery    |
        |1005546   |117530       |225        |courier      |40   |delivery    |
        |1005546   |120565       |225        |courier      |120  |delivery    |
        |1005546   |1            |225        |courier      |1100 |delivery    |
        +----------+-------------+-----------+-------------+-----+------------+
         */

        List<PartnerCapacity> capacities = Arrays.asList(
            getCapacityWithCounter(1L, 5, 5, 120565, CapacityType.REGULAR, 1),
            getCapacityWithCounter(2L, 5, 5, 1, CapacityType.REGULAR, 1),
            getCapacityWithCounter(3L, 5, 5, 255, CapacityType.REGULAR, 1),
            getCapacityWithCounter(4L, 5, 5, 213, CapacityType.REGULAR, 1)
        );

        final Long moscowRegion = 1L;
        final Long moscow = 213L;
        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(moscowRegion)
            .setLocationToId(moscow);

        List<CapacityCounter> capacityCounters = capacityService.getCapacityCountersForDecrement(
            orderToShip,
            capacities
        );
        softly.assertThat(capacityCounters).extracting(c -> c.getPartnerCapacity().getCapacityId())
            .containsExactly(2L);
    }

    private List<PartnerCapacity> getPartnerCapacities() {
        PartnerCapacity topWithType = getCapacity(1L, CapacityType.REGULAR, DeliveryType.DELIVERY, 225L, 225L);
        PartnerCapacity topWithoutType = getCapacity(2L, CapacityType.REGULAR, null, 225L, 225L);

        PartnerCapacity midWithType = getCapacity(3L, CapacityType.REGULAR, DeliveryType.POST, 1L, 20279L);
        PartnerCapacity midWithoutType = getCapacity(4L, CapacityType.REGULAR, null, 1L, 20279L);

        PartnerCapacity lowWithType = getCapacity(5L, CapacityType.REGULAR, DeliveryType.PICKUP, 1L, 20482L);
        PartnerCapacity lowWithoutType = getCapacity(6L, CapacityType.REGULAR, null, 1L, 20482L);

        PartnerCapacity reserve = getCapacity(7L, CapacityType.RESERVE, null, 1L, 213L);

        PartnerCapacity wrongLocFrom = getCapacity(8L, CapacityType.REGULAR, DeliveryType.PICKUP, 213L, 20482L);
        PartnerCapacity notInTree = getCapacity(9L, CapacityType.REGULAR, null, 1L, 117066L);
        PartnerCapacity dayFilled = getCapacity(10L, CapacityType.REGULAR, null, 225L, 225L)
            .setDay(LocalDate.of(2019, 1, 2));

        PartnerCapacity capacityWithCountingType =
            getCapacity(
                11L, CapacityType.REGULAR, DeliveryType.DELIVERY,
                CapacityCountingType.ITEM, CapacityServiceType.DELIVERY, 225L, 225L
            );

        return Lists.newArrayList(
            topWithType, topWithoutType, midWithType, midWithoutType, lowWithType, lowWithoutType, reserve,
            wrongLocFrom, notInTree, dayFilled, capacityWithCountingType
        );
    }

    private PartnerCapacity getCapacity(
        Long capacityId,
        CapacityType capacityType,
        DeliveryType deliveryType,
        Long locationFrom,
        Long locationTo
    ) {
        return getCapacity(capacityId, capacityType, deliveryType, null,
            CapacityServiceType.DELIVERY, locationFrom, locationTo
        );
    }

    private PartnerCapacity getCapacity(
        Long capacityId,
        CapacityType capacityType,
        DeliveryType deliveryType,
        CapacityCountingType countingType,
        CapacityServiceType capacityServiceType,
        Long locationFrom,
        Long locationTo
    ) {
        return new PartnerCapacity()
            .setCapacityId(capacityId)
            .setCapacityType(capacityType)
            .setDeliveryType(deliveryType)
            .setCountingType(countingType)
            .setLocationFromId(locationFrom)
            .setServiceType(capacityServiceType)
            .setLocationToId(locationTo)
            .setPartnerId(PARTNER_ID)
            .setPlatformClientId(1L)
            .setValue(1L);
    }

    private PartnerCapacity getCapacityWithCounter(
        long id,
        long currentCapacity,
        long totalCapacity,
        long locationId,
        CapacityType type
    ) {
        return getCapacityWithCounter(id, currentCapacity, totalCapacity, locationId, type,
            CapacityServiceType.DELIVERY, null, 1
        );
    }

    private PartnerCapacity getCapacityWithCounter(
        long id,
        long currentCapacity,
        long totalCapacity,
        long locationId,
        CapacityType type,
        long platformClientId
    ) {
        return getCapacityWithCounter(id, currentCapacity, totalCapacity, locationId, type,
            CapacityServiceType.DELIVERY, null, platformClientId
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private PartnerCapacity getCapacityWithCounter(
        long id,
        long currentCapacity,
        long totalCapacity,
        long locationId,
        CapacityType type,
        CapacityServiceType capacityServiceType,
        CapacityCountingType countingType,
        long platformClientId
    ) {
        return new PartnerCapacity()
            .setCapacityId(id)
            .setPartnerId(PARTNER_ID)
            .setCapacityType(type)
            .setCountingType(countingType)
            .setValue(totalCapacity)
            .setLocationFromId(locationId)
            .setLocationToId(locationId)
            .setServiceType(capacityServiceType)
            .setPlatformClientId(platformClientId)
            .addCapacityCounter(
                new CapacityCounter()
                    .setParcelCount(currentCapacity)
                    .setDay(LocalDate.of(2019, 1, 1))
                    .updateCapacityLimit(totalCapacity)
            );
    }

    private OrderToShipDto prepareOrderToShipWithValues() {
        OrderToShipDto orderToShip = prepareOrderToShip(PARTNER_ID, "123")
            .setLocationFromId(1L)
            .setLocationToId(20482L);
        Stream.of(
            new OrderToShipValue()
                .setId(1L)
                .setCountingType(CapacityCountingType.ORDER)
                .setValue(1L),
            new OrderToShipValue()
                .setId(2L)
                .setCountingType(CapacityCountingType.ITEM)
                .setValue(3L)
        ).forEach(orderToShip::addOrderToShipValue);
        return orderToShip;
    }

    private OrderToShipDto prepareOrderToShip(Long partnerId, String parcelId) {
        return prepareOrderToShip(PARTNER_ID, "123", CapacityServiceType.DELIVERY);
    }

    private OrderToShipDto prepareOrderToShip(
        Long partnerId, String parcelId,
        CapacityServiceType capacityServiceType
    ) {
        return new OrderToShipDto()
            .setId(OrderToShipConverter.toOrderToShipId(
                parcelId, 1L, partnerId, capacityServiceType, OrderToShipStatus.CREATED))
            .setLocationFromId(1L)
            .setLocationToId(213L)
            .setDeliveryType(DeliveryType.DELIVERY)
            .setShipmentDay(LocalDate.of(2019, 1, 1));
    }
}
