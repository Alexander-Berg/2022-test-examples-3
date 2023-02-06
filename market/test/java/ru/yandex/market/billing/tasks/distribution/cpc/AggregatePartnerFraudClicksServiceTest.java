package ru.yandex.market.billing.tasks.distribution.cpc;

import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class AggregatePartnerFraudClicksServiceTest extends FunctionalTest {

    private static final LocalDate TEST_CURRENT_DATE = LocalDate.of(2020, Month.JANUARY, 1);

    @Autowired
    private AggregatePartnerFraudClicksService aggregatePartnerFraudClicksService;

    @Test
    @DbUnitDataSet(
            before = "db/AggregatePartnerFraudClicksServiceTest.test_aggregate.before.csv",
            after = "db/AggregatePartnerFraudClicksServiceTest.test_aggregate.after.csv"
    )
    void test_aggregate() {
        aggregatePartnerFraudClicksService.aggregate(TEST_CURRENT_DATE);
    }
}
