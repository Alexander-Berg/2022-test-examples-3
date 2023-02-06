package ru.yandex.market.mbi.partner.status.partner.dao;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner.status.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.status.model.PartnerInfo;

public class PartnerDaoTest extends AbstractFunctionalTest {
    @Autowired
    private PartnerDao partnerDao;

    @Test
    @DbUnitDataSet(after = "partnerDao.save.after.csv")
    void testSave() {
            partnerDao.saveBatch(List.of(new PartnerInfo(1L, 1L),
                new PartnerInfo(1L, 2L),
                new PartnerInfo(1L, 3L),
                new PartnerInfo(2L, 3L)
        ));
        partnerDao.saveBatch(List.of(new PartnerInfo(2L, 3L)));
    }

    @Test
    @DbUnitDataSet(before = "partnerDao.save.after.csv")
    void testGetByBusiness() {
        MatcherAssert.assertThat(partnerDao.getPartnersByBusiness(100L), Matchers.empty());

        MatcherAssert.assertThat(partnerDao.getPartnersByBusiness(1L), Matchers.equalTo(
                        List.of(new PartnerInfo(1L, 1L), new PartnerInfo(1L, 2L))
                )
        );
    }

}
