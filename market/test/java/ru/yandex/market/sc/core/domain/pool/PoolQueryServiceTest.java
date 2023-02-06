package ru.yandex.market.sc.core.domain.pool;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.pool.repository.Pool;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PoolQueryServiceTest {

    private final TestFactory testFactory;
    private final PoolQueryService poolQueryService;
    private final JdbcTemplate jdbcTemplate;

    private List<Cell> cells;
    private List<Pool> pools;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();

        cells = List.of(
                testFactory.storedCell(sortingCenter, "o1"),
                testFactory.storedCell(sortingCenter, "o2"),
                testFactory.storedCell(sortingCenter, "o3"),
                testFactory.storedCell(sortingCenter, "o4"),
                testFactory.storedCell(sortingCenter, "o5")
        );
        pools = List.of(
                testFactory.storedPool(1L, sortingCenter, Set.of()),
                testFactory.storedPool(2L, sortingCenter, Set.of(cells.get(0))),
                testFactory.storedPool(3L, sortingCenter, Set.of(cells.get(0), cells.get(1)))
        );
    }

    @Test
    public void filterFreeCellsByPool_commonPool() {
        assertThat(poolQueryService.filterCommonPoolCells(sortingCenter.getId(), Set.copyOf(cells))
        ).contains(cells.get(2), cells.get(3), cells.get(4));
    }

    @Test
    public void filterFreeCellsByPool_cellFromPool() {
        var unknownDsId = System.nanoTime() * 2;
        assertThat(poolQueryService.filterFreeCellsForDeliveryService(
                sortingCenter.getId(), unknownDsId, Set.copyOf(cells))
        ).isEmpty();

        var pool1DsId = pools.get(0).getDestinationId();
        assertThat(poolQueryService.filterFreeCellsForDeliveryService(
                sortingCenter.getId(), pool1DsId, Set.copyOf(cells))
        ).isEmpty();

        var pool2DsId = pools.get(1).getDestinationId();
        assertThat(poolQueryService.filterFreeCellsForDeliveryService(
                sortingCenter.getId(), pool2DsId, Set.copyOf(cells))
        ).containsExactlyInAnyOrder(cells.get(0));

        var pool3DsId = pools.get(2).getDestinationId();
        assertThat(poolQueryService.filterFreeCellsForDeliveryService(
                sortingCenter.getId(), pool3DsId, Set.copyOf(cells))
        ).containsExactlyInAnyOrder(cells.get(0), cells.get(1));
    }

    @Test
    @DisplayName("Проверка выбора ячейки только из переданных")
    public void filterFreeCellsByPool_cellFromPoolButNotFree() {
        var pool3DsId = pools.get(2).getDestinationId();
        assertThat(poolQueryService.filterFreeCellsForDeliveryService(
                sortingCenter.getId(), pool3DsId, Set.of(cells.get(0)))
        ).containsExactlyInAnyOrder(cells.get(0));
    }

    @Test
    @DisplayName("Проверка назначения ячеек при отсутствии пулов")
    public void filterFreeCellsByPool_noPools() {
        var sortingCenterWithoutPools = testFactory.storedSortingCenter(System.nanoTime());

        var cellsWithoutPools = List.of(
                testFactory.storedCell(sortingCenterWithoutPools, "o1"),
                testFactory.storedCell(sortingCenterWithoutPools, "o2")
        );

        assertThat(poolQueryService.filterFreeCellsForDeliveryService(
                sortingCenterWithoutPools.getId(), 1L, Set.copyOf(cellsWithoutPools))
        ).isEmpty();
        assertThat(poolQueryService.filterCommonPoolCells(
                sortingCenterWithoutPools.getId(), Set.copyOf(cellsWithoutPools))
        ).containsExactlyInAnyOrderElementsOf(cellsWithoutPools);
    }

    @Test
    @DisplayName("Проверка наличия пулов на СЦ")
    public void poolsEnabledForSortingCenterId() {
        assertThat(poolQueryService.poolsEnabledForSortingCenterId(sortingCenter.getId())).isTrue();
        jdbcTemplate.update("delete from cell_pool; delete from pool");
        assertThat(poolQueryService.poolsEnabledForSortingCenterId(sortingCenter.getId())).isFalse();
    }

}
