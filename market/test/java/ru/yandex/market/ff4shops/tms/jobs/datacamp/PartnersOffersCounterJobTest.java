package ru.yandex.market.ff4shops.tms.jobs.datacamp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;

public class PartnersOffersCounterJobTest extends FunctionalTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    @DbUnitDataSet(before = "partnerOffersCounterJobTest.before.csv",
    after = "partnerOffersCounterJobTest.after.csv")
    public void doJob() {
        var partnersOffersCounterJob = new PartnersOffersCounterJob(
                transactionTemplate, namedParameterJdbcTemplate);
        partnersOffersCounterJob.doJob(null);
    }
}
