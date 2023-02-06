package ru.yandex.chemodan.app.docviewer.monica;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.misc.monica.core.name.FullMetricName;
import ru.yandex.misc.monica.core.name.LocalMetricNamespace;
import ru.yandex.misc.monica.core.name.MetricName;
import ru.yandex.misc.monica.core.name.MetricNamespace;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class DvGraphitePusherConfigurationTest {

    private static final MetricNamespace NAMESPACE = new LocalMetricNamespace(
            "development", "docviewer", "web", "group", "ugr", "some.host.ru", 10023);

    private DvGraphitePusherConfiguration configuration = new DvGraphitePusherConfiguration(
            Duration.standardSeconds(5),
            "media.docviewer.monica",
            new DvGraphiteMetricsRegistry("java.gc.*"),
            "v1-0-2",
            "another_host_ru"
    );

    @Test
    public void isMetricPushEnabled() {
        FullMetricName fullMetricName = FullMetricName.Factory.consRaw(NAMESPACE, new MetricName("java", "gc", "rps"));
        Assert.isTrue(configuration.isMetricPushEnabled(fullMetricName));
        fullMetricName = FullMetricName.Factory.consRaw(NAMESPACE, new MetricName("http", "client", "rps"));
        Assert.isFalse(configuration.isMetricPushEnabled(fullMetricName));
    }

    @Test
    public void getGraphitePath() {
        FullMetricName fullMetricName = FullMetricName.Factory.consRaw(NAMESPACE, new MetricName("java", "gc", "rps"));
        String result = configuration.getGraphitePath(fullMetricName, "field-name");
        Assert.equals("media.docviewer.monica.development.ugr.another_host_ru.v1-0-2.java.gc.rps.field-name", result);
    }
}
