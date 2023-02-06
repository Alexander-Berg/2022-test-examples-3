package ru.yandex.market.billing.distribution.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.distribution.share.model.DistributionReferral;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

public class DistributionReferralDaoTest extends FunctionalTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2022, 7, 26, 12, 57);

    @Autowired
    private DistributionReferralDao dao;

    @Test
    @DbUnitDataSet(
            before = "DistributionReferralDaoTest.before.csv",
            after = "DistributionReferralDaoTest.upsert.after.csv")
    public void testUpsertEntries() {
        dao.upsertEntries(List.of(
                new DistributionReferral("user-one", 1L, TIME),
                new DistributionReferral("user-three", 1L, TIME.plusHours(1)),
                new DistributionReferral("user-four", 5L, TIME.plusHours(2))
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "DistributionReferralDaoTest.before.csv",
            after = "DistributionReferralDaoTest.drop.after.csv")
    public void testDropEntries() {
        dao.dropEntries(Set.of("user-one"));
    }

    @Test
    @DbUnitDataSet(
            before = "DistributionReferralDaoTest.before.csv",
            after = "DistributionReferralDaoTest.before.csv")
    public void testDropEmpty() {
        dao.dropEntries(Set.of());
    }

    @Test
    @DbUnitDataSet(before = "DistributionReferralDaoTest.before.csv")
    public void testGetAllUserLogins() {
        assertThat(dao.getAllUserLogins())
                .containsExactlyInAnyOrder("user-one", "user-two", "user-eleven");
    }
}
