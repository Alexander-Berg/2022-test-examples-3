package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

class BillOfMaterialDaoTest extends IntegrationTest {

    @Autowired
    BillOfMaterialDao billOfMaterialDao;

    @Test
    void mapSerialsToComponentSkus_emptyInput() {
        Map<SkuId, Map<SkuId, List<String>>> result =
                billOfMaterialDao.mapUitsToComponentSkus(Collections.emptySet());
        assertions.assertThat(result).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/billofmaterial/before.xml")
    void mapSerialsToComponentSkus() {
        Map<SkuId, Map<SkuId, List<String>>> result =
                billOfMaterialDao.mapUitsToComponentSkus(Set.of("UIT1", "UIT2", "UIT3", "UIT4"));
        SkuId skuId = SkuId.of("STORER1", "ROV123");
        assertions.assertThat(result).containsOnlyKeys(skuId);
        assertions.assertThat(result.get(skuId))
                .containsOnlyKeys(SkuId.of("STORER1", "ROV123BOM1"), SkuId.of("STORER1", "ROV123BOM2"));
        assertions.assertThat(result.get(skuId).get(SkuId.of("STORER1", "ROV123BOM1"))).containsOnly("UIT1", "UIT3");
        assertions.assertThat(result.get(skuId).get(SkuId.of("STORER1", "ROV123BOM2"))).containsOnly("UIT2", "UIT4");
    }
}
