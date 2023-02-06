package ru.yandex.market.health.configs.clickphite.defaults;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.MetricType;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricsAndSolomonSensorsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonLabelEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonSensorEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistSettingsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceReportEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SubAggregateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SubAggregateExpressionEntity;
import ru.yandex.market.health.configs.common.TableEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("MethodName")
public class ClickphiteDefaultValueResolverTest {
    private static final String DEFAULT_DATABASE = "DEFAULT_DATABASE";
    private static final Double DEFAULT_VALUE_ON_NAN = -1.0;

    private final ClickphiteDefaultValueResolver resolver =
        new ClickphiteDefaultValueResolver(DEFAULT_DATABASE, DEFAULT_VALUE_ON_NAN, false);

    private final ClickphiteConfigEntity nullConfig = new ClickphiteConfigEntity(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    private final ClickphiteConfigEntity config1 = new ClickphiteConfigEntity(
        new TableEntity("database1", "table1"),
        "filter1",
        Collections.singletonList(MetricPeriod.ONE_SEC),
        1.0,
        2L,
        new SubAggregateEntity(
            Collections.singletonList("key1"),
            Collections.singletonList(new SubAggregateExpressionEntity("name1", "expression1"))
        ),
        new GraphiteMetricsAndSolomonSensorsEntity(
            Collections.singletonList(new SplitEntity(
                "name1",
                "expression1",
                new SplitWhitelistSettingsEntity(Collections.emptyList(), null, false)
            )),
            "metricExpression1",
            MetricType.QUANTILE,
            Collections.singletonList("quantile1"),
            Collections.singletonList(new GraphiteMetricEntity(
                "name1",
                Collections.emptyList(),
                Collections.emptyList()
            )),
            null,
            Collections.singletonList(new SolomonSensorEntity(
                Collections.singletonList(new SolomonLabelEntity("name1", "expression1"))
            ))
        ),
        new StatfaceReportEntity(
            "title1",
            "report1",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        )
    );

    private final ClickphiteConfigEntity config2 = new ClickphiteConfigEntity(
        new TableEntity("database2", "table2"),
        "filter2",
        Collections.singletonList(MetricPeriod.ONE_SEC),
        1.0,
        2L,
        new SubAggregateEntity(
            Collections.singletonList("key2"),
            Collections.singletonList(new SubAggregateExpressionEntity("name2", "expression2"))
        ),
        new GraphiteMetricsAndSolomonSensorsEntity(
            Collections.singletonList(new SplitEntity("name2", "expression2", null)),
            "metricExpression2",
            MetricType.QUANTILE,
            Collections.singletonList("quantile2"),
            Collections.singletonList(new GraphiteMetricEntity(
                "name2",
                Collections.emptyList(),
                Collections.emptyList()
            )),
            null,
            Collections.singletonList(new SolomonSensorEntity(
                Collections.singletonList(new SolomonLabelEntity("name2", "expression2"))
            ))
        ),
        new StatfaceReportEntity(
            "title1",
            "report1",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        )
    );

    @Test
    void emptyConfig_emptyDefaultValues() {
        assertThat(resolver.resolveDefaults(nullConfig, nullConfig))
            .usingRecursiveComparison()
            .isEqualTo(
                new ClickphiteConfigEntity(
                    new TableEntity(DEFAULT_DATABASE, null),
                    null,
                    Collections.emptyList(),
                    DEFAULT_VALUE_ON_NAN,
                    1L,
                    null,
                    null,
                    null
                )
            );
    }

    @Test
    void emptyConfig_nonEmptyDefaultValues() {
        assertThat(resolver.resolveDefaults(nullConfig, config1))
            .usingRecursiveComparison()
            .isEqualTo(new ClickphiteConfigEntity(
                new TableEntity("database1", "table1"),
                "filter1",
                Collections.singletonList(MetricPeriod.ONE_SEC),
                1.0,
                2L,
                new SubAggregateEntity(
                    Collections.singletonList("key1"),
                    Collections.singletonList(new SubAggregateExpressionEntity("name1", "expression1"))
                ),
                null,
                null
            ));
    }

    @Test
    void nonEmptyConfig_emptyDefaultValues() {
        assertThat(resolver.resolveDefaults(config1, nullConfig))
            .usingRecursiveComparison()
            .isEqualTo(config1);
    }

    @Test
    void nonEmptyConfig_nonEmptyDefaultValues() {
        assertThat(resolver.resolveDefaults(config1, config2))
            .usingRecursiveComparison()
            .isEqualTo(config1);
    }

    @Test
    void commonSolomonLabels() {
        assertThat(resolver.resolveDefaults(
            new ClickphiteConfigEntity(
                null,
                null,
                null,
                null,
                null,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    null,
                    null,
                    null,
                    null,
                    null,
                    Arrays.asList(
                        new SolomonLabelEntity("name1", "expression1"),
                        new SolomonLabelEntity("name2", "expression21")
                    ),
                    Collections.singletonList(new SolomonSensorEntity(
                        Arrays.asList(
                            new SolomonLabelEntity("name2", "expression22"),
                            new SolomonLabelEntity("name3", "expression3")
                        )
                    ))
                ),
                null
            ),
            new ClickphiteConfigEntity(
                null,
                null,
                null,
                null,
                null,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    null,
                    null,
                    null,
                    Collections.singletonList("quantile1"),
                    null,
                    Collections.singletonList(
                        new SolomonLabelEntity("ignored", "ignored")
                    ),
                    Collections.singletonList(new SolomonSensorEntity(
                        Collections.singletonList(
                            new SolomonLabelEntity("ignored", "ignored")
                        )
                    ))
                ),
                null
            )
        ))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                new ClickphiteConfigEntity(
                    new TableEntity(DEFAULT_DATABASE, null),
                    null,
                    Collections.emptyList(),
                    DEFAULT_VALUE_ON_NAN,
                    1L,
                    null,
                    new GraphiteMetricsAndSolomonSensorsEntity(
                        Collections.emptyList(),
                        "",
                        MetricType.SIMPLE,
                        Collections.singletonList("quantile1"),
                        Collections.emptyList(),
                        null,
                        Collections.singletonList(new SolomonSensorEntity(
                            Arrays.asList(
                                new SolomonLabelEntity("name1", "expression1"),
                                new SolomonLabelEntity("name2", "expression22"),
                                new SolomonLabelEntity("name3", "expression3")
                            )
                        ))
                    ),
                    null
                )
            );
    }
}
