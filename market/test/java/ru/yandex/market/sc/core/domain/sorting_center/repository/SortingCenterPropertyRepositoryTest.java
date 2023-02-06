package ru.yandex.market.sc.core.domain.sorting_center.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class SortingCenterPropertyRepositoryTest {

    @Autowired
    SortingCenterPropertyRepository sortingCenterPropertyRepository;
    @Autowired
    TestFactory testFactory;

    @Test
    void findBySortingCenterIdAndKey() {
        var sortingCenter = testFactory.storedSortingCenter();
        var expected = new SortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.COURIER_ROUTE_SHEET_V2_ENABLED, "true"
        );
        sortingCenterPropertyRepository.save(expected);
        assertThat(sortingCenterPropertyRepository.findBySortingCenterIdAndKey(
                sortingCenter.getId(), SortingCenterPropertiesKey.COURIER_ROUTE_SHEET_V2_ENABLED
        ).orElseThrow()).isEqualToIgnoringGivenFields(expected, "id", "createdAt", "updatedAt");
    }

}
