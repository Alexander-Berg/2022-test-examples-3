package ru.yandex.cs.placement.tms.cutoff;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.placement.tms.analytics.AnalyticsUploadService;
import ru.yandex.cs.placement.tms.util.JsonArgMatcher;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Functional tests for {@link ExportAnalyticsCutoffsExecutor}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/cutoff/ExportAnalyticsCutoffsExecutorTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/cutoff/ExportAnalyticsCutoffsExecutorTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class ExportAnalyticsCutoffsExecutorTest extends AbstractCsPlacementTmsFunctionalTest {

    @Autowired
    private AnalyticsUploadService analyticsCutoffUploadService;

    @Autowired
    private ExportAnalyticsCutoffsExecutor executor;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("Выгрузка отключений вендоров для Маркет.Аналитики")
    void doJob() {
        when(clock.instant()).thenReturn(Instant.now());
        executor.doJob(null);
        String expectedJson = "" +
                "[  \n" +
                "   {  \n" +
                "      \"vendorId\":100,\n" +
                "      \"cutoffs\":[  \n" +
                "         {  \n" +
                "            \"id\":1,\n" +
                "            \"type\":6,\n" +
                "            \"fromTime\":1528578000.000000000,\n" +
                "            \"toTime\":null\n" +
                "         }\n" +
                "      ]\n" +
                "   },\n" +
                "   {  \n" +
                "      \"vendorId\":200,\n" +
                "      \"cutoffs\":[  \n" +
                "         {  \n" +
                "            \"id\":2,\n" +
                "            \"type\":6,\n" +
                "            \"fromTime\":1559336400.000000000,\n" +
                "            \"toTime\":1561928400.000000000\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "]";
        verify(analyticsCutoffUploadService, times(1))
                .upload(argThat(new JsonArgMatcher(expectedJson)));
    }
}
