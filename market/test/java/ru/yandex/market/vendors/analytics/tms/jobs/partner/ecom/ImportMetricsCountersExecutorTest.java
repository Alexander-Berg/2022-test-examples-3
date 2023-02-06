package ru.yandex.market.vendors.analytics.tms.jobs.partner.ecom;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.MetricsCounter;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Тест для джобы {@link ImportMetricsCountersExecutor}.
 *
 * @author ogonek
 */
public class ImportMetricsCountersExecutorTest extends FunctionalTest {

    @Autowired
    private YtTableReader<MetricsCounter> ytMetricsCounterReader;

    @Autowired
    private ImportMetricsCountersExecutor importMetricsCountersExecutor;

    @Test
    @DbUnitDataSet(before = "ImportMetricsExecutorTest.before.csv", after = "ImportMetricsExecutorTest.after.csv")
    void importMetricsCountersExecutorTest() {
        reset(ytMetricsCounterReader);
        when(ytMetricsCounterReader.loadInfoFromYtTable()).thenReturn(
                ImmutableList.of(
                        new MetricsCounter(998, "metrika", 1000),
                        new MetricsCounter(999, "metrika", 1000),
                        new MetricsCounter(999, "metrika", 1002)
                )
        );
        importMetricsCountersExecutor.doJob(null);
    }
}
