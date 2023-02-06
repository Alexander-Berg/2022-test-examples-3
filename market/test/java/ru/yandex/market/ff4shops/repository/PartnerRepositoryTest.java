package ru.yandex.market.ff4shops.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.mbi.feature.model.FeatureStatus;
import ru.yandex.market.ff4shops.partner.dao.PartnerRepository;
import ru.yandex.market.ff4shops.partner.dao.model.PartnerEntity;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PartnerRepositoryTest extends FunctionalTest {

    @Autowired
    private PartnerRepository tested;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(after = "SupplierRepositoryTest.insertSuppliers.after.csv")
    void testInsert() {
        PartnerEntity entity1 = new PartnerEntity(1, 100L, true, FeatureType.MARKETPLACE, FeatureStatus.SUCCESS, true,
                true, null, true);
        PartnerEntity entity2 = new PartnerEntity(2, 100L, true, FeatureType.MARKETPLACE, FeatureStatus.NEW, true,
                false, null, false);

        transactionTemplate.execute(status -> {
            tested.insert(asList(entity1, entity2));
            return null;
        });
    }

    /**
     * Проверяем, что берутся только записи с запрошенными id и с push_stocks = true
     */
    @Test
    @DbUnitDataSet(before = "PartnerRepositoryTest.testFindAllWithPushStocks.before.csv")
    void testFindAllWithPushStocks() {
        List<Long> actualPartners = tested.findAllWithPushStocks(Set.of(1L, 2L, 3L)).stream()
                .map(PartnerEntity::getId)
                .collect(Collectors.toList());

        assertEquals(2, actualPartners.size());
        assertTrue(actualPartners.containsAll(Set.of(1L, 3L)));
    }

    /**
     * Тест простого поиска по ID
     */
    @Test
    @DbUnitDataSet(before = "PartnerRepositoryTest.testFindById.before.csv")
    void testFindById() {
        Optional<PartnerEntity> partner = tested.findById(1L);


        PartnerEntity expected = new PartnerEntity();
        expected.setId(1);
        expected.setBusinessId(1L);
        expected.setEnabled(true);
        expected.setFeatureStatus(FeatureStatus.SUCCESS);
        expected.setFeatureType(FeatureType.DROPSHIP);
        expected.setCpaPartnerInterface(true);
        expected.setPushStocks(true);
        expected.setStocksByPartnerInterface(false);

        assertThat(partner.isPresent());
        assertThat(partner.get()).isEqualTo(expected);
    }

    /**
     * Тест простого поиска по ID
     */
    @Test
    @DbUnitDataSet(before = "PartnerRepositoryTest.testFindByIdWithNullStockByPiFlag.before.csv")
    void testFindByIdWithNullStockByPiFlag() {
        Optional<PartnerEntity> partner = tested.findById(1L);


        PartnerEntity expected = new PartnerEntity();
        expected.setId(1);
        expected.setBusinessId(1L);
        expected.setEnabled(true);
        expected.setFeatureStatus(FeatureStatus.SUCCESS);
        expected.setFeatureType(FeatureType.DROPSHIP);
        expected.setCpaPartnerInterface(true);
        expected.setPushStocks(true);

        assertThat(partner.isPresent());
        assertThat(partner.get()).isEqualTo(expected);
    }

    @Test
    @DbUnitDataSet(before = "PartnerRepositoryTest.testUpdateStocksByPiFlag.before.csv",
            after = "PartnerRepositoryTest.testUpdateStocksByPiFlag.after.csv")
    void testUpdateStocksByPiFlag() {
        tested.updateStocksByPiFlag(1, true);
    }
}
