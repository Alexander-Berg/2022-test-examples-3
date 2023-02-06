package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.List;
import java.util.Set;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuExistenceRepositoryImplTest extends MdmBaseDbTestClass {

    @Autowired
    private SskuExistenceRepository repository;
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(6545654L);
    }

    @Test
    public void testInsertMarkers() {
        // given
        ShopSkuKey interestingKey1 = key();
        ShopSkuKey interestingKey2 = key();
        ShopSkuKey interestingKey3 = key();
        repository.markExistence(List.of(key(), key(), interestingKey1, key(), key()), true);
        repository.markExistence(List.of(interestingKey2, key(), key()), true);
        repository.markExistence(List.of(key(), key(), interestingKey3), true);

        ShopSkuKey nonExistentKey1 = key();
        ShopSkuKey nonExistentKey2 = key();

        // when
        Set<ShopSkuKey> retained = repository.retainExisting(
            List.of(interestingKey1, interestingKey2, interestingKey3, nonExistentKey1, nonExistentKey2));

        // then
        Assertions.assertThat(retained).containsExactlyInAnyOrder(
            interestingKey1,
            interestingKey2,
            interestingKey3
        );
    }

    @Test
    public void testRemoveMarkers() {
        // given
        ShopSkuKey interestingKey1 = key();
        ShopSkuKey interestingKey2 = key();
        ShopSkuKey interestingKey3 = key();
        ShopSkuKey keyToRemove1 = key();
        ShopSkuKey keyToRemove2 = key();
        var keys = List.of(
            interestingKey1, interestingKey2, interestingKey3, keyToRemove1, keyToRemove2
        );
        repository.markExistence(keys, true);

        // precondition
        Assertions.assertThat(repository.retainExisting(keys)).containsExactlyInAnyOrderElementsOf(keys);

        // when
        repository.markExistence(List.of(keyToRemove1, keyToRemove2), false);

        // then
        Assertions.assertThat(repository.retainExisting(keys)).containsExactlyInAnyOrder(
            interestingKey1, interestingKey2, interestingKey3
        );
    }

    @Test
    public void testEmpties() {
        repository.markExistence(List.of(), true);
        repository.markExistence(List.of(), false);
        Assertions.assertThat(repository.retainExisting(List.of())).isEmpty();
    }

    @Test
    public void testSameUpdates() {
        var key = key();
        repository.markExistence(key, true);
        repository.markExistence(key, true);
        repository.markExistence(key, true);
        Assertions.assertThat(repository.retainExisting(List.of(key))).containsExactly(key);
        repository.markExistence(key, false);
        repository.markExistence(key, false);
        repository.markExistence(key, false);
        Assertions.assertThat(repository.retainExisting(List.of(key))).isEmpty();
    }

    private ShopSkuKey key() {
        return random.nextObject(ShopSkuKey.class);
    }
}
