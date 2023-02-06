package ru.yandex.market.sc.core.domain.pool.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PoolRepositoryTest {

    private final TestFactory testFactory;
    private final PoolRepository poolRepository;

    private SortingCenter sortingCenter;
    private Pool pool1;
    private Pool pool2;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();

        var cell1 = testFactory.storedCell(sortingCenter, "O1");
        var cell2 = testFactory.storedCell(sortingCenter, "O2");

        pool1 = testFactory.storedPool(2L, sortingCenter, "pool2", RouteDestinationType.COURIER, 1L,
                new HashSet<>(Set.of(cell1, cell2)));
        pool2 = testFactory.storedPool(1L, sortingCenter, "pool1", RouteDestinationType.COURIER, 1L, Set.of());

        assertThat(poolRepository.findByIdOrThrow(pool1.getId()).getCellIds()).hasSize(2);
    }

    @Test
    void getEntityClass() {
        Class<Pool> entityClass = poolRepository.getEntityClass();
        assertThat(entityClass).isEqualTo(Pool.class);
    }

    @Test
    void findAll() {
        List<Pool> allStoredPools = poolRepository.findAll();
        assertThat(allStoredPools.size()).isEqualTo(2);
        assertThat(allStoredPools).contains(pool1);
        assertThat(allStoredPools).contains(pool2);
    }

    @Test
    void deletePool() {
        poolRepository.deleteAll();
        assertThat(poolRepository.findAll()).isEmpty();
    }

}
