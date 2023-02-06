package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.service.export.dynamic.dto.CapacityDto;
import ru.yandex.market.logistics.management.service.export.dynamic.dto.DayOffGroupDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerCapacityDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapacityPrepareServiceTest extends AbstractTest {

    private static final LocalDate DAY_1 = LocalDate.of(2019, 1, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2019, 1, 2);
    private static final Long PARTNER_ID = 1L;

    @Mock(lenient = true)
    private CapacityTreeProcessorService processorService;

    private static final Clock CLOCK_MOCK = Clock.fixed(
        ZonedDateTime.of(2018, 10, 4, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
    );
    private static final LocalDate START_DATE = LocalDate.now(CLOCK_MOCK);

    private CapacityPrepareService prepareService;

    @BeforeEach
    void setUp() {
        prepareService = new CapacityPrepareService(processorService);
        when(processorService.process((any(DayOffGroupDto.class)))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });
    }

    @Test
    void emptyRegularCapacity() {
        var capacity = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(225)
            .setServiceType(CapacityService.DELIVERY)
            .setType(CapacityType.REGULAR);

        var reserve = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(213)
            .setServiceType(CapacityService.DELIVERY)
            .setType(CapacityType.RESERVE)
            .addPartnerCapacityDayOff(createDayOff(DAY_1));

        Set<PartnerCapacityDto> capacities = ImmutableSet.of(capacity, reserve);
        var prepared = prepareService.prepareDeliveryService(
            getPartner(capacities),
            START_DATE
        );

        var day1 = getByDay(DAY_1, prepared);

        softly.assertThat(day1.getEmptyRegularCapacities()).hasSize(1)
            .containsOnlyKeys(225);

        softly.assertThat(day1.getEmptyReserveCapacities()).isEmpty();

        softly.assertThat(day1.getReserveDaysOff()).hasSize(1)
            .containsOnlyKeys(213);
    }

    @Test
    void regularDaysOff() {
        var capacity = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(213)
            .setType(CapacityType.REGULAR)
            .setServiceType(CapacityService.DELIVERY)
            .addPartnerCapacityDaysOff(List.of(createDayOff(DAY_1), createDayOff(DAY_2)));

        var capacity2 = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(98580)
            .setType(CapacityType.REGULAR)
            .setServiceType(CapacityService.DELIVERY)
            .addPartnerCapacityDaysOff(List.of(createDayOff(DAY_1), createDayOff(DAY_2)));

        Set<PartnerCapacityDto> capacities = ImmutableSet.of(capacity, capacity2);
        var prepared = prepareService.prepareDeliveryService(
            getPartner(capacities),
            START_DATE
        );

        softly.assertThat(prepared).as("Should contain days off for 2 days").hasSize(2);
        var daysOff = extractDaysOff(prepared, DayOffGroupDto::getDaysOff);

        softly.assertThat(daysOff).as("Should contain 4 days").hasSize(4);
    }

    @Test
    void emptyReservesProperlyAdded() {
        var regular = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(213)
            .setType(CapacityType.REGULAR)
            .setServiceType(CapacityService.DELIVERY)
            .addPartnerCapacityDaysOff(List.of(createDayOff(DAY_1), createDayOff(DAY_2)));

        var reserve1 = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(225)
            .setType(CapacityType.RESERVE)
            .setServiceType(CapacityService.DELIVERY)
            .addPartnerCapacityDayOff(createDayOff(DAY_1));

        var reserve2 = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(213)
            .setType(CapacityType.RESERVE)
            .setServiceType(CapacityService.DELIVERY)
            .addPartnerCapacityDayOff(createDayOff(DAY_2));

        var reserveWithDate = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(98580)
            .setServiceType(CapacityService.DELIVERY)
            .setType(CapacityType.RESERVE)
            .setDay(DAY_1);

        Set<PartnerCapacityDto> capacities = ImmutableSet.of(regular, reserve1, reserve2, reserveWithDate);
        var prepared = prepareService.prepareDeliveryService(
            getPartner(capacities),
            START_DATE
        );

        var day1 = getByDay(DAY_1, prepared);
        var day2 = getByDay(DAY_2, prepared);

        softly.assertThat(day1.getDaysOff())
            .as("Regular day off included in day 1")
            .hasSize(1)
            .containsOnlyKeys(213);

        softly.assertThat(day1.getReserveDaysOff())
            .as("Reserve day off included in day 1")
            .hasSize(1)
            .containsOnlyKeys(225);

        softly.assertThat(day1.getSingleDayReserveDaysOff())
            .as("Single day off reserve not included in day 1")
            .hasSize(0);

        softly.assertThat(day1.getEmptyReserveCapacities())
            .as("Only 2 reserves included in day 1")
            .hasSize(2)
            .containsOnlyKeys(213, 98580);

        softly.assertThat(day2.getDaysOff())
            .as("Single regular reserve included in day 2")
            .hasSize(1)
            .containsOnlyKeys(213);

        softly.assertThat(day2.getEmptyReserveCapacities())
            .as("Single reserve included in day 2")
            .hasSize(1)
            .containsOnlyKeys(225);

        softly.assertThat(day2.getSingleDayReserveDaysOff())
            .as("Day off for day 1 reserve included in day 2 singleDaysReserves")
            .hasSize(1)
            .containsOnlyKeys(98580);

        softly.assertThat(day2.getSingleDayReserveDaysOff())
            .as("Should contain day off for reserve with date")
            .hasSize(1)
            .containsOnlyKeys(98580);
    }

    @Test
    void emptyReserveIncludedInAllDaysOff() {
        var regular = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(213)
            .setType(CapacityType.REGULAR)
            .setServiceType(CapacityService.DELIVERY)
            .addPartnerCapacityDaysOff(List.of(createDayOff(DAY_1), createDayOff(DAY_2)));

        var reserve = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(225)
            .setServiceType(CapacityService.DELIVERY)
            .setType(CapacityType.RESERVE);

        Set<PartnerCapacityDto> capacities = ImmutableSet.of(regular, reserve);
        var prepared = prepareService.prepareDeliveryService(
            getPartner(capacities),
            START_DATE);

        var day1 = getByDay(DAY_1, prepared);
        var day2 = getByDay(DAY_2, prepared);

        softly.assertThat(day1.getEmptyReserveCapacities()).as("Empty reserve included in day 1").hasSize(1);
        softly.assertThat(day2.getEmptyReserveCapacities()).as("Empty reserve included in day 2").hasSize(1);
    }

    @Test
    void fulfillmentCapacitiesProperlyMapped() {
        var regular = new PartnerCapacityDto()
            .setLocationFrom(1)
            .setLocationTo(213)
            .setType(CapacityType.REGULAR)
            .setServiceType(CapacityService.SHIPMENT)
            .addPartnerCapacityDaysOff(List.of(createDayOff(DAY_1), createDayOff(DAY_2)));

        var dsDaysOff = ImmutableSet.of(
            new CapacityDto(DAY_1, 1, 216, null),
            new CapacityDto(DAY_1, 1, 20279, DeliveryType.PICKUP)
        );

        Set<PartnerCapacityDto> capacities = ImmutableSet.of(regular);
        var dtos = prepareService.prepareFulfillment(
            getPartner(capacities),
            START_DATE);
        var prepared = prepareService.addAdditionalDaysOff(dtos, dsDaysOff, ImmutableSet.of());

        softly.assertThat(prepared).hasSize(6).extracting(CapacityDto::getDate)
            .containsOnly(DAY_1, DAY_2);
    }

    @Nonnull
    private List<CapacityDto> extractDaysOff(
        Set<DayOffGroupDto> prepared,
        Function<DayOffGroupDto, Map<Integer, Set<CapacityDto>>> extractor
    ) {
        return prepared.stream()
            .map(extractor)
            .map(Map::values)
            .flatMap(Collection::stream)
            .flatMap(Set::stream)
            .collect(Collectors.toList());
    }

    private DayOffGroupDto getByDay(LocalDate day, Set<DayOffGroupDto> dtos) {
        return dtos.stream()
            .filter(dto -> day.equals(dto.getDate()))
            .findFirst()
            .orElseThrow(RuntimeException::new);
    }

    private PartnerDto getPartner(Set<PartnerCapacityDto> capacities) {
        return new PartnerDto().setId(PARTNER_ID).setCapacities(capacities);
    }

    private PartnerCapacityDto.DayOff createDayOff(LocalDate day) {
        return new PartnerCapacityDto.DayOff(null, day);
    }
}
