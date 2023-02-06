package ru.yandex.market.billing.tasks.distribution.cpc;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

public class PartnerFraudClicksDbDaoTest extends FunctionalTest {

    private static final LocalDate TEST_CURRENT_DATE = LocalDate.of(2020, Month.JANUARY, 1);
    private static final LocalDate JAN_1_2020 = LocalDate.of(2020, Month.JANUARY, 1);
    private static final LocalDate DEC_31_2019 = LocalDate.of(2019, Month.DECEMBER, 31);

    @Autowired
    private PartnerFraudClicksDbDao partnerFraudClicksDbDao;

    @Test
    @DbUnitDataSet(
            before = "db/PartnerFraudClicksDbDaoTest.test_persistClickRowIds.before.csv",
            after = "db/PartnerFraudClicksDbDaoTest.test_persistClickRowIds.after.csv"
    )
    void test_persistClickRowIds() {
        partnerFraudClicksDbDao.persistClickRowIds(List.of("rowid1", "rowid2"), TEST_CURRENT_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = "db/PartnerFraudClicksDbDaoTest.test_deleteClickRowIds.before.csv",
            after = "db/PartnerFraudClicksDbDaoTest.test_deleteClickRowIds.after.csv"
    )
    void test_deleteClickRowIds() {
        partnerFraudClicksDbDao.deleteClickRowIds(TEST_CURRENT_DATE);
    }

    @Test
    @DbUnitDataSet(before = "db/PartnerFraudClicksDbDaoTest.test_getActualClickdates.before.csv")
    void test_getActualClickdates() {
        final List<LocalDate> actualClickdates = partnerFraudClicksDbDao.getActualClickdates(TEST_CURRENT_DATE);
        assertThat(actualClickdates, containsInAnyOrder(equalTo(JAN_1_2020), equalTo(DEC_31_2019)));
    }

    @Test
    @DbUnitDataSet(
            before = "db/PartnerFraudClicksDbDaoTest.test_deleteAggregatedClicks.before.csv",
            after = "db/PartnerFraudClicksDbDaoTest.test_deleteAggregatedClicks.after.csv"
    )
    void test_deleteAggregatedClicks() {
        partnerFraudClicksDbDao.deleteAggregatedClicks(List.of(JAN_1_2020, DEC_31_2019));
    }

    @Test
    @DbUnitDataSet(
            before = "db/PartnerFraudClicksDbDaoTest.test_aggregateFraudClicks.before.csv",
            after = "db/PartnerFraudClicksDbDaoTest.test_aggregateFraudClicks.after.csv"
    )
    void test_aggregateFraudClicks() {
        partnerFraudClicksDbDao.aggregateFraudClicks(List.of(JAN_1_2020, DEC_31_2019));
    }
}
