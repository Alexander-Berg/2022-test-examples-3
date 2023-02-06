
package ru.yandex.market.tpl.core.domain.usershift.location;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.tpl.core.domain.usershift.location.DetailingEnum.CITY_DISTRICT_RURAL;
import static ru.yandex.market.tpl.core.domain.usershift.location.DetailingEnum.CITY_FEDERATION_DISTRICT;

class DetailingEnumTest {

    @Test
    void traverse_CITY_DISTRICT_RURAL_isAllAvaliable() {

        var root = new Region(1, "1", RegionType.SETTLEMENT, null);
        var nonCitiesSubTree = new Region(2, "2", RegionType.VILLAGE, root);
        var nonCitiesSubTreeChild = new Region(3, "3", RegionType.VILLAGE, nonCitiesSubTree);
        var withCitiesSubTree = new Region(4, "4", RegionType.CITY, root);
        var withCitiesSubTreeChild1 = new Region(5, "5", RegionType.CITY_DISTRICT, withCitiesSubTree);
        var withCitiesSubTreeChild2 = new Region(6, "6", RegionType.CITY_DISTRICT, withCitiesSubTree);

        var res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, true, Set.of()));

        assertTrue(res.contains(nonCitiesSubTree));
        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));

        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));
    }

    @Test
    void traverse_CITY_DISTRICT_RURAL_isAllNotAvaliable() {

        var root = new Region(1, "1", RegionType.SETTLEMENT, null);
        var nonCitiesSubTree = new Region(2, "2", RegionType.VILLAGE, root);
        var nonCitiesSubTreeChild = new Region(3, "3", RegionType.VILLAGE, nonCitiesSubTree);
        var withCitiesSubTree = new Region(4, "4", RegionType.CITY, root);
        var withCitiesSubTreeChild1 = new Region(5, "5", RegionType.CITY_DISTRICT, withCitiesSubTree);
        var withCitiesSubTreeChild2 = new Region(6, "6", RegionType.CITY_DISTRICT, withCitiesSubTree);

        var res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of()));

        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(withCitiesSubTreeChild1));
        assertFalse(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));
    }

    @Test
    void traverse_CITY_DISTRICT_RURAL_isAllAvailableRoot() {

        var root = new Region(1, "1", RegionType.SETTLEMENT, null);

        var res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, true, Set.of()));

        assertTrue(res.contains(root));
    }

    @Test
    void traverse_CITY_DISTRICT_RURAL_isAllAvailableRootAndNoCities() {

        var root = new Region(1, "1", RegionType.SETTLEMENT, null);
        var nonCitiesSubTree1 = new Region(2, "2", RegionType.VILLAGE, root);
        var nonCitiesSubTree2 = new Region(3, "3", RegionType.VILLAGE, root);

        var res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, true, Set.of()));

        assertTrue(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTree1));
        assertFalse(res.contains(nonCitiesSubTree2));
    }


    @Test
    void traverse_CITY_DISTRICT_RURAL_checkAvailabilityLogic_nonCitiesTree() {

        var root = new Region(1, "1", RegionType.SETTLEMENT, null);
        var nonCitiesSubTree1 = new Region(2, "2", RegionType.VILLAGE, root);
        var nonCitiesSubTree2 = new Region(3, "3", RegionType.VILLAGE, root);

        var res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(3)));

        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTree1));
        assertTrue(res.contains(nonCitiesSubTree2));

        res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(1)));

        assertTrue(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTree1));
        assertFalse(res.contains(nonCitiesSubTree2));
        res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(2)));

        assertFalse(res.contains(root));
        assertTrue(res.contains(nonCitiesSubTree1));
        assertFalse(res.contains(nonCitiesSubTree2));
    }

    @Test
    void traverse_CITY_DISTRICT_RURAL_checkAvailabilityLogic_citiesTree() {

        var root = new Region(1, "1", RegionType.SETTLEMENT, null);
        var nonCitiesSubTree = new Region(2, "2", RegionType.VILLAGE, root);
        var nonCitiesSubTreeChild = new Region(3, "3", RegionType.VILLAGE, nonCitiesSubTree);
        var withCitiesSubTree = new Region(4, "4", RegionType.CITY, root);
        var withCitiesSubTreeChild1 = new Region(5, "5", RegionType.CITY_DISTRICT, withCitiesSubTree);
        var withCitiesSubTreeChild2 = new Region(6, "6", RegionType.CITY_DISTRICT, withCitiesSubTree);

        var res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(5)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertFalse(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(6)));

        assertFalse(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(4)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(3, 4)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertTrue(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(1, 3, 4)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertTrue(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_DISTRICT_RURAL.traverse(root, false, Set.of(1, 3, 5)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertFalse(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertTrue(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));
    }

    @Test
    void traverse_CITY_FEDERATION_DISTRICT_isAllAvaliable() {

        var root = new Region(1, "1", RegionType.SUBJECT_FEDERATION, null);
        var nonCitiesSubTree = new Region(2, "2", RegionType.SETTLEMENT, root);
        var nonCitiesSubTreeChild = new Region(3, "3", RegionType.SETTLEMENT, nonCitiesSubTree);
        var withCitiesSubTree = new Region(4, "4", RegionType.SETTLEMENT, root);
        var withCitiesSubTreeChild1 = new Region(5, "5", RegionType.CITY, withCitiesSubTree);
        var withCitiesSubTreeChild2 = new Region(6, "6", RegionType.SETTLEMENT, withCitiesSubTree);

        var res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, true, Set.of()));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertTrue(res.contains(nonCitiesSubTree));

        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

    }

    @Test
    void traverse_CITY_FEDERATION_DISTRICT_isAllNotAvaliable() {

        var root = new Region(1, "1", RegionType.SUBJECT_FEDERATION, null);
        var nonCitiesSubTree = new Region(2, "2", RegionType.SETTLEMENT, root);
        var nonCitiesSubTreeChild = new Region(3, "3", RegionType.SETTLEMENT, nonCitiesSubTree);
        var withCitiesSubTree = new Region(4, "4", RegionType.SETTLEMENT, root);
        var withCitiesSubTreeChild1 = new Region(5, "5", RegionType.CITY, withCitiesSubTree);
        var withCitiesSubTreeChild2 = new Region(6, "6", RegionType.SETTLEMENT, withCitiesSubTree);

        var res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of()));

        assertFalse(res.contains(withCitiesSubTreeChild1));
        assertFalse(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));
    }


    @Test
    void traverse_CITY_FEDERATION_DISTRICT_isAllAvailableRoot() {

        var root = new Region(1, "1", RegionType.SUBJECT_FEDERATION, null);

        var res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, true, Set.of()));

        assertTrue(res.contains(root));
    }

    @Test
    void traverse_CITY_FEDERATION_DISTRICT_isAllAvailableRootAndNoCities() {

        var root = new Region(1, "1", RegionType.SUBJECT_FEDERATION, null);
        var nonCitiesSubTree1 = new Region(2, "2", RegionType.SETTLEMENT, root);
        var nonCitiesSubTree2 = new Region(3, "3", RegionType.SETTLEMENT, root);

        var res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, true, Set.of()));

        assertTrue(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTree1));
        assertFalse(res.contains(nonCitiesSubTree2));
    }

    @Test
    void traverse_CITY_FEDERATION_DISTRICT_checkAvailabilityLogic_nonCitiesTree() {

        var root = new Region(1, "1", RegionType.SUBJECT_FEDERATION, null);
        var nonCitiesSubTree1 = new Region(2, "2", RegionType.SETTLEMENT, root);
        var nonCitiesSubTree2 = new Region(3, "3", RegionType.SETTLEMENT, root);

        var res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(3)));

        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTree1));
        assertTrue(res.contains(nonCitiesSubTree2));

        res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(1)));

        assertTrue(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTree1));
        assertFalse(res.contains(nonCitiesSubTree2));
        res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(2)));

        assertFalse(res.contains(root));
        assertTrue(res.contains(nonCitiesSubTree1));
        assertFalse(res.contains(nonCitiesSubTree2));
    }

    @Test
    void traverse_CITY_FEDERATION_DISTRICT_checkAvailabilityLogic_citiesTree() {

        var root = new Region(1, "1", RegionType.SUBJECT_FEDERATION, null);
        var nonCitiesSubTree = new Region(2, "2", RegionType.SETTLEMENT, root);
        var nonCitiesSubTreeChild = new Region(3, "3", RegionType.SETTLEMENT, nonCitiesSubTree);
        var withCitiesSubTree = new Region(4, "4", RegionType.SETTLEMENT, root);
        var withCitiesSubTreeChild1 = new Region(5, "5", RegionType.CITY, withCitiesSubTree);
        var withCitiesSubTreeChild2 = new Region(6, "6", RegionType.SETTLEMENT, withCitiesSubTree);

        var res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(5)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertFalse(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(6)));

        assertFalse(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(4)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertFalse(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(3, 4)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertTrue(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(1, 3, 4)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertTrue(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertTrue(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));

        res = new HashSet<>(CITY_FEDERATION_DISTRICT.traverse(root, false, Set.of(1, 3, 5)));

        assertTrue(res.contains(withCitiesSubTreeChild1));
        assertFalse(res.contains(withCitiesSubTreeChild2));
        assertFalse(res.contains(nonCitiesSubTree));
        assertFalse(res.contains(root));
        assertTrue(res.contains(nonCitiesSubTreeChild));
        assertFalse(res.contains(withCitiesSubTree));
    }

}
