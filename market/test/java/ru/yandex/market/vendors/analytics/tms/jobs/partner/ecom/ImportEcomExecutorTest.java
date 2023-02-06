package ru.yandex.market.vendors.analytics.tms.jobs.partner.ecom;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.EcomCounter;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для джобы {@link ImportEcomExecutor}.
 *
 * @author sergeymironov
 */
class ImportEcomExecutorTest extends FunctionalTest {

    @Autowired
    private YtTableReader<EcomCounter> ytEcomCounterReader;

    @Autowired
    private ImportEcomExecutor importEcomExecutor;

    @Test
    @DbUnitDataSet(before = "ImportEcomExecutorTest.before.csv", after = "ImportEcomExecutorTest.after.csv")
    void importEcomCountersExecutorTestOld() {
        reset(ytEcomCounterReader);
        when(ytEcomCounterReader.loadInfoFromYtTable()).thenReturn(
                ImmutableList.of(
                        new EcomCounter("metrika", 1000, "domain_0.ru", 998),
                        new EcomCounter("metrika", 1000, "domain_1.ru", 999),
                        new EcomCounter("metrika", 1002, "domain_2.ru", 999)
                )
        );
        importEcomExecutor.doJob(null);
    }
}
