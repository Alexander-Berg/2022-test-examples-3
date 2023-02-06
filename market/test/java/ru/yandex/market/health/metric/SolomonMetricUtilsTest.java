package ru.yandex.market.health.metric;

import junit.framework.Assert;
import org.junit.Test;

import ru.yandex.market.health.SolomonMetricUtils;
import ru.yandex.monlib.metrics.labels.validate.StrictValidator;

public class SolomonMetricUtilsTest {

    @Test
    public void testSolomonEscaping() {
        testEscaping(
            "solomon|cluster='production'" +
                "|host='${host}'|level='${level}'|period='one_min'|project='market-tst'" +
                "|sensor='logistic_lom.tskv_warns_and_errors'|service='market-delivery--lom'"
        );
        testEscaping(
            "solomon|cluster='stable'|page='${page}'|period='one_hour'|project='market-tst'" +
                "|sensor='errors-rate.5xx'|service='market-analytics-platform--frontend'"
        );
    }

    @Test
    public void testGraphiteEscaping() {
        testEscaping(
            "graphite.one_hour.market-resources-counting.dbaas.clusters." +
                "${projectId}.${clusterType}.${instanceType}.${clusterName}.networkQuotaBytes"
        );
        testEscaping(
            "graphite.one_min.blue-market-report.external_snippet_stall_time_ms.env.ALL.loc.ALL.cluster.ALL.place.ALL"
        );
    }

    private void testEscaping(String value) {
        String escapedLabel = SolomonMetricUtils.escapeLabelValue(value);
        Assert.assertTrue(StrictValidator.SELF.isValueValid(escapedLabel));
    }
}
