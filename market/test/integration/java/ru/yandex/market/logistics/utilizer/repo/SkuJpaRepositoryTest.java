package ru.yandex.market.logistics.utilizer.repo;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.entity.Sku;

public class SkuJpaRepositoryTest extends AbstractContextualTest {
    @Autowired
    SkuJpaRepository skuJpaRepository;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/sku/1/db-state.xml")
    void findById() {
        Sku result = skuJpaRepository.findById(1L).get();

        softly.assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/sku/1/db-state.xml")
    void findBySkuAndWarehouseIdAndVendorId() {
        Sku result = skuJpaRepository
                .findBySkuAndWarehouseIdAndVendorId("sku", 172, 100500).get();
        softly.assertThat(result.getId()).isEqualTo(1);

        Optional<Sku> maybeSku = skuJpaRepository
                .findBySkuAndWarehouseIdAndVendorId("sku", 172, 100501);
        softly.assertThat(maybeSku).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/repo/sku/1/db-state.xml")
    void lockSku() {
        Sku result = skuJpaRepository.lockSku("sku", 172, 100500).get();
        softly.assertThat(result.getId()).isEqualTo(1);

        Optional<Sku> maybeSku = skuJpaRepository.lockSku("sku", 172, 100501);
        softly.assertThat(maybeSku).isEmpty();
    }

}
