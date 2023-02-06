package ru.yandex.market.clickphite.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.clickphite.config.validation.context.ConfigValidationException;
import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.MetricType;
import ru.yandex.market.health.configs.clickphite.config.metric.MetricField;
import ru.yandex.market.health.configs.clickphite.config.metric.MetricSplit;
import ru.yandex.market.health.configs.clickphite.config.metric.SolomonSensorConfig;
import ru.yandex.market.health.configs.clickphite.metric.MetricStorage;
import ru.yandex.market.health.configs.clickphite.metric.graphite.SplitNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 25.07.2018
 */
public class SolomonSensorsParsingTest {
    private static final String DEFAULT_DATABASE = "DEFAULT_DATABASE";

    private static final String TABLE_NAME = "table_name";
    private static final String FULL_TABLE_NAME = DEFAULT_DATABASE + "." + TABLE_NAME;
    private static final MetricSplit SPLIT1 = new MetricSplit("name1", "field1");
    private static final MetricSplit SPLIT2 = new MetricSplit("name2", "field2");

    private final ConfigurationService configurationService = new ConfigurationService();

    @Before
    public void setUp() {
        configurationService.setDefaultDatabase(DEFAULT_DATABASE);
    }

    @Test
    public void noSolomonSensorsInConfig() {
        assertThat(parse("noSolomonSensors")).isEmpty();
    }

