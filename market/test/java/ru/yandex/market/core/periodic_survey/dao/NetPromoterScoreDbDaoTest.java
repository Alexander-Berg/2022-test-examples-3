package ru.yandex.market.core.periodic_survey.dao;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.model.PartnerPlacementType;

@DbUnitDataSet(before = "NetPromoterScoreDbDaoTest.csv")
class NetPromoterScoreDbDaoTest extends FunctionalTest {

    // NOW - 180 = 2021-03-17T20:37:00Z
    // NOW - 60 = 2021-07-15T20:37:00
    private static final Instant NOW = Instant.parse("2021-09-13T20:37:00Z");

    @Autowired
    NetPromoterScoreDbDao netPromoterScoreDbDao;

    @Test
    void testGetNextBatchDropship() {
        List<Long> dropships = netPromoterScoreDbDao.getNextPartnersBatch(107, 3,
                Set.of(PartnerPlacementType.DROPSHIP), NOW);
        Assertions.assertThat(dropships.size()).isEqualTo(2);
        Assertions.assertThat(dropships).containsExactlyInAnyOrder(114L, 117L);

        // явно проверяем, что 118 партнер с ever_activated = 0 не попадает в выборку
        List<Long> dropships2 = netPromoterScoreDbDao.getNextPartnersBatch(117, 3,
                Set.of(PartnerPlacementType.DROPSHIP), NOW);
        Assertions.assertThat(dropships2).isEmpty();
    }

    @Test
    void testGetNextBatchFby() {
        List<Long> fbys = netPromoterScoreDbDao.getNextPartnersBatch(101, 3,
                Set.of(PartnerPlacementType.CROSSDOCK, PartnerPlacementType.FULFILLMENT), NOW);
        Assertions.assertThat(fbys).isEqualTo(List.of(112L));
    }

    @Test
    void testGetNextBatchDbs() {
        List<Long> dbs = netPromoterScoreDbDao.getNextPartnersBatch(100, 3,
                Set.of(PartnerPlacementType.DROPSHIP_BY_SELLER, PartnerPlacementType.CLICK_AND_COLLECT), NOW);
        Assertions.assertThat(dbs).isEqualTo(List.of(108L));
    }
}
