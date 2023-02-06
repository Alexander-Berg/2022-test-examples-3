package ru.yandex.market.sc.core.domain.warehouse.repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.util.LocalCacheName.WAREHOUSE_BY_ID;
import static ru.yandex.market.sc.core.test.TestFactory.warehouse;

/**
 * @author valter
 */
@ActiveProfiles({TplProfiles.TESTS, "cache"})
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class WarehouseRepositoryTest {
    private final WarehouseRepository warehouseRepository;
    private final CacheManager cacheManager;

    @Test
    void save() {
        var expected = warehouse();
        assertThat(warehouseRepository.save(expected)).isEqualTo(expected);
    }

    @Test
    void testCache() {
        var warehouse = warehouse();
        long id = warehouseRepository.save(warehouse).getId();
        var expected = warehouseRepository.findByIdOrThrow(id);
        var cachedWarehouse = getCachedWarehouse(id);
        assertThat(cachedWarehouse).isPresent();
        assertThat(cachedWarehouse.get()).isEqualTo(expected);
    }

    private Optional<Warehouse> getCachedWarehouse(long id) {
        return Optional.ofNullable(cacheManager.getCache(WAREHOUSE_BY_ID)).map(c -> c.get(id, Warehouse.class));
    }


}
