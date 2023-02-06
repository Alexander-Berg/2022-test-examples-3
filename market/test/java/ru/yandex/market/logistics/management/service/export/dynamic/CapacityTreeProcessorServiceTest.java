package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.service.export.dynamic.dto.CapacityDto;
import ru.yandex.market.logistics.management.service.export.dynamic.dto.DayOffGroupDto;
import ru.yandex.market.logistics.management.service.export.dynamic.dto.RegionToRegion;
import ru.yandex.market.logistics.management.util.UnitTestUtil;

class CapacityTreeProcessorServiceTest extends AbstractTest {

    private static final LocalDate DAY_1 = LocalDate.of(2019, 1, 1);

    private final CapacityTreeProcessorService service =
        new CapacityTreeProcessorService(new RegionHelper(UnitTestUtil.getRegionTree()));

    @Test
    void lowerLevelDayOffMergedIfSameOrMoreNarrowType() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 225, null))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 213, null))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 20279, DeliveryType.COURIER))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 117067, DeliveryType.PICKUP))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 117066, DeliveryType.POST));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getDaysOff())
            .as("The widest region is chosen (225)").containsOnlyKeys(225);
    }

    @Test
    void combineDaysOffWithDifferentDeliveryTypes() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 225, DeliveryType.COURIER))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 213, DeliveryType.PICKUP))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 20279, DeliveryType.POST))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 117067, null))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 117066, null));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getDaysOff())
            .as("All regions should be present").containsOnlyKeys(225, 213, 20279, 117067, 117066);
    }

    @Test
    void lowerReservesOfDifferentTypesAreSaved() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 225, null))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 213, DeliveryType.PICKUP))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 20279, DeliveryType.POST))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 117067, DeliveryType.COURIER))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 117066, null));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getDaysOff())
            .as("Should contain single day off in 225").containsOnlyKeys(225);

        softly.assertThat(result.getEmptyReserveCapacities())
            .as("Should contain all reserves as they are of different types")
            .containsOnlyKeys(213, 20279, 117067, 117066);
    }

    @Test
    void lowerReservesOfSameOrNarrowerTypesAreMerged() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 225, null))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 20279, DeliveryType.COURIER))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 117065, DeliveryType.COURIER))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 20481, DeliveryType.COURIER))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 117066, null))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 20478, DeliveryType.PICKUP));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getDaysOff())
            .as("Should contain single day off in 225").containsOnlyKeys(225);

        softly.assertThat(result.getEmptyReserveCapacities())
            .as("Should contain all reserves as they are of different types")
            .containsOnlyKeys(20279, 117066);
    }

    @Test
    void longTreeOfCapacitiesAndReserves() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 225, null))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 213, null))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 216, DeliveryType.COURIER))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 20279, DeliveryType.COURIER))
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 117066, null))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 20478, DeliveryType.PICKUP));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getDaysOff()).containsOnlyKeys(225, 216, 117066);

        softly.assertThat(result.getEmptyReserveCapacities()).containsOnlyKeys(213, 20478);
    }

    @Test
    void reserveDayOffUsedIfNoRegularCapacity() {
        var dto = new DayOffGroupDto(DAY_1)
            .addReserveDayOff(new CapacityDto(DAY_1, 1, 20478, DeliveryType.PICKUP));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getReserveDaysOff()).containsOnlyKeys(20478);
    }

    @Test
    void reserveDayOffUsedEvenIfRegularCapacityIsPresent() {
        var dto = new DayOffGroupDto(DAY_1)
            .addEmptyRegularCapacity(new CapacityDto(DAY_1, 1, 213, DeliveryType.PICKUP))
            .addReserveDayOff(new CapacityDto(DAY_1, 1, 20478, null));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getReserveDaysOff()).hasSize(1)
            .containsOnlyKeys(20478);
    }

    @Test
    void reserveDayOffProperTypeUsedIfParentTypeNull() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 213, null))
            .addReserveDayOff(new CapacityDto(DAY_1, 1, 20478, DeliveryType.PICKUP));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getReserveDaysOff()).containsOnlyKeys(20478);
        softly.assertThat(result.getReserveDaysOff().get(20478))
            .extracting(CapacityDto::getType).containsOnly((DeliveryType) null);
    }

    @Test
    void reserveDayOffProperTypeUsedIfNull() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 213, DeliveryType.POST))
            .addReserveDayOff(new CapacityDto(DAY_1, 1, 20478, null));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getReserveDaysOff()).containsOnlyKeys(20478);
        softly.assertThat(result.getReserveDaysOff().get(20478))
            .extracting(CapacityDto::getType).containsOnly((DeliveryType) null);
    }

    @Test
    void reserveDayOffProperTypeUsedIfParentOfTheSameType() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 213, DeliveryType.POST))
            .addReserveDayOff(new CapacityDto(DAY_1, 1, 20478, DeliveryType.POST));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getReserveDaysOff()).containsOnlyKeys(20478);
        softly.assertThat(result.getReserveDaysOff().get(20478))
            .extracting(CapacityDto::getType).containsOnly(DeliveryType.POST);
    }

    @Test
    void reserveDayOffNotUsedIfParentOfTheDifferentType() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 213, DeliveryType.POST))
            .addReserveDayOff(new CapacityDto(DAY_1, 1, 20478, DeliveryType.COURIER));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getReserveDaysOff()).isEmpty();
    }

    @Test
    void reserveDayOffNotUsedIfParentIsEmptyReserve() {
        var dto = new DayOffGroupDto(DAY_1)
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 213, null))
            .addReserveDayOff(new CapacityDto(DAY_1, 1, 20478, DeliveryType.COURIER));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getReserveDaysOff()).isEmpty();
    }

    @Test
    void emptyReserveCapacityNotAddedIfNoParentDayOff() {
        var dto = new DayOffGroupDto(DAY_1)
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 213, null));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getEmptyReserveCapacities()).isEmpty();
    }

    @Test
    void dayOffOfSingleReserveInOtherDateApplicable() {
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 225, null))
            .addSingleDayReserveDayOff(new CapacityDto(DAY_1, 1, 213, null));

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getDaysOff()).containsOnlyKeys(225);

        softly.assertThat(result.getSingleDayReserveDaysOff()).containsOnlyKeys(213);
    }


    @Test
    void dayOffOfSingleReserveInOtherDateNotApplicable() {
        var dto = new DayOffGroupDto(DAY_1)
            .addSingleDayReserveDayOff(new CapacityDto(DAY_1, 1, 213, null));

        var dto1 = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(new CapacityDto(DAY_1, 1, 225, null))
            .addEmptyReserveCapacity(new CapacityDto(DAY_1, 1, 213, null))
            .addSingleDayReserveDayOff(new CapacityDto(DAY_1, 1, 216, null));

        DayOffGroupDto result1 = service.process(dto);
        DayOffGroupDto result2 = service.process(dto1);

        softly.assertThat(result1.getSingleDayReserveDaysOff()).isEmpty();
        softly.assertThat(result2.getSingleDayReserveDaysOff()).isEmpty();
    }

    @Test
    public void detectParentDayOffUsingBothFromAndTo() {
        CapacityDto dto1 = new CapacityDto(DAY_1, 2, 225, null);
        CapacityDto dto2 = new CapacityDto(DAY_1, 1, 17, null);
        CapacityDto dto3 = new CapacityDto(DAY_1, 1, 3, null);
        var dto = new DayOffGroupDto(DAY_1)
            .addRegularDayOff(dto1)
            .addRegularDayOff(dto2)
            .addRegularDayOff(dto3);

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getDaysOff().get(225))
            .containsExactlyInAnyOrder(dto1);
        softly.assertThat(result.getDaysOff().get(17))
            .containsExactlyInAnyOrder(dto2);
        softly.assertThat(result.getDaysOff().get(3))
            .containsExactlyInAnyOrder(dto3);

        // the same check another way
        softly.assertThat(result.getDaysOffByRegionFromAndTo().get(new RegionToRegion(2, 225)))
            .containsExactlyInAnyOrder(dto1);
        softly.assertThat(result.getDaysOffByRegionFromAndTo().get(new RegionToRegion(1, 17)))
            .containsExactlyInAnyOrder(dto2);
        softly.assertThat(result.getDaysOffByRegionFromAndTo().get(new RegionToRegion(1, 3)))
            .containsExactlyInAnyOrder(dto3);
    }

    @Test
    public void detectParentDayOffUsingBothFromAndToReservedCapacity() {
        CapacityDto dto1 = new CapacityDto(DAY_1, 2, 225, null);
        CapacityDto dto2 = new CapacityDto(DAY_1, 1, 17, null);
        CapacityDto dto3 = new CapacityDto(DAY_1, 1, 3, null);
        var dto = new DayOffGroupDto(DAY_1)
            .addReserveDayOff(dto1)
            .addReserveDayOff(dto2)
            .addReserveDayOff(dto3);

        DayOffGroupDto result = service.process(dto);

        softly.assertThat(result.getReserveDaysOff().get(225))
            .containsExactlyInAnyOrder(dto1);
        softly.assertThat(result.getReserveDaysOff().get(17))
            .containsExactlyInAnyOrder(dto2);
        softly.assertThat(result.getReserveDaysOff().get(3))
            .containsExactlyInAnyOrder(dto3);
    }
}
