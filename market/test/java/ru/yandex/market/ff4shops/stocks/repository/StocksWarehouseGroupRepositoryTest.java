package ru.yandex.market.ff4shops.stocks.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.repository.WarehouseRepository;
import ru.yandex.market.ff4shops.stocks.model.StocksWarehouseGroupEntity;
import ru.yandex.market.ff4shops.stocks.service.StocksWarehouseGroupService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тестирует {@link StocksWarehouseGroupService}.
 */
@DbUnitDataSet(before = "StockWarehouseGroupTest.before.csv")
public class StocksWarehouseGroupRepositoryTest extends FunctionalTest {
    @Autowired
    private StocksWarehouseGroupRepository stocksWarehouseGroupRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;

    @Test
    @DbUnitDataSet(after = "StockWarehouseGroupTest.insert.after.csv")
    void testInsert() {
        StocksWarehouseGroupEntity persistedGroup = insertGroup();
        assertThat(persistedGroup)
                .returns(1L, StocksWarehouseGroupEntity::getId);

        //noinspection OptionalGetWithoutIsPresent
        assertThat(stocksWarehouseGroupRepository.findById(1L).get().getCreatedAt()).isNotNull();
    }

    @Test
    @DbUnitDataSet(after = "StockWarehouseGroupTest.update.after.csv")
    void testUpdate() {
        insertGroup();

        StocksWarehouseGroupEntity group = new StocksWarehouseGroupEntity(1L, "Группа 2", 14,
                List.of(warehouseRepository.getOne(14L), warehouseRepository.getOne(15L),
                        warehouseRepository.getOne(16L)));
        StocksWarehouseGroupEntity persistedGroup = stocksWarehouseGroupRepository.save(group);
        assertThat(persistedGroup)
                .returns(1L, StocksWarehouseGroupEntity::getId);
    }

    @Test
    void getGroupByWarehouseIdTest() {
        var actual = stocksWarehouseGroupRepository.getSecondaryWarehouseIdsInGroups(List.of(9L));
        Assertions.assertEquals(1, actual.size());
    }

    @Test
    void testGetGroupsByPartners() {
        assertThat(stocksWarehouseGroupRepository.findByPartner(List.of(110L, 111L, 113L)))
                .hasSize(1);
    }

    private StocksWarehouseGroupEntity insertGroup() {
        StocksWarehouseGroupEntity group = new StocksWarehouseGroupEntity("Группа 1", 14,
                List.of(warehouseRepository.getOne(14L), warehouseRepository.getOne(15L),
                        warehouseRepository.getOne(16L)));
        return stocksWarehouseGroupRepository.save(group);
    }
}
