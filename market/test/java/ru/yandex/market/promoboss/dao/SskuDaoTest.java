package ru.yandex.market.promoboss.dao;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.model.MechanicsData;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.SskuData;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = SskuDao.class)
@DbUnitDataSet(
        before = "SskuDaoTest.before.csv"
)
public class SskuDaoTest extends AbstractDaoTest {

    private static final String SSKU_1 = "ssku1";
    private static final String SSKU_2 = "ssku2";

    private static final Long PROMO_ID_1 = 1000L;
    private static final Long PROMO_ID_2 = 2000L;

    private static final SskuData SSKU_DATA1 = buildSskuData(3);
    private static final SskuData SSKU_DATA2 = buildSskuData(2);

    @Autowired
    protected SskuDao sskuDao;

    @Test
    public void shouldDoNotReturnSskuAndDoNotReturnPromoIdIfNoRecordsSaved() {

        // act
        Set<String> sskuByPromoId = sskuDao.getSskuByPromoId(PROMO_ID_1);
        Set<Long> promoIdBySsku = sskuDao.getPromoIdBySsku(SSKU_1);

        // verify
        assertNotNull(sskuByPromoId);
        assertNotNull(promoIdBySsku);

        assertTrue(sskuByPromoId.isEmpty());
        assertTrue(promoIdBySsku.isEmpty());
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldReturnOneSskuAndOnePromoIdIfSavedOne.before.csv"
    )
    public void shouldReturnOneSskuAndOnePromoIdIfSavedOne() {
        // act
        Set<String> sskuByPromoId = sskuDao.getSskuByPromoId(PROMO_ID_1);
        Set<Long> promoIdBySsku = sskuDao.getPromoIdBySsku(SSKU_1);

        // verify
        assertEquals(1, sskuByPromoId.size());
        assertTrue(sskuByPromoId.contains(SSKU_1));

        assertEquals(1, promoIdBySsku.size());
        assertTrue(promoIdBySsku.contains(PROMO_ID_1));
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldReturnOneSskuAndTwoPromoIdIfSavedOneSskuForTwoPromo.before.csv"
    )
    public void shouldReturnOneSskuAndTwoPromoIdIfSavedOneSskuForTwoPromo() {
        // act
        Set<String> sskuByPromoId = sskuDao.getSskuByPromoId(PROMO_ID_1);
        Set<Long> promoIdBySsku = sskuDao.getPromoIdBySsku(SSKU_1);

        // verify
        assertEquals(1, sskuByPromoId.size());
        assertTrue(sskuByPromoId.contains(SSKU_1));

        assertEquals(2, promoIdBySsku.size());
        assertTrue(promoIdBySsku.contains(PROMO_ID_1));
        assertTrue(promoIdBySsku.contains(PROMO_ID_2));
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldReturnTwoSskuAndOnePromoIdIfSavedTwoSskuForOnePromo.before.csv"
    )
    public void shouldReturnTwoSskuAndOnePromoIdIfSavedTwoSskuForOnePromo() {

        // act
        Set<String> sskuByPromoId = sskuDao.getSskuByPromoId(PROMO_ID_1);
        Set<Long> promoIdBySsku = sskuDao.getPromoIdBySsku(SSKU_1);

        // verify
        assertEquals(2, sskuByPromoId.size());
        assertTrue(sskuByPromoId.contains(SSKU_1));
        assertTrue(sskuByPromoId.contains(SSKU_2));

        assertEquals(1, promoIdBySsku.size());
        assertTrue(promoIdBySsku.contains(PROMO_ID_1));
    }

    @Test
    @DbUnitDataSet(
            after = "SskuDaoTest.shouldInsertSskuConstraintForOneSsku.after.csv"
    )
    public void shouldInsertSskuConstraintForOneSsku() {
        sskuDao.insertSskuWithPromoId(Set.of(SSKU_1), PROMO_ID_1, SSKU_DATA1);
    }

    @Test
    @DbUnitDataSet(
            after = "SskuDaoTest.shouldInsertSskuConstraintsForTwoSskus.after.csv"
    )
    public void shouldInsertSskuConstraintsForTwoSskus() {
        sskuDao.insertSskuWithPromoId(Set.of(SSKU_1, SSKU_2), PROMO_ID_1, SSKU_DATA1);
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldDeleteSskuConstraintsWithPromoId.before.csv",
            after = "SskuDaoTest.shouldDeleteSskuConstraintsWithPromoId.after.csv"
    )
    public void shouldDeleteSskuConstraintsWithPromoId() {
        sskuDao.deleteSskuWithPromoId(Set.of(SSKU_1, SSKU_2), PROMO_ID_1);
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldDeleteSskuConstraintsWithPromoId_emptySet.before.csv",
            after = "SskuDaoTest.shouldDeleteSskuConstraintsWithPromoId_emptySet.after.csv"
    )
    public void shouldDeleteSskuConstraintsWithPromoId_emptySet() {
        sskuDao.deleteSskuWithPromoId(Collections.emptySet(), PROMO_ID_1);
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldDeleteSskuConstraintsBySsku.before.csv",
            after = "SskuDaoTest.shouldDeleteSskuConstraintsBySsku.after.csv"
    )
    public void shouldDeleteSskuConstraintsBySsku() {
        sskuDao.deletePromoIdBySsku(SSKU_1);
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldDeleteSskuConstraintsByPromoId.before.csv",
            after = "SskuDaoTest.shouldDeleteSskuConstraintsByPromoId.after.csv"
    )
    public void shouldDeleteSskuConstraintsByPromoId() {
        sskuDao.deleteSskuByPromoId(PROMO_ID_1);
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldInsertAndDeleteLongSet.csv",
            after = "SskuDaoTest.shouldInsertAndDeleteLongSet.csv"
    )
    public void shouldInsertAndDeleteLongSet() {

        // setup
        Set<String> sskus = new HashSet<>();
        int setSize = 2200;

        while (sskus.size() < setSize) {
            sskus.add(RandomStringUtils.randomNumeric(10));
        }

        // act
        sskuDao.insertSskuWithPromoId(sskus, PROMO_ID_1, SSKU_DATA1);

        // verify
        Set<String> sskuByPromoIdAfterInsert = sskuDao.getSskuByPromoId(PROMO_ID_1);

        assertEquals(setSize, sskuByPromoIdAfterInsert.size());

        // act
        sskuDao.deleteSskuWithPromoId(sskus, PROMO_ID_1);

        // verify
        Set<String> sskuByPromoIdAfterDelete = sskuDao.getSskuByPromoId(PROMO_ID_1);

        assertTrue(sskuByPromoIdAfterDelete.isEmpty());
    }

    @Test
    @DbUnitDataSet(
            before = "SskuDaoTest.shouldUpdateSskuData.before.csv",
            after = "SskuDaoTest.shouldUpdateSskuData.after.csv"
    )
    public void shouldUpdateSskuData() {
        sskuDao.updateSskuDataByPromoId(PROMO_ID_1, SSKU_DATA2);
    }

    private static SskuData buildSskuData(int cheapestAsGiftCount) {
        CheapestAsGift cheapestAsGift = new CheapestAsGift(cheapestAsGiftCount);
        MechanicsData mechanicsData = new MechanicsData(MechanicsType.CHEAPEST_AS_GIFT, cheapestAsGift);
        return new SskuData(mechanicsData);
    }
}
