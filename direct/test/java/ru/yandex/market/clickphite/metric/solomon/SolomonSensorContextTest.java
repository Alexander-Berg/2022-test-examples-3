package ru.yandex.market.clickphite.metric.solomon;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.clickphite.ClickHouseTable;
import ru.yandex.market.clickphite.config.ConfigFile;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;
import ru.yandex.market.clickphite.config.metric.MetricPeriod;
import ru.yandex.market.clickphite.metric.MetricResultRow;
import ru.yandex.market.clickphite.solomon.SolomonShardId;
import ru.yandex.market.clickphite.solomon.dto.SolomonSensor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 25.07.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class SolomonSensorContextTest {
    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void twoSensorsWithCommonLabels() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "commonLabels",
            row(ImmutableMap.of(), 1, 2.34),
            row(ImmutableMap.of(), 2, 6.78)
        );

        assertThat(requests).hasSize(1);

        assertThat(requests.get(0).getShardId())
            .extracting(SolomonShardId::getProject, SolomonShardId::getService, SolomonShardId::getCluster)
            .containsExactly("myProject", "myService", "myCluster");

        assertThat(requests.get(0).getCommonLabels())
            .containsOnly(entry("sensor", "myCommonLabelValue"));

        // Лейбл period переместился в поле labels каждого сенсора чтобы поле labels не было пустым.
        // Это багофича Соломона, подробнее в SolomonSensorIdTemplate.
        assertThat(requests.get(0).getSensors())
            .extracting(SolomonSensor::getLabels, SolomonSensor::getKind, SolomonSensor::getTimestamp, SolomonSensor::getValue)
            .containsOnly(
                tuple(ImmutableMap.of("period", MetricPeriod.ONE_MIN.getGraphiteName()), SolomonSensor.Kind.DGAUGE, 1L, 2.34),
                tuple(ImmutableMap.of("period", MetricPeriod.ONE_MIN.getGraphiteName()), SolomonSensor.Kind.DGAUGE, 2L, 6.78)
            );
    }

    @Test
    public void twoSensorsWithDifferentLabels() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "differentLabels",
            row(ImmutableMap.of("mySplit", "value1"), 1, 2.34),
            row(ImmutableMap.of("mySplit", "value2"), 2, 6.78)
        );

        assertThat(requests).hasSize(1);

        // А здесь period никуда не перемещается, потому что поле labels каждого сенсора и так не пусто
        assertThat(requests.get(0).getCommonLabels())
            .containsExactly(entry("period", MetricPeriod.ONE_MIN.getGraphiteName()));

        assertThat(requests.get(0).getSensors())
            .extracting(SolomonSensor::getLabels, SolomonSensor::getTimestamp)
            .containsOnly(
                tuple(ImmutableMap.of("sensor", "value1"), 1L),
                tuple(ImmutableMap.of("sensor", "value2"), 2L)
            );
    }

    @Test
    public void twoSensorsWithDifferentShards() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "differentShards",
            row(ImmutableMap.of("mySplit", "value1"), 1, 2.34),
            row(ImmutableMap.of("mySplit", "value2"), 2, 6.78)
        );

        assertThat(requests)
            .extracting(PushSolomonSensorsRequest::getShardId)
            .contains(
                new SolomonShardId("myProject", "myService", "value1"),
                new SolomonShardId("myProject", "myService", "value2")
            );

        assertThat(requests.get(0).getSensors()).hasSize(1);
        assertThat(requests.get(1).getSensors()).hasSize(1);
    }

    @Test
    public void infValueSkip() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "commonLabels",
            row(ImmutableMap.of(), 1, Double.NEGATIVE_INFINITY),
            row(ImmutableMap.of(), 1, Double.POSITIVE_INFINITY)
        );
        assertThat(requests).isEmpty();
    }

    @Test
    public void nanWithoutValueOnNan() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "commonLabels",
            row(ImmutableMap.of(), 1, Double.NaN)
        );

        assertThat(requests).isEmpty();
    }

    @Test
    public void nanWithValueOnNan() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "valueOnNan",
            row(ImmutableMap.of("mySplit", "value1"), 1, Double.NaN)
        );

        assertThat(requests.get(0).getSensors())
            .extracting(SolomonSensor::getValue)
            .containsOnly(0.0);
    }

    @Test
    public void quantileSensors() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "quantile",
            quantileRow(ImmutableMap.of(), 1, 1.0, 2.0, 4.0)
        );

        assertThat(requests).hasSize(1);

        assertThat(requests.get(0).getSensors())
            .extracting(SolomonSensor::getLabels, SolomonSensor::getTimestamp, SolomonSensor::getValue)
            .containsOnly(
                tuple(ImmutableMap.of("quantile", "0.5"), 1L, 1.0),
                tuple(ImmutableMap.of("quantile", "0.9"), 1L, 2.0),
                tuple(ImmutableMap.of("quantile", "1"), 1L, 4.0)
            );
    }

    @Test
    public void labelQuantileInConfig() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "labelQuantileInConfig",
            quantileRow(ImmutableMap.of(), 1, 1.0, 2.0, 4.0)
        );

        assertThat(requests).hasSize(1);

        assertThat(requests.get(0).getSensors())
            .extracting(SolomonSensor::getLabels, SolomonSensor::getTimestamp, SolomonSensor::getValue)
            .containsOnly(
                tuple(ImmutableMap.of("quantile", "0.5"), 1L, 1.0),
                tuple(ImmutableMap.of("quantile", "0.9"), 1L, 2.0),
                tuple(ImmutableMap.of("quantile", "1"), 1L, 4.0)
            );
    }

    @Test
    public void sensorBatches() throws Exception {
        List<PushSolomonSensorsRequest> requests = createRequests(
            "commonLabels",
            Collections.nCopies(15000, row(ImmutableMap.of(), 1, 2.34)).stream()
                .toArray(MetricResultRow[]::new)
        );

        assertThat(requests).hasSize(2);
    }

    private List<PushSolomonSensorsRequest> createRequests(String config, MetricResultRow... rows) throws IOException {
        ConfigFile configFile = new ConfigFile(new File(
            "src/test/resources/solomon_sensor_context_test/" + config + ".json"
        ));
        configurationService.parseAndCheck(configFile);
        return new SolomonSensorContext(
            configFile.getSolomonSensorConfigs().get(0),
            ClickHouseTable.create("table", "database")
        )
            .createPushRequestsFromResultRows(Arrays.asList(rows));
    }

    private static MetricResultRow row(Map<String, String> splits, int timestamp, double value) {
        MetricResultRow row = mock(MetricResultRow.class);
        when(row.getSplitValue(any())).thenAnswer(invocation -> splits.get(invocation.getArgument(0)));
        when(row.getTimestampSeconds()).thenReturn(timestamp);
        when(row.getValue()).thenReturn(value);
        return row;
    }

    private static MetricResultRow quantileRow(Map<String, String> splits, int timestamp, double... values) {
        MetricResultRow row = mock(MetricResultRow.class);
        when(row.getSplitValue(any())).thenAnswer(invocation -> splits.get(invocation.getArgument(0)));
        when(row.getTimestampSeconds()).thenReturn(timestamp);
        when(row.getQuantileValueArray()).thenReturn(values);
        return row;
    }
}
