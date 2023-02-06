package ru.yandex.market.logistics.utilizer.repo;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.entity.StocksForUtilization;
import ru.yandex.market.logistics.utilizer.domain.enums.StockType;
import ru.yandex.market.logistics.utilizer.domain.enums.UtilizationCycleStatus;
import ru.yandex.market.logistics.utilizer.domain.internal.AggregatedCountForStock;

public class StocksForUtilizationJpaRepositoryTest extends AbstractContextualTest {
    @Autowired
    StocksForUtilizationJpaRepository stocksForUtilizationJpaRepository;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/stocks-for-utilization/1/db-state.xml")
    void loadTest() {
        StocksForUtilization result = stocksForUtilizationJpaRepository.findById(1L).get();
        softly.assertThat(result).isNotNull();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/stocks-for-utilization/2/db-state.xml")
    void findAllByUtilizationCycleIdHavingCountGreaterZero() {
        List<StocksForUtilization> stocksForUtilization =
                stocksForUtilizationJpaRepository.findAllByUtilizationCycleIdHavingCountGreaterZero(1);
        softly.assertThat(stocksForUtilization).hasSize(2);
        Set<Long> ids = stocksForUtilization.stream()
                .map(StocksForUtilization::getId)
                .collect(Collectors.toSet());
        softly.assertThat(ids).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/stocks-for-utilization/2/db-state.xml")
    void getCountOfItemsInCycle() {
        int items = stocksForUtilizationJpaRepository.getCountOfItemsInCycle(1);
        softly.assertThat(items).isEqualTo(11);

        items = stocksForUtilizationJpaRepository.getCountOfItemsInCycle(3);
        softly.assertThat(items).isEqualTo(0);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/stocks-for-utilization/3/db-state.xml")
    void getAggregatedCountForStocks() {
        Map<StockType, Long> countByStockType = stocksForUtilizationJpaRepository.getAggregatedCountForStocks(
                1,
                EnumSet.of(UtilizationCycleStatus.CREATED, UtilizationCycleStatus.FINALIZED)
        ).stream().collect(Collectors.toMap(AggregatedCountForStock::getStockType, AggregatedCountForStock::getCount));
        softly.assertThat(countByStockType).hasSize(2);
        softly.assertThat(countByStockType.get(StockType.DEFECT)).isEqualTo(30);
        softly.assertThat(countByStockType.get(StockType.EXPIRED)).isEqualTo(1);

        List<AggregatedCountForStock> aggregatedCounts = stocksForUtilizationJpaRepository.getAggregatedCountForStocks(
                3,
                EnumSet.of(UtilizationCycleStatus.TRANSFERRED)
        );
        softly.assertThat(aggregatedCounts).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/stocks-for-utilization/3/db-state.xml")
    void findAllByUtilizationCycleId() {
        Set<Long> ids = stocksForUtilizationJpaRepository.findAllByUtilizationCycleId(1).stream()
                .map(StocksForUtilization::getId)
                .collect(Collectors.toSet());
        softly.assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 4L);

        List<StocksForUtilization> stocks = stocksForUtilizationJpaRepository.findAllByUtilizationCycleId(4);
        softly.assertThat(stocks).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/stocks-for-utilization/3/db-state.xml")
    void findAllByUtilizationCycleIdAndSkuId() {
        Set<Long> ids = stocksForUtilizationJpaRepository.findAllByUtilizationCycleIdAndSkuId(1, 1).stream()
                .map(StocksForUtilization::getId)
                .collect(Collectors.toSet());
        softly.assertThat(ids).containsExactlyInAnyOrder(1L, 4L);

        List<StocksForUtilization> stocks = stocksForUtilizationJpaRepository.findAllByUtilizationCycleIdAndSkuId(1, 3);
        softly.assertThat(stocks).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/stocks-for-utilization/3/db-state.xml")
    void findAllByUtilizationCycleIdInAndSkuId() {
        Set<Long> ids = stocksForUtilizationJpaRepository.findAllByUtilizationCycleIdInAndSkuId(Set.of(1L, 2L), 1)
                .stream()
                .map(StocksForUtilization::getId)
                .collect(Collectors.toSet());
        softly.assertThat(ids).containsExactlyInAnyOrder(1L, 3L, 4L);

        List<StocksForUtilization> stocks = stocksForUtilizationJpaRepository
                .findAllByUtilizationCycleIdInAndSkuId(Set.of(1L, 2L), 3);
        softly.assertThat(stocks).isEmpty();
    }
}
