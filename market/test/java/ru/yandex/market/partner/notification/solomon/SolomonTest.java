package ru.yandex.market.partner.notification.solomon;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.web.solomon.pull.SolomonUtils;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.monlib.metrics.Metric;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.primitives.GaugeInt64;
import ru.yandex.monlib.metrics.registry.MetricId;

public class SolomonTest extends AbstractFunctionalTest {
    @Autowired
    NotificationSolomonMetricDao notificationSolomonMetricDao;

    @Autowired
    TransactionTemplate transactionTemplate;

    /**
     * Проверка, что /solomon-jvm отвечает корректно
     */
    @Test
    void solomonTestOk() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(
                "http://localhost:8080" + "/solomon-jvm",
                HttpMethod.GET,
                String.class
        );
        Assertions.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
    }

    @Test
    @DbUnitDataSet(before = "solomon.metric.task.before.csv")
    void testMetricsTask() {
        long lastTimeStart = notificationSolomonMetricDao.getTimestampAndLock(SolomonMetricTask.SOLOMON_METRIC_LAST_UPDATE_TIME_ENV);
        new SolomonMetricTask(notificationSolomonMetricDao, transactionTemplate).execute();
        Metric notificationSuccessCount = SolomonUtils.getMetricRegistry().getMetric(new MetricId("notification_success_count",
                Labels.of("transport_type", "EMAIL")));
        Metric notificationNewCount = SolomonUtils.getMetricRegistry().getMetric(new MetricId("notification_new_count",
                Labels.of("transport_type", "EMAIL")));
        Metric notificationPrepareErrCount = SolomonUtils.getMetricRegistry().getMetric(new MetricId("notification_prepare_err_count",
                Labels.of("transport_type", "EMAIL")));
        Metric notificationErrCount = SolomonUtils.getMetricRegistry().getMetric(new MetricId("notification_err_count",
                Labels.of("transport_type", "EMAIL")));

        Assertions.assertTrue(EqualsBuilder.reflectionEquals(notificationSuccessCount, new GaugeInt64(1)));
        Assertions.assertTrue(EqualsBuilder.reflectionEquals(notificationNewCount, new GaugeInt64(2)));
        Assertions.assertTrue(EqualsBuilder.reflectionEquals(notificationPrepareErrCount, new GaugeInt64(0)));
        Assertions.assertTrue(EqualsBuilder.reflectionEquals(notificationErrCount, new GaugeInt64(0)));
        long lastTimeFinish = notificationSolomonMetricDao.getTimestampAndLock(SolomonMetricTask.SOLOMON_METRIC_LAST_UPDATE_TIME_ENV);
        Assertions.assertTrue(lastTimeFinish > lastTimeStart);
    }
}