    @Test
    public void minimalConfig() {
        SolomonSensorConfig actual = parseOne("minimalConfig");

        assertThat(actual.getTableName()).isEqualTo(TABLE_NAME);
        assertThat(actual.getTable().getDatabase()).isEqualTo(DEFAULT_DATABASE);
        assertThat(actual.getTable().getName()).isEqualTo(TABLE_NAME);
        assertThat(actual.getTable().getFullName()).isEqualTo(FULL_TABLE_NAME);

        assertThat(actual.getPeriod()).isEqualTo(MetricPeriod.ONE_MIN);
        assertThat(actual.getSubAggregate()).isNull();
        assertThat(actual.getFilter()).isNull();
        assertThat(actual.getValueOnNan()).isNaN();
        assertThat(actual.getFields())
            .extracting(MetricField::getName, MetricField::getField, MetricField::getType, MetricField::getQuantiles)
            .containsExactly(tuple("value", "METRIC_FIELD", MetricType.SIMPLE, Collections.emptyList()));
        assertThat(actual.getSplits()).isEmpty();
        assertThat(actual.getStorage()).isEqualTo(MetricStorage.SOLOMON);

        assertThat(actual.getLabels()).containsOnly(
            entry("project", "PROJECT"),
            entry("service", "SERVICE"),
            entry("cluster", "CLUSTER"),
            entry("sensor", "SENSOR"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );
        assertThat(actual.getMetricField()).isEqualTo("METRIC_FIELD");
        assertThat(actual.getType()).isEqualTo(MetricType.SIMPLE);
        assertThat(actual.getQuantiles()).isEmpty();
    }

    @Test
    public void overrideTableName() {
        SolomonSensorConfig actual = parseOne("overrideTableName");

        assertThat(actual.getTableName()).isEqualTo("overridden_table_name");
        assertThat(actual.getTable().getDatabase()).isEqualTo(DEFAULT_DATABASE);
        assertThat(actual.getTable().getName()).isEqualTo("overridden_table_name");
        assertThat(actual.getTable().getFullName()).isEqualTo(DEFAULT_DATABASE + ".overridden_table_name");
    }

    @Test
    public void solomonClickphiteEnvironment() {
        configurationService.setSolomonProjectOverride("testing-project");

        SolomonSensorConfig actual = parseOne("minimalConfig");

        assertThat(actual.getLabels()).containsOnly(
            entry("project", "testing-project"),
            entry("service", "PROJECT--SERVICE"),
            entry("cluster", "CLUSTER"),
            entry("sensor", "SENSOR"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );
    }

    @Test
    public void commonSolomonLabels() {
        SolomonSensorConfig actual = parseOne("commonSolomonLabels");

        assertThat(actual.getLabels()).containsOnly(
            entry("project", "PROJECT"),
            entry("service", "NOT_COMMON_SERVICE"),
            entry("cluster", "CLUSTER"),
            entry("sensor", "SENSOR"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );
    }

    @Test
    public void oneUsedAndOneUnusedSplit() {
        SolomonSensorConfig actual = parseOne("oneUsedAndOneUnusedSplit");

        assertThat(actual.getSplits()).containsExactly(SPLIT1);
    }

    @Test
    public void twoSplitsInOneLabel() {
        SolomonSensorConfig actual = parseOne("twoUsedSplitsInOneLabel");

        assertThat(actual.getSplits()).containsExactly(SPLIT1, SPLIT2);
    }

    @Test
    public void oneSplitUsedMultipleTimes() {
        SolomonSensorConfig actual = parseOne("oneSplitUsedMultipleTimes");

        assertThat(actual.getSplits()).containsExactly(SPLIT1);
    }

    @Test
    public void periodArrayWithTwoElements() {
        List<SolomonSensorConfig> actual = parse("periodArrayWithTwoElements");

        assertThat(actual).hasSize(2);

        assertThat(actual.get(0).getPeriod()).isEqualTo(MetricPeriod.ONE_MIN);
        assertThat(actual.get(0).getLabels()).containsOnly(
            entry("project", "PROJECT"),
            entry("service", "SERVICE"),
            entry("cluster", "CLUSTER"),
            entry("sensor", "SENSOR"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );

        assertThat(actual.get(1).getPeriod()).isEqualTo(MetricPeriod.FIVE_MIN);
        assertThat(actual.get(1).getLabels()).containsOnly(
            entry("project", "PROJECT"),
            entry("service", "SERVICE"),
            entry("cluster", "CLUSTER"),
            entry("sensor", "SENSOR"),
            entry("period", MetricPeriod.FIVE_MIN.getGraphiteName())
        );
    }

    @Test
    public void bothPeriodArrayAndPeriod() {
        List<SolomonSensorConfig> actual = parse("bothPeriodArrayAndPeriod");

        assertThat(actual).hasSize(1);

        assertThat(actual.get(0).getPeriod()).isEqualTo(MetricPeriod.DAY);
        assertThat(actual.get(0).getLabels()).containsOnly(
            entry("project", "PROJECT"),
            entry("service", "SERVICE"),
            entry("cluster", "CLUSTER"),
            entry("sensor", "SENSOR"),
            entry("period", MetricPeriod.DAY.getGraphiteName())
        );
    }

    @Test
    public void labelsArrayWithTwoElements() {
        List<SolomonSensorConfig> actual = parse("labelsArrayWithTwoElements");

        assertThat(actual).hasSize(2);

        assertThat(actual.get(0).getLabels()).containsOnly(
            entry("project", "PROJECT1"),
            entry("service", "SERVICE1"),
            entry("cluster", "CLUSTER1"),
            entry("sensor", "SENSOR1"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );

        assertThat(actual.get(1).getLabels()).containsOnly(
            entry("project", "PROJECT2"),
            entry("service", "SERVICE2"),
            entry("cluster", "CLUSTER2"),
            entry("sensor", "SENSOR2"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );
    }

    @Test(expected = ConfigValidationException.class)
    public void bothLabelsArrayAndLabels() {
        List<SolomonSensorConfig> actual = parse("bothLabelsArrayAndLabels");

        assertThat(actual).hasSize(1);

        assertThat(actual.get(0).getLabels()).containsOnly(
            entry("project", "PROJECT"),
            entry("service", "SERVICE"),
            entry("cluster", "CLUSTER"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );
    }

    @Test
    public void bothLabelsArrayAndPeriodArray() {
        List<SolomonSensorConfig> actual = parse("bothLabelsArrayAndPeriodArray");

        assertThat(actual).hasSize(4);

        assertThat(actual.get(0).getLabels()).containsOnly(
            entry("project", "PROJECT1"),
            entry("service", "SERVICE1"),
            entry("cluster", "CLUSTER1"),
            entry("sensor", "SENSOR1"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );

        assertThat(actual.get(1).getLabels()).containsOnly(
            entry("project", "PROJECT2"),
            entry("service", "SERVICE2"),
            entry("cluster", "CLUSTER2"),
            entry("sensor", "SENSOR2"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );

        assertThat(actual.get(2).getLabels()).containsOnly(
            entry("project", "PROJECT1"),
            entry("service", "SERVICE1"),
            entry("cluster", "CLUSTER1"),
            entry("sensor", "SENSOR1"),
            entry("period", MetricPeriod.FIVE_MIN.getGraphiteName())
        );

        assertThat(actual.get(3).getLabels()).containsOnly(
            entry("project", "PROJECT2"),
            entry("service", "SERVICE2"),
            entry("cluster", "CLUSTER2"),
            entry("sensor", "SENSOR2"),
            entry("period", MetricPeriod.FIVE_MIN.getGraphiteName())
        );
    }

    @Test
    public void typeQuantile() {
        SolomonSensorConfig actual = parseOne("typeQuantile");

        assertThat(actual.getType()).isEqualTo(MetricType.QUANTILE);
        assertThat(actual.getQuantiles()).containsExactly("0.5", "0.9", "1");
        assertThat(actual.getFields())
            .extracting(MetricField::getName, MetricField::getField, MetricField::getType, MetricField::getQuantiles)
            .containsExactly(tuple("value", "METRIC_FIELD", MetricType.QUANTILE, Arrays.asList("0.5", "0.9", "1")));
    }

    @Test
    public void labelPeriodInConfig() {
        SolomonSensorConfig actual = parseOne("labelsClickphiteEnvironmentAndPeriodInConfig");

        assertThat(actual.getLabels()).containsOnly(
            entry("project", "PROJECT"),
            entry("service", "SERVICE"),
            entry("cluster", "CLUSTER"),
            entry("sensor", "SENSOR"),
            entry("period", MetricPeriod.ONE_MIN.getGraphiteName())
        );
    }


    private List<SolomonSensorConfig> parse(String configName) {
        ConfigFile configFile = new ConfigFile(
            ResourceUtils.getResourceFile("solomon_sensors_parsing_test/" + configName + ".json")
        );
        try {
            configurationService.parseAndCheck(configFile);
        } catch (IOException | SplitNotFoundException e) {
            throw new RuntimeException(e);
        }
        return configFile.getSolomonSensorConfigs();
    }

    private SolomonSensorConfig parseOne(String configName) {
        List<SolomonSensorConfig> actualList = parse(configName);
        assertEquals(1, actualList.size());
        return actualList.get(0);
    }
}
