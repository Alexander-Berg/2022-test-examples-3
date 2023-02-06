package ru.yandex.market.sc.core.domain.sorting_center.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.sortingCenter;

/**
 * @author valter
 */
@EmbeddedDbTest
class SortingCenterRepositoryTest {

    @Autowired
    SortingCenterRepository sortingCenterRepository;

    @Test
    void save() {
        var expected = sortingCenter();
        assertThat(sortingCenterRepository.save(expected)).isEqualTo(expected);
    }

}
