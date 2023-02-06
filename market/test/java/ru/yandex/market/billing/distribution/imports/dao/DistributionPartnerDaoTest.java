package ru.yandex.market.billing.distribution.imports.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.distribution.share.ClidInfoCache;
import ru.yandex.market.billing.distribution.share.model.DistributionPartner;
import ru.yandex.market.billing.distribution.share.model.DistributionPartnerSegment;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionPartnerDaoTest extends FunctionalTest {

    @Autowired
    private DistributionPartnerDao distributionPartnerDao;

    @Test
    @DbUnitDataSet(
            before = "DistributionPartnerDaoTest.testGetClidsInfoCache.before.csv"
    )
    void testGetClidsInfoCache() {
        ClidInfoCache clidsInfoCache = new ClidInfoCache(distributionPartnerDao);


        assertTrue(clidsInfoCache.getClid(101).isPresent());
        assertEquals(clidsInfoCache.getClid(101).get().getPartnerSegment(), DistributionPartnerSegment.CLOSER);

        assertTrue(clidsInfoCache.getClid(102).isPresent());
        assertEquals(clidsInfoCache.getClid(102).get().getPartnerSegment(), DistributionPartnerSegment.MARKETING);
        assertEquals(clidsInfoCache.getClid(102).get().getPlaceType(), "Instagram блог");

        assertTrue(clidsInfoCache.getClid(103).isPresent());
        assertNull(clidsInfoCache.getClid(103).get().getPartnerSegment());

        assertFalse(clidsInfoCache.getClid(110).isPresent());
    }

    @Test
    @DbUnitDataSet(
            before = "DistributionPartnerDaoTest.testDeleteByClids.before.csv",
            after = "DistributionPartnerDaoTest.testDeleteByClids.after.csv")
    void testDeleteByClids() {
        distributionPartnerDao.deleteByClids(Set.of(102L, 103L, 110L));
    }

    @Test
    @DbUnitDataSet(
            before = "DistributionPartnerDaoTest.before.csv",
            after = "DistributionPartnerDaoTest.upsert.after.csv")
    void testUpsert() {
        distributionPartnerDao.upsert(List.of(
                DistributionPartner.builder()
                        .setClid(1L)
                        .setClidTypeId(34)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setHasContract(true)
                        .setManagerLogin("robot")
                        .setUrl("vk.com/hello")
                        .setStatus(3)
                        .build(),
                DistributionPartner.builder()
                        .setClid(2503777L)
                        .setClidType("Партнерские виджеты")
                        .setClidTypeId(34)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPayable(false)
                        .setPackId(47459)
                        .setPackDomain("vk.com/beautyfashion2021")
                        .setPackComment("Старая автогенерация")
                        .setPackCreateDate(LocalDate.of(2021, 10, 2))
                        .setSetId(398047)
                        .setSoftId(1033)
                        .setUserLogin("ekaterina-kuzina-81")
                        .setPlaceType("Группа в соцсети")
                        .setName("Мода")
                        .setUrl("vk.com/beautyfashion2021")
                        .setUniqueVisitors("500-1000")
                        .setStatus(0)
                        .setManagerLogin("system-robot")
                        .setHasContract(true)
                        .build())
        );
    }


}
