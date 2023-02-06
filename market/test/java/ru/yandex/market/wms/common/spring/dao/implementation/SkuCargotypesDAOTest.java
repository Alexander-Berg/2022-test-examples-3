package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestConstructor;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuCargotype;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SkuCargotypesDAOTest extends IntegrationTest {

    private final SkuCargotypesDAO skuCargotypesDAO;

    SkuCargotypesDAOTest(SkuCargotypesDAO skuCargotypesDAO) {
        this.skuCargotypesDAO = skuCargotypesDAO;
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    void createFindDelete() {
        final SkuId skuId = new SkuId("465852", "ROV0000000000000001456");
        final SkuCargotype skuCargotype = SkuCargotype.builder()
                .storer(skuId.getStorerKey())
                .sku(skuId.getSku())
                .cargotype(100)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        skuCargotypesDAO.create(skuCargotype);

        final Set<SkuCargotype> actualSet = skuCargotypesDAO.findBySku(skuId);
        assertNotNull(actualSet);
        assertEquals(1, actualSet.size());

        final SkuCargotype actual = actualSet.iterator().next();
        assertEquals(skuCargotype, actual);
        assertNotNull(actual.getSerialKey());

        skuCargotypesDAO.deleteBySku(skuId);
        assertEquals(0, skuCargotypesDAO.findBySku(skuId).size());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection")
    void createBatchDeleteBatch() {
        final SkuId skuId = new SkuId("465852", "ROV0000000000000001456");

        final SkuCargotype skuCargoType1 = SkuCargotype.builder()
                .storer(skuId.getStorerKey())
                .sku(skuId.getSku())
                .cargotype(900)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();

        final SkuCargotype skuCargoType2 = SkuCargotype.builder()
                .storer(skuId.getStorerKey())
                .sku(skuId.getSku())
                .cargotype(985)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        Set<SkuCargotype> skuCargoTypes = new HashSet<>(Arrays.asList(skuCargoType1, skuCargoType2));

        skuCargotypesDAO.createBatch(skuCargoTypes);

        final Set<SkuCargotype> actualSet = skuCargotypesDAO.findBySku(skuId);
        assertNotNull(actualSet);
        assertEquals(2, actualSet.size());

        skuCargotypesDAO.deleteBatchBySkuCargoType(skuCargoTypes);
        assertEquals(0, skuCargotypesDAO.findBySku(skuId).size());
    }

    @Test
    void empty() {
        assertThrows(NullPointerException.class, () -> skuCargotypesDAO.create(null));
        skuCargotypesDAO.findBySku(null);
        skuCargotypesDAO.deleteBySku(null);
    }

    @Test
    void findBySkus_emptySkus() {
        assertions.assertThat(skuCargotypesDAO.findBySkus(Collections.emptySet())).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    void findBySkus_singleSku_oneCargo() {
        final SkuId skuId = new SkuId("465852", "ROV0000000000000001456");
        final SkuCargotype skuCargotype = SkuCargotype.builder()
                .storer(skuId.getStorerKey())
                .sku(skuId.getSku())
                .cargotype(100)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        skuCargotypesDAO.create(skuCargotype);
        Map<SkuId, List<SkuCargotype>> cargos = skuCargotypesDAO.findBySkus(Sets.newHashSet(skuId));
        assertions.assertThat(cargos.size()).isEqualTo(1);
        assertions.assertThat(cargos.get(skuId).get(0).getCargotype()).isEqualTo(100);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    void findBySkus_singleSku_multipleCargos() {
        final SkuId skuId = new SkuId("465852", "ROV0000000000000001456");
        final SkuCargotype skuCargotype = SkuCargotype.builder()
                .storer(skuId.getStorerKey())
                .sku(skuId.getSku())
                .cargotype(100)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        final SkuCargotype skuCargotype1 = SkuCargotype.builder()
                .storer(skuId.getStorerKey())
                .sku(skuId.getSku())
                .cargotype(120)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        skuCargotypesDAO.create(skuCargotype);
        skuCargotypesDAO.create(skuCargotype1);
        Map<SkuId, List<SkuCargotype>> cargos = skuCargotypesDAO.findBySkus(Sets.newHashSet(skuId));
        assertions.assertThat(cargos.size()).isEqualTo(1);
        assertions.assertThat(cargos.get(skuId).size()).isEqualTo(2);
        assertions.assertThat(cargos.get(skuId))
                .anySatisfy(cargo -> assertions.assertThat(cargo.getCargotype()).isEqualTo(100));
        assertions.assertThat(cargos.get(skuId))
                .anySatisfy(cargo -> assertions.assertThat(cargo.getCargotype()).isEqualTo(120));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    void findBySkus_multipleSku_singleCargo() {
        final SkuId skuId = new SkuId("465852", "ROV0000000000000001456");
        final SkuId skuId1 = new SkuId("465852", "ROV0000000000000001459");
        final SkuCargotype skuCargotype = SkuCargotype.builder()
                .storer(skuId.getStorerKey())
                .sku(skuId.getSku())
                .cargotype(100)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        final SkuCargotype skuCargotype1 = SkuCargotype.builder()
                .storer(skuId1.getStorerKey())
                .sku(skuId1.getSku())
                .cargotype(120)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        skuCargotypesDAO.create(skuCargotype);
        skuCargotypesDAO.create(skuCargotype1);
        Map<SkuId, List<SkuCargotype>> cargos = skuCargotypesDAO.findBySkus(Sets.newHashSet(skuId, skuId1));
        assertions.assertThat(cargos.size()).isEqualTo(2);
        assertions.assertThat(cargos.get(skuId))
                .hasOnlyOneElementSatisfying(cargo -> assertions.assertThat(cargo.getCargotype()).isEqualTo(100));
        assertions.assertThat(cargos.get(skuId1))
                .hasOnlyOneElementSatisfying(cargo -> assertions.assertThat(cargo.getCargotype()).isEqualTo(120));
    }
}
