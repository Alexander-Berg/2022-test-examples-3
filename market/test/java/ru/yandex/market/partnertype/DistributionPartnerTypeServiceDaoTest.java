package ru.yandex.market.partnertype;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;


public class DistributionPartnerTypeServiceDaoTest extends FunctionalTest {
    private static final LocalDate DATE = LocalDate.of(2021, 4, 15);

    @Autowired
    private DistributionPartnerTypeServiceDao dao;

    @Test
    @DbUnitDataSet(
            before = "DistributionPartnerTypeServiceDaoTest.testSave.before.csv",
            after = "DistributionPartnerTypeServiceDaoTest.testSave.after.csv"
    )
    public void testSave() {
        dao.savePartnerData(List.of(
                new DistributionPartnerTypeData(1L, 11L, 101L, DistributionPartnerTypeData.PartnerType.INDIVIDUAL_SELF_EMPLOYED, DistributionPartnerTypeData.ResidenceStatus.RESIDENT),
                new DistributionPartnerTypeData(2L, 21L, 201L, DistributionPartnerTypeData.PartnerType.OTHER, DistributionPartnerTypeData.ResidenceStatus.RESIDENT),
                new DistributionPartnerTypeData(3L, 31L, 301L, DistributionPartnerTypeData.PartnerType.INDIVIDUAL_SELF_EMPLOYED, DistributionPartnerTypeData.ResidenceStatus.RESIDENT),
                new DistributionPartnerTypeData(4L, 41L, 402L, DistributionPartnerTypeData.PartnerType.INDIVIDUAL_SELF_EMPLOYED, DistributionPartnerTypeData.ResidenceStatus.NON_RESIDENT),
                new DistributionPartnerTypeData(5L, 51L, 501L, DistributionPartnerTypeData.PartnerType.OTHER, DistributionPartnerTypeData.ResidenceStatus.NON_RESIDENT)),
                DATE);
    }

}
