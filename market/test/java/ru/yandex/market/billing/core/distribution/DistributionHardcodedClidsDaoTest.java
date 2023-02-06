package ru.yandex.market.billing.core.distribution;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.core.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class DistributionHardcodedClidsDaoTest extends FunctionalTest {
    @Autowired
    DistributionHardcodedClidsDao dao;

    @Test
    @DbUnitDataSet(
            before = "DistributionHardcodedClidsDaoTest.before.csv",
            after = "DistributionHardcodedClidsDaoTest.upsert.after.csv")
    void testUpsert() {
        dao.upsert(List.of(
                new HardcodedClid(2L, PartnerSegment.CLOSER, "Купонный агрегатор"),
                new HardcodedClid(5L, PartnerSegment.MARKETING, "Instagram блог"),
                new HardcodedClid(6L, PartnerSegment.CLOSER, "Не определено")
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "DistributionHardcodedClidsDaoTest.before.csv",
            after = "DistributionHardcodedClidsDaoTest.delete.after.csv")
    void testDelete() {
        dao.delete(List.of(2L, 3L));
    }
}
