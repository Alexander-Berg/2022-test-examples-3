package ru.yandex.market.sc.tms.metric;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.tms.metric.MetricQuery;
import ru.yandex.market.tpl.common.tms.metric.MetricQueryTaskService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

/**
 * @author valter
 */
@EmbeddedDbTmsTest
class MonitoringMetricQueryExecutorTest {

    @Autowired
    MetricQueryTaskService metricQueryTaskService;
    @Autowired
    MonitoringMetricQueryExecutor monitoringMetricQueryExecutor;
    @Autowired
    TestFactory testFactory;

    SortingCenter sortingCenter;
    Logger mockLog;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        mockLog = TestFactory.mockScMetricsLogger();
    }

    @AfterEach
    void tearDown() {
        TestFactory.unmockScMetricsLogger();
    }

    @Test
    void testMetricQueriesValid() {
        List<MetricQuery> metricQueries = metricQueryTaskService.repository().findAll();
        assertThat(metricQueries).isNotEmpty();
        for (MetricQuery metricQuery : metricQueries) {
            monitoringMetricQueryExecutor.doJob(null, metricQuery);
            verify(mockLog, atLeast(0)).info(anyString());
        }
    }

}
