package ru.yandex.market.logistics.utilizer.repo;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;
import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.entity.Sku;
import ru.yandex.market.logistics.utilizer.domain.entity.Stock;
import ru.yandex.market.logistics.utilizer.domain.enums.StockType;

public class StockJpaRepositoryTest extends AbstractContextualTest {
    @Autowired
    StockJpaRepository stockJpaRepository;

    @Test
    public void persistAndLoadTest() {
        Sku newSku = Sku.builder()
                .warehouseId(172)
                .vendorId(100500L)
                .sku("sku")
                .build();

        Stock newStock = Stock.builder()
                .sku(newSku)
                .lastUpdate(LocalDateTime.now())
                .stockType(StockType.FIT)
                .build();

        var result = stockJpaRepository.save(newStock);
        var persistedStock = stockJpaRepository.findById(result.getId()).get();

        softly.assertThat(result).isNotNull();
        softly.assertThat(persistedStock).isNotNull();
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup(value = "classpath:fixtures/repo/stock/1/db-state.xml")
    public void findAllBySkuIdInWithSkuFetched() {
        List<Stock> actual = stockJpaRepository.findAllBySkuIdInWithSkuFetched(Set.of(1L, 3L));
        Map<Long, Stock> stockById = actual.stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));
        softly.assertThat(stockById).hasSize(2);
        Stock firstStock = stockById.get(1L);
        Stock secondStock = stockById.get(2L);
        softly.assertThat(firstStock).isNotNull();
        softly.assertThat(secondStock).isNotNull();
        softly.assertThat(firstStock.getStockType()).isEqualTo(StockType.FIT);
        softly.assertThat(secondStock.getStockType()).isEqualTo(StockType.DEFECT);
        Sku firstSku = firstStock.getSku();
        Sku secondSku = secondStock.getSku();
        softly.assertThat(firstSku.getSku()).isEqualTo("sku1");
        softly.assertThat(secondSku.getSku()).isEqualTo("sku3");
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/stock/2/db-state.xml")
    public void findAllBySkuIdInAndStockType() {
        Set<Long> stockIds = stockJpaRepository
                .findAllBySkuIdAndStockTypes(1, EnumSet.of(StockType.DEFECT, StockType.EXPIRED)).stream()
                .map(Stock::getId)
                .collect(Collectors.toSet());
        softly.assertThat(stockIds).containsExactlyInAnyOrder(2L, 3L);

        List<Stock> stocks = stockJpaRepository.findAllBySkuIdAndStockTypes(3, EnumSet.of(StockType.EXPIRED));
        softly.assertThat(stocks).isEmpty();
    }
}
