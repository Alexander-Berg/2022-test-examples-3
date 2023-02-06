package ru.yandex.travel.hotels.common.partners.travelline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.util.Lists;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.travelline.model.AgeGroup;
import ru.yandex.travel.hotels.common.partners.travelline.model.Currency;
import ru.yandex.travel.hotels.common.partners.travelline.model.GuestCount;
import ru.yandex.travel.hotels.common.partners.travelline.model.GuestPlacementKind;
import ru.yandex.travel.hotels.common.partners.travelline.model.Placement;
import ru.yandex.travel.hotels.common.partners.travelline.placements.Allocation;
import ru.yandex.travel.hotels.common.partners.travelline.placements.InvalidPlacementAllocationException;
import ru.yandex.travel.hotels.common.partners.travelline.placements.PlacementGenerator;
import ru.yandex.travel.hotels.common.token.Occupancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestPlacements {
    private static final List<AgeGroup> CHILDREN = List.of(AgeGroup.builder().minAge(0).maxAge(5).build(),
            AgeGroup.builder().minAge(6).maxAge(11).build());


    @Test
    public void testAllocationGeneration() {
        // ================
        // Двухместный номер: ни дополнительных, ни детских-без-мест (как "Комфорт" в Деметре)
        // ================
        // Двух гостей селим нормально
        var res = PlacementGenerator.generateAllocation(Occupancy.fromString("2"), 2, 0, 0, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OO");

        // Одного тоже
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("1"), 2, 0, 0, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("O");

        // Ребенка селим на взрослое место
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("1-1"), 2, 0, 0, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("Oo");

        // Трех взрослых не можем
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("3"), 2, 0, 0, CHILDREN);
        assertThat(res).isEmpty();

        // Двоих взрослых с детьми - тоже не можем
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2-1"), 2, 0, 0, CHILDREN);
        assertThat(res).isEmpty();

        // ================
        // Двухместный с детским-без-места (как "Классик" в Деметре)
        // ================
        // Взрослый с ребенком: либо селим взрослого и ребенка на основные места, либо ребенка без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("1-4"), 2, 0, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("Oo", "O+c");

        // Взрослый с ребенком, не подходищим по возрасту на "без места": селим на основные
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("1-12"), 2, 0, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("Oo");

        // ================
        // Два основных, одно дополнительное, одно без мест (как "Супериор", "Делюкс" или "Джуниор Суит" в
        // Деметре)
        // ================
        // Два взрослых
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2"), 2, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OO");

        // Один взрослый, один ребенок 1 год. Ребенка можно либо на основное, либо без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("1-1"), 2, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("Oo", "O+c");

        // Два взрослых, один ребенок 1 год. Ребенка можно либо на дополнительное детское, либо без места.
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2-1"), 2, 1, 1, CHILDREN);

        // ================
        // Номер 3 основных, одно дополнительное, одно без места (как "Семейный" в Деметре)
        // ================
        // Два взрослых
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OO");

        // Трое взрослых
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("3"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OOO");

        // Четверо взрослых
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("4"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OOO+O");

        // Два взрослых, ребенок 1 год: ребенка можно либо на третье основное, либо без-места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2-1"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OOo", "OO+c");

        // Два взрослых, ребенок 12 лет: ребенка можно только на третье основное, так как на без-места он не проходит
        // по возрасту
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2-12"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OOo");

        // Три взрослых, ребенок 1 год: можно всех взрослых селить на основные места, а ребенка либо на
        // дополнительное, либо без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("3-1"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OOO+o", "OOO+c");

        // Четверо взрослых, ребенок 1 год: троих взрослых селим на основные места, одного - на дополнительное,
        // ребенка - без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("4-1"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OOO+Oc");

        // Четверо взрослых, ребенок 12 лет: троих взрослых селим на основные места, одного - на дополнительное, но
        // ребенок не влезает, так что нет офферов
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("4-12"), 3, 1, 1, CHILDREN);
        assertThat(res).isEmpty();

        // Один взрослый, три ребенка: один взрослый и два ребенок на основном, третий ребенок либо дополнительном,
        // либо - без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("1-1,2,3"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsOnly("Ooo+o", "Ooo+c");
        assertThat(res).hasSize(12); // трех детей по четырем местам расселить можно 12 способами (всего -  4!/1! =
        // 24, но из них 12 имеет пропуск на двух основных местах, т.е не валидны)

        // Два взрослых, три ребенка: взрослые и одни ребенок на основном, второй ребенок на дополнительном, третий -
        // без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2-1,2,3"), 3, 1, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsOnly("OOo+oc");
        assertThat(res).hasSize(6); // трех детей по трем местам расселить можно 3!/0! = 6 способами

        // ================
        // Двухместный номер с двумя дополнительными кроватями и одним без места (как "Двухместный Делюкс" в
        // "Санкт-Петербурге")
        // ================
        // Один взрослый и ребенок. Дополнительные места не участвуют, ребенка селим либо на взрослое, либо без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("1-1"), 2, 2, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("Oo", "O+c");

        // Два взрослых и ребенок. Оба взрослых - на основных местах, ребенка селим либо на дополнительное, либо без
        // места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2-1"), 2, 2, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsExactlyInAnyOrder("OO+o", "OO+c");

        // Два взрослых и два ребенка. Оба взрослых - на основных местах, одного ребенка селим на дополнительное,
        // второго - либо тоже на дополнительное, либо без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2-1,2"), 2, 2, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsOnly("OO+oc", "OO+oo");
        assertThat(res).hasSize(4);

        // Три взрослых и два ребенка. Оба взрослых - на основных местах, одного ребенка селим на дополнительное,
        // второго - либо тоже на дополнительное, либо без места
        res = PlacementGenerator.generateAllocation(Occupancy.fromString("2-1,2"), 2, 2, 1, CHILDREN);
        assertThat(res).extracting(Allocation::toString).containsOnly("OO+oc", "OO+oo");
        assertThat(res).hasSize(4);
    }

    @Test
    public void testManyIterations() {
        var res = PlacementGenerator.generateAllocation(Occupancy.fromString("8-1,2,3,4"), 40, 0, 0, CHILDREN);
        assertThat(res).isNotEmpty();
    }

    @Test
    public void testTooManyIterations() {
        assertThatThrownBy(() -> PlacementGenerator.generateAllocation(Occupancy.fromString("8-1,2,3,4,5,6,7"), 40, 0
                , 0, CHILDREN)).isInstanceOf(InvalidPlacementAllocationException.class);
    }

    @Test
    public void testAllocateAndMapTwoAdults() {
        var placements = getDemetraFamilyPlacements();
        var occupancy = Occupancy.fromString("2");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 3, 1, 1, CHILDREN));
        var placementSet = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(0),
                getDemetraAgeGroups()
                , occupancy, false, false, false);
        assertThat(placementSet.getPlacements()).hasSize(1);
        assertThat(placementSet.getPlacements().get(0).getIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).hasSize(1);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getCount()).isEqualTo(2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getPlacementIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getAgeQualifyingCode()).isEqualTo("adult");
        assertThat(placementSet.getGuestPlacementIndexes()).containsExactly(0, 0);
    }

    @Test
    public void testAllocateAndMapTwoAdultsOneChild() {
        var placements = getDemetraFamilyPlacements();
        var occupancy = Occupancy.fromString("2-1");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 3, 1, 1, CHILDREN));

        var placementSet = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(0),
                getDemetraAgeGroups(), occupancy, false, false, false);
        assertThat(placementSet.getPlacements()).hasSize(1);
        assertThat(placementSet.getPlacements().get(0).getIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).hasSize(2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getCount()).isEqualTo(2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getPlacementIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getAgeQualifyingCode()).isEqualTo("adult");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getCount()).isEqualTo(1);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getPlacementIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getAgeQualifyingCode()).isEqualTo("child");
        assertThat(placementSet.getGuestPlacementIndexes()).containsExactly(0, 0, 0);

        placementSet = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(1),
                getDemetraAgeGroups(), occupancy, false, false, false);
        assertThat(placementSet.getPlacements()).hasSize(2);
        assertThat(placementSet.getPlacements().get(0).getIndex()).isEqualTo(0);
        assertThat(placementSet.getPlacements().get(1).getCode()).isEqualTo("23691");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).hasSize(2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getCount()).isEqualTo(2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getPlacementIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getAgeQualifyingCode()).isEqualTo("adult");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getCount()).isEqualTo(1);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getPlacementIndex()).isEqualTo(placementSet.getPlacements().get(1).getIndex());
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getAgeQualifyingCode()).isEqualTo("child");
        assertThat(placementSet.getGuestPlacementIndexes()).containsExactly(0, 0, 1);
    }

    @Test
    public void testAllocateAndMapTwoAdultsOneOlderChild() {
        var placements = getDemetraFamilyPlacements();
        var occupancy = Occupancy.fromString("2-14");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 3, 1, 1, CHILDREN));
        assertThat(allocations).hasSize(1);
        var placementSet = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(0),
                getDemetraAgeGroups(), occupancy, false, false, false);

        assertThat(placementSet.getPlacements()).hasSize(1);
        assertThat(placementSet.getPlacements().get(0).getIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).hasSize(2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getCount()).isEqualTo(2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getPlacementIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getAgeQualifyingCode()).isEqualTo("adult");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getCount()).isEqualTo(1);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getPlacementIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(1).getAgeQualifyingCode()).isEqualTo("child");
        assertThat(placementSet.getGuestPlacementIndexes()).containsExactly(0, 0, 0);
    }

    @Test
    public void testAllocateAndMapFourAdultsInPushkin() {
        var placements = getPushkinSemiJuniorLuxPlacements();
        var occupancy = Occupancy.fromString("4");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 2, 2, 0, Lists.emptyList()));
        assertThat(allocations).hasSize(1);
        var placementSet = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(0),
                getPushkinAgeGroups()
                , occupancy, false, false, false);
        assertThat(placementSet.getPlacements()).extracting(Placement::getKind).containsExactly(GuestPlacementKind.ADULT, GuestPlacementKind.EXTRA_ADULT, GuestPlacementKind.EXTRA_ADULT);
        assertThat(placementSet.getPlacements()).extracting(Placement::getIndex).containsExactly(0, 1, 2);
        assertThat(placementSet.getPlacements()).extracting(Placement::getCapacity).containsExactly(2, 1, 1);
        assertThat(placementSet.getPlacements()).extracting(Placement::getCode).containsExactly("132728", "31776",
                "31776");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getCount).containsExactly(2, 1, 1);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getAgeQualifyingCode).containsOnly("adult");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getPlacementIndex).containsExactly(0, 1, 2);
        assertThat(placementSet.getGuestPlacementIndexes()).containsExactly(0, 0, 1, 2);
    }


    @Test
    public void testAllocateAndMapTwoAdultsTwoChildrenInPushkin() {
        var placements = getPushkinSemiJuniorLuxPlacements();
        var occupancy = Occupancy.fromString("2-1,2");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 2, 2, 0, Lists.emptyList()));
        assertThat(allocations).hasSize(2);
        var placementSet = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(0),
                getPushkinAgeGroups()
                , occupancy, false, false, false);
        assertThat(placementSet.getPlacements()).extracting(Placement::getKind).containsExactly(GuestPlacementKind.ADULT, GuestPlacementKind.EXTRA_CHILD, GuestPlacementKind.EXTRA_CHILD);
        assertThat(placementSet.getPlacements()).extracting(Placement::getIndex).containsExactly(0, 1, 2);
        assertThat(placementSet.getPlacements()).extracting(Placement::getCapacity).containsExactly(2, 1, 1);
        assertThat(placementSet.getPlacements()).extracting(Placement::getCode).containsExactly("132728", "44278",
                "44278");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getCount).containsExactly(2, 1, 1);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getAgeQualifyingCode).containsExactly("adult", "child", "child");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getPlacementIndex).containsExactlyInAnyOrder(0, 1, 2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getPlacementIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestPlacementIndexes()).containsExactlyInAnyOrder(0, 0, 1, 2);
        assertThat(placementSet.getGuestPlacementIndexes().get(0)).isEqualTo(0);
    }

    @Test
    public void testAllocateAndMapOneAdultsThreeChildrenInPushkin() {
        var placements = getPushkinSemiJuniorLuxPlacements();
        var occupancy = Occupancy.fromString("1-1,2,3");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 2, 2, 0, Lists.emptyList()));
        assertThat(allocations).hasSize(6);
        var placementSet = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(0),
                getPushkinAgeGroups()
                , occupancy, false, false, false);
        assertThat(placementSet.getPlacements()).extracting(Placement::getKind).containsExactly(GuestPlacementKind.ADULT, GuestPlacementKind.EXTRA_CHILD, GuestPlacementKind.EXTRA_CHILD);
        assertThat(placementSet.getPlacements()).extracting(Placement::getIndex).containsExactly(0, 1, 2);
        assertThat(placementSet.getPlacements()).extracting(Placement::getCapacity).containsExactly(2, 1, 1);
        assertThat(placementSet.getPlacements()).extracting(Placement::getCode).containsExactly("132728", "44278",
                "44278");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getCount).containsExactly(1, 1, 1, 1);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getAgeQualifyingCode).containsExactly("adult", "child", "child", "child");
        assertThat(placementSet.getGuestCountInfo().getGuestCounts()).extracting(GuestCount::getPlacementIndex).containsExactlyInAnyOrder(0, 0, 1, 2);
        assertThat(placementSet.getGuestCountInfo().getGuestCounts().get(0).getPlacementIndex()).isEqualTo(0);
        assertThat(placementSet.getGuestPlacementIndexes()).containsExactlyInAnyOrder(0, 0, 1, 2);
        assertThat(placementSet.getGuestPlacementIndexes().get(0)).isEqualTo(0);
    }

    @Test
    public void testSanatoriumPlacements1() {
        var placements = getSanatoriumPlacements();
        var occupancy = Occupancy.fromString("3-5,2");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 2, 2, 1,
                new ArrayList<>(getSanatoriumAgeGroups().values())));
        assertThat(allocations).hasSize(2);
        var placementSet = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(0),
                getSanatoriumAgeGroups(), occupancy, true, false, false);
        assertThat(placementSet.getTotalCost()).isEqualTo(21532);
    }

    @Test
    public void testSanatoriumPlacements2() {
        var placements = getSanatoriumPlacements();
        var occupancy = Occupancy.fromString("1-2");
        var allocationsSet = PlacementGenerator.generateAllocation(occupancy, 2, 2, 1,
                new ArrayList<>(getSanatoriumAgeGroups().values()));
        assertThat(allocationsSet).hasSize(2);
        var allocations = allocationsSet.stream().sorted(
                Comparator.comparingInt(a -> a.getChildrenWithNoPlaceIndexes().size())
        ).collect(Collectors.toList());
        var placementSet1 = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(0),
                getSanatoriumAgeGroups(), occupancy, true, false, false);
        var placementSet2 = PlacementGenerator.mapPlacementsForAllocation(placements, allocations.get(1),
                getSanatoriumAgeGroups(), occupancy, true, false, false);
        assertThat(placementSet1.getPlacements().get(0).getCapacity()).isEqualTo(2);
        assertThat(placementSet1.getPlacements().get(1).getCapacity()).isEqualTo(2);
        assertThat(placementSet1.getTotalCost()).isEqualTo(13050);
        assertThat(placementSet2.getPlacements().get(0).getCapacity()).isEqualTo(1);
        assertThat(placementSet2.getPlacements().get(1).getCapacity()).isEqualTo(1);
        assertThat(placementSet2.getTotalCost()).isEqualTo(11250);
    }

    @Test
    public void testExtraChildWhenLargerAdultsArePresent() {
        var placements = getVozdvishenskoePlacements();
        var occupancy = Occupancy.fromString("2-5,7");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 4, 2, 1,
                Collections.emptyList()));
        assertThat(allocations).hasSize(2);

        var placementSet = PlacementGenerator.mapPlacementsForAllocation(
                placements,
                allocations.get(0),
                getVozdvishenskoeAgeGroups(),
                occupancy,
                false,
                false, false);
        assertThat(placementSet).isNotNull();
        assertThat(placementSet.getPlacements()).hasSize(3);
        assertThat(placementSet.getPlacements().get(0).getKind()).isEqualTo(GuestPlacementKind.ADULT);
        assertThat(placementSet.getPlacements().get(0).getCapacity()).isEqualTo(2);
        assertThat(placementSet.getPlacements().get(1).getKind()).isEqualTo(GuestPlacementKind.CHILD);
        assertThat(placementSet.getPlacements().get(1).getCapacity()).isEqualTo(1);
        assertThat(placementSet.getPlacements().get(2).getKind()).isEqualTo(GuestPlacementKind.CHILD);
    }

    @Test
    public void testPreAgeChildAsAdultOnPrimary() {
        var placements = getTwoPrimaryAdultsPlacement();
        var occupancy = Occupancy.fromString("1-4");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 2, 0, 0,
                Collections.emptyList()));
        assertThat(allocations).hasSize(1);

        var placementSet = PlacementGenerator.mapPlacementsForAllocation(
                placements,
                allocations.get(0),
                Collections.emptyMap(),
                occupancy,
                false,
                false, false);
        assertThat(placementSet).isNotNull();
        assertThat(placementSet.getPlacements()).hasSize(1);
        assertThat(placementSet.getPlacements().get(0).getKind()).isEqualTo(GuestPlacementKind.ADULT);
        assertThat(placementSet.getPlacements().get(0).getCapacity()).isEqualTo(2);
        assertThat(placementSet.getTotalCost()).isEqualTo(200);
    }

    @Test
    public void testPreAgeChildAsAdultsOnExtra() {
        var placements = getPrimaryAndExtraAdultsPlacement();
        var occupancy = Occupancy.fromString("1-4");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 1, 1, 0,
                Collections.emptyList()));
        assertThat(allocations).hasSize(1);

        var placementSet = PlacementGenerator.mapPlacementsForAllocation(
                placements,
                allocations.get(0),
                Collections.emptyMap(),
                occupancy,
                false,
                false, false);
        assertThat(placementSet).isNotNull();
        assertThat(placementSet.getPlacements()).hasSize(2);
        assertThat(placementSet.getPlacements().get(0).getKind()).isEqualTo(GuestPlacementKind.ADULT);
        assertThat(placementSet.getPlacements().get(1).getKind()).isEqualTo(GuestPlacementKind.EXTRA_ADULT);
        assertThat(placementSet.getTotalCost()).isEqualTo(150);
    }

    @Test
    public void testNoPreAgeChildAsAdultOnPrimaryIfTeenageAgeGroupSpecified() {
        var placements = getTwoPrimaryAdultsPlacement();
        var occupancy = Occupancy.fromString("1-4");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 2, 0, 0,
                Collections.emptyList()));
        assertThat(allocations).hasSize(1);

        assertThatThrownBy(() -> PlacementGenerator.mapPlacementsForAllocation(
                placements,
                allocations.get(0),
                getTeenagerAgeGroups(),
                occupancy,
                false,
                false, false)).isInstanceOf(InvalidPlacementAllocationException.class);
    }

    @Test
    public void testNoPreAgeChildAsAdultsOnExtraIfTeenageAgeGroupSpecified() {
        var placements = getPrimaryAndExtraAdultsPlacement();
        var occupancy = Occupancy.fromString("1-4");
        var allocations = List.copyOf(PlacementGenerator.generateAllocation(occupancy, 1, 1, 0,
                Collections.emptyList()));
        assertThat(allocations).hasSize(1);

        assertThatThrownBy(() -> PlacementGenerator.mapPlacementsForAllocation(
                placements,
                allocations.get(0),
                getTeenagerAgeGroups(),
                occupancy,
                false,
                false, false)).isInstanceOf(InvalidPlacementAllocationException.class);
    }


    private Map<Integer, AgeGroup> getDemetraAgeGroups() {
        Map<Integer, AgeGroup> ageGroupsByAge = new HashMap<>();
        List.of(
                AgeGroup.builder()
                        .code("5435")
                        .minAge(0)
                        .maxAge(5)
                        .build(),
                AgeGroup.builder()
                        .code("5436")
                        .minAge(6)
                        .maxAge(11)
                        .build(),
                AgeGroup.builder()
                        .code("5437")
                        .minAge(12)
                        .maxAge(17)
                        .build()
        ).forEach(ag -> IntStream.range(ag.getMinAge(), ag.getMaxAge() + 1).forEach(age -> ageGroupsByAge.put(age,
                ag)));
        return ageGroupsByAge;
    }

    private List<Placement> getDemetraFamilyPlacements() {
        return List.of(
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(0)
                        .code("182298")
                        .capacity(3)
                        .priceBeforeTax(30800)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD)
                        .index(1)
                        .code("67310")
                        .capacity(1)
                        .ageGroup(5435)
                        .priceBeforeTax(0)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD)
                        .index(2)
                        .code("67312")
                        .capacity(1)
                        .ageGroup(5436)
                        .priceBeforeTax(0)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD)
                        .index(3)
                        .code("67314")
                        .capacity(1)
                        .ageGroup(5437)
                        .priceBeforeTax(0)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_ADULT)
                        .index(7)
                        .code("51706")
                        .capacity(1)
                        .priceBeforeTax(6000)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(6)
                        .code("55470")
                        .capacity(1)
                        .ageGroup(5435)
                        .priceBeforeTax(3940)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(4)
                        .code("49070")
                        .capacity(1)
                        .ageGroup(5436)
                        .priceBeforeTax(4840)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(5)
                        .code("49071")
                        .capacity(1)
                        .ageGroup(5437)
                        .priceBeforeTax(5540)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD_BAND_WITHOUT_BED)
                        .index(8)
                        .code("23691")
                        .capacity(1)
                        .ageGroup(5435)
                        .priceBeforeTax(0)
                        .currency(Currency.RUB)
                        .build()
        );
    }

    private Map<Integer, AgeGroup> getSanatoriumAgeGroups() {
        Map<Integer, AgeGroup> ageGroupsByAge = new HashMap<>();
        List.of(
                AgeGroup.builder()
                        .code("4621")
                        .minAge(0)
                        .maxAge(5)
                        .build(),
                AgeGroup.builder()
                        .code("4667")
                        .minAge(6)
                        .maxAge(11)
                        .build()
        ).forEach(ag -> IntStream.range(ag.getMinAge(), ag.getMaxAge() + 1).forEach(age -> ageGroupsByAge.put(age,
                ag)));
        return ageGroupsByAge;
    }

    private List<Placement> getSanatoriumPlacements() {
        return List.of(
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(0)
                        .code("706692")
                        .capacity(1)
                        .priceBeforeTax(11250)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(1)
                        .code("706691")
                        .capacity(2)
                        .priceBeforeTax(7410)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD)
                        .index(2)
                        .code("33517")
                        .capacity(2)
                        .ageGroup(4621)
                        .priceBeforeTax(5640)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD)
                        .index(3)
                        .code("33518")
                        .capacity(2)
                        .ageGroup(4667)
                        .priceBeforeTax(6710)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_ADULT)
                        .index(8)
                        .code("17554")
                        .capacity(1)
                        .priceBeforeTax(4912)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_ADULT)
                        .index(9)
                        .code("29060")
                        .capacity(2)
                        .priceBeforeTax(4912)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(4)
                        .code("14284")
                        .capacity(1)
                        .ageGroup(4621)
                        .priceBeforeTax(1800)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(6)
                        .code("27069")
                        .capacity(2)
                        .ageGroup(4621)
                        .priceBeforeTax(1800)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(5)
                        .code("14285")
                        .capacity(1)
                        .ageGroup(4667)
                        .priceBeforeTax(4180)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(7)
                        .code("27070")
                        .capacity(2)
                        .ageGroup(4667)
                        .priceBeforeTax(4180)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD_BAND_WITHOUT_BED)
                        .index(10)
                        .code("23691")
                        .capacity(1)
                        .ageGroup(4667)
                        .priceBeforeTax(100)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD_BAND_WITHOUT_BED)
                        .index(11)
                        .code("23692")
                        .capacity(1)
                        .ageGroup(4621)
                        .priceBeforeTax(0)
                        .currency(Currency.RUB)
                        .build()
        );
    }

    private Map<Integer, AgeGroup> getPushkinAgeGroups() {
        Map<Integer, AgeGroup> ageGroupsByAge = new HashMap<>();
        List.of(
                AgeGroup.builder()
                        .code("8506")
                        .minAge(0)
                        .maxAge(6)
                        .build()
        ).forEach(ag -> IntStream.range(ag.getMinAge(), ag.getMaxAge() + 1).forEach(age -> ageGroupsByAge.put(age,
                ag)));
        return ageGroupsByAge;
    }

    private List<Placement> getPushkinSemiJuniorLuxPlacements() {
        return List.of(
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(0)
                        .code("132727")
                        .capacity(1)
                        .priceBeforeTax(4465.0)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(1)
                        .code("132728")
                        .capacity(2)
                        .priceBeforeTax(5415.0)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_ADULT)
                        .index(3)
                        .code("31776")
                        .capacity(1)
                        .priceBeforeTax(1425.00)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(2)
                        .code("44278")
                        .capacity(1)
                        .ageGroup(8506)
                        .priceBeforeTax(1425.00)
                        .currency(Currency.RUB)
                        .build()
        );
    }

    private Map<Integer, AgeGroup> getVozdvishenskoeAgeGroups() {
        Map<Integer, AgeGroup> ageGroupsByAge = new HashMap<>();
        List.of(
                AgeGroup.builder()
                        .code("3880")
                        .minAge(0)
                        .maxAge(2)
                        .build(),
                AgeGroup.builder()
                        .code("2971")
                        .minAge(3)
                        .maxAge(9)
                        .build()
        ).forEach(ag -> IntStream.range(ag.getMinAge(), ag.getMaxAge() + 1).forEach(age -> ageGroupsByAge.put(age,
                ag)));
        return ageGroupsByAge;

    }

    private List<Placement> getVozdvishenskoePlacements() {
        return List.of(
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(0)
                        .code("211975")
                        .capacity(1)
                        .priceBeforeTax(46800)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(1)
                        .code("150104")
                        .capacity(2)
                        .priceBeforeTax(49200)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(2)
                        .code("211976")
                        .capacity(3)
                        .priceBeforeTax(55200)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(3)
                        .code("211974")
                        .capacity(4)
                        .priceBeforeTax(61200)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.CHILD)
                        .index(4)
                        .code("47884")
                        .capacity(1)
                        .ageGroup(2971)
                        .priceBeforeTax(4000)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_ADULT)
                        .index(8)
                        .code("37131")
                        .capacity(1)
                        .priceBeforeTax(6000)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(6)
                        .code("96178")
                        .capacity(1)
                        .ageGroup(3880)
                        .priceBeforeTax(4000)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_CHILD)
                        .index(7)
                        .code("36756")
                        .capacity(1)
                        .ageGroup(2971)
                        .priceBeforeTax(4000)
                        .currency(Currency.RUB)
                        .build()
        );
    }

    private List<Placement> getTwoPrimaryAdultsPlacement() {
        return List.of(
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(0)
                        .code("1")
                        .capacity(1)
                        .priceBeforeTax(100)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(1)
                        .code("2")
                        .capacity(2)
                        .priceBeforeTax(200)
                        .currency(Currency.RUB)
                        .build()
        );
    }

    private List<Placement> getPrimaryAndExtraAdultsPlacement() {
        return List.of(
                Placement.builder()
                        .kind(GuestPlacementKind.ADULT)
                        .index(0)
                        .code("1")
                        .capacity(1)
                        .priceBeforeTax(100)
                        .currency(Currency.RUB)
                        .build(),
                Placement.builder()
                        .kind(GuestPlacementKind.EXTRA_ADULT)
                        .index(1)
                        .code("2")
                        .capacity(1)
                        .priceBeforeTax(50)
                        .currency(Currency.RUB)
                        .build()
        );
    }

    private Map<Integer, AgeGroup> getTeenagerAgeGroups() {
        Map<Integer, AgeGroup> ageGroupsByAge = new HashMap<>();
        List.of(
                AgeGroup.builder()
                        .code("20589")
                        .minAge(12)
                        .maxAge(17)
                        .build()
        ).forEach(ag -> IntStream.range(ag.getMinAge(), ag.getMaxAge() + 1).forEach(age -> ageGroupsByAge.put(age,
                ag)));
        return ageGroupsByAge;

    }


}
