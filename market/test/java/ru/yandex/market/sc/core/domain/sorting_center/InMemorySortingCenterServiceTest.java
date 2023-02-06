package ru.yandex.market.sc.core.domain.sorting_center;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class InMemorySortingCenterServiceTest {

    @Autowired
    InMemorySortingCenterService inMemorySortingCenterService;
    @Autowired
    TestFactory testFactory;

    @AfterEach
    void tearDown() {
        inMemorySortingCenterService.clean();
    }

    @Test
    void getSortingCenterByPartnerIdFromDb() {
        long total = 10;
        List<SortingCenter> expectedSortingCenters = LongStream.range(0L, total).mapToObj(
                i -> testFactory.storedSortingCenter(i)
        ).toList();

        inMemorySortingCenterService.clean();

        for (long i = 0L; i < total; i++) {
            assertThat(inMemorySortingCenterService.findByPartnerId("partner-" + i).orElseThrow())
                    .isEqualTo(expectedSortingCenters.get((int) i));
        }
    }

    @Test
    void getSortingCenterByPartnerIdFromCache() {
        long total = 10;
        List<SortingCenter> expectedSortingCenters = LongStream.range(0L, total).mapToObj(
                i -> testFactory.storedSortingCenter(i)
        ).toList();

        inMemorySortingCenterService.loadSync();

        for (long i = 0L; i < total; i++) {
            assertThat(inMemorySortingCenterService.findByPartnerId("partner-" + i).orElseThrow())
                    .isEqualTo(expectedSortingCenters.get((int) i));
        }
    }

}
