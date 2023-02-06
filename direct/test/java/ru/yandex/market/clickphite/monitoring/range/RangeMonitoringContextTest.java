package ru.yandex.market.clickphite.monitoring.range;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clickphite.config.metric.GraphiteMetricConfig;
import ru.yandex.market.clickphite.config.monitoring.MonitoringConfig;
import ru.yandex.market.clickphite.graphite.GraphiteClient;
import ru.yandex.market.clickphite.graphite.Metric;
import ru.yandex.market.clickphite.monitoring.MonitoringService;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 22/01/16
 */
public class RangeMonitoringContextTest {
    private MonitoringService monitoringService;

    @Before
    public void setUp() {
        GraphiteClient graphiteClient = new GraphiteClient();
        graphiteClient.setHosts("market-graphite.yandex-team.ru");

        monitoringService = new MonitoringService();
        monitoringService.setGraphiteClient(graphiteClient);
        monitoringService.setMonitoringUrlPath("/render?width=800&height=600&from=-4hours&target=");
    }

    @Test
    public void onMetric() throws MalformedURLException {
        Gson gson = new Gson();

        MonitoringConfig monitoringConfig = gson.fromJson(
            "{\n" +
                "    'name': 'tsum.nginx.timings.ALL.95',\n" +
                "    'title': 'Тайминги ЦУМа',\n" +
                "    'quantile': 0.95,\n" +
                "    'metric': 'tsum.nginx.timings.ALL',\n" +
                "    'range': {\n" +
                "        'critTop': 2000\n" +
                "    }\n" +
                "}",
            MonitoringConfig.class
        );

        GraphiteMetricConfig graphiteMetricConfig = gson.fromJson(
            "{\n" +
                "    'metricName': 'tsum.nginx.timings.ALL',\n" +
                "    'period': 'ONE_MIN',\n" +
                "    'metricField': 'resptime_ms',\n" +
                "    'type': 'QUANTILE_TIMING'\n" +
                "}",
            GraphiteMetricConfig.class
        );


        RangeMonitoringContext sut = new RangeMonitoringContext(
            graphiteMetricConfig,
            monitoringConfig,
            monitoringService,
            null
        );

        int timeStampSeconds = 1475585400;

        // убеждаемся, что критический статус выставится, если метрика мигнёт на одну минуту выше критического уровня
        sut.onMetric(new Metric(timeStampSeconds, 60, ""));
        sut.onMetric(new Metric(timeStampSeconds + 60, 3000.12345678, ""));
        sut.onMetric(new Metric(timeStampSeconds + 120, 1, ""));

        MonitoringStatus currentStatus = sut.getCurrentStatus();

        assertEquals(MonitoringStatus.CRITICAL, currentStatus);

        assertEquals("CRIT: value <= 2000.0\n", sut.getMetricString());
        assertEquals("[{ 15:51:00: 3,000.1235 }]", sut.getCauseString());

        URL expectedUrl = new URL("https://market-graphite.yandex-team.ru/render?width=800&height=600&from=-4hours&target=one_min.tsum.nginx.timings.ALL.0_95");
        assertEquals(expectedUrl, sut.getMetricUrl());
    }

    @Test
    public void testFullMetricString() {
        Gson gson = new Gson();

        MonitoringConfig monitoringConfig = gson.fromJson(
                "{\n" +
                        "    'name': 'market-front.dynamic-timings.95',\n" +
                        "    'title': 'Тайминги Фронт',\n" +
                        "    'quantile': 0.95,\n" +
                        "    'metric': 'market-front.timings-dynamic',\n" +
                        "    'range': {\n" +
                        "        'critTop': 2000,\n" +
                        "        'critBottom': 1000,\n" +
                        "        'warnTop': 2000,\n" +
                        "        'warnBottom': 1000\n" +
                        "    }\n" +
                        "}",
                MonitoringConfig.class
        );

        GraphiteMetricConfig graphiteMetricConfig = gson.fromJson(
                "{\n" +
                        "    'metricName': 'market-front.timings-dynamic',\n" +
                        "    'period': 'ONE_SEC',\n" +
                        "    'metricField': 'resptime_ms',\n" +
                        "    'filter': \"vhost in ('market.yandex.ru', 'market.yandex.ua', 'market.yandex.by', 'market.yandex.kz') and dynamic = 1 and http_code = 200\",\n" +
                        "    'type': 'QUANTILE_TIMING'\n" +
                        "}",
                GraphiteMetricConfig.class
        );

        RangeMonitoringContext sut = new RangeMonitoringContext(
            graphiteMetricConfig,
                monitoringConfig,
                monitoringService,
                null
        );

        Assert.assertEquals("WARN: value >= 1000.0, value <= 2000.0; CRIT: value >= 1000.0, value <= 2000.0\n", sut.getMetricString());
    }
}
