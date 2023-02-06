package ru.yandex.market.clickphite.config.storage.json;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.MetricType;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricsAndSolomonSensorsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonLabelEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonSensorEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceGraphEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceReportEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceSplitOrFieldEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SubAggregateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SubAggregateExpressionEntity;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.statface.StatfaceField;

import static org.assertj.core.api.Assertions.assertThat;

public class ClickphiteConfigFileJsonLoaderTest {

    @SuppressWarnings("MethodLength")
    @Test
    public void allFields() {
        check("allFields", new ClickphiteConfigGroupVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("allFields", -1L),
            VersionedConfigSource.CODE,
            null,
            "owner1",
            new ClickphiteConfigEntity(
                new TableEntity(
                    null,
                    "table1"
                ),
                "defaultFilter1",
                null,
                null,
                null,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    Collections.singletonList(new SplitEntity(
                        "split1",
                        "expression1",
                        null
                    )),
                    null,
                    null,
                    null,
                    null,
                    Collections.singletonList(new SolomonLabelEntity(
                        "label1", "value1"
                    )),
                    null
                ),
                null
            ),
            Arrays.asList(
                new ClickphiteConfigEntity(
                    new TableEntity(
                        null,
                        "table2"
                    ),
                    "filter1",
                    Arrays.asList(MetricPeriod.ONE_MIN, MetricPeriod.FIVE_MIN),
                    42.0,
                    5L,
                    new SubAggregateEntity(
                        Collections.singletonList("key1"),
                        Collections.singletonList(new SubAggregateExpressionEntity(
                            "name1",
                            "expression1"
                        ))
                    ),
                    new GraphiteMetricsAndSolomonSensorsEntity(
                        null,
                        "metricExpression1",
                        MetricType.SIMPLE,
                        Collections.singletonList("quantile1"),
                        Arrays.asList(
                            new GraphiteMetricEntity(
                                "metricName1",
                                Collections.emptyList(),
                                Collections.emptyList()
                            ),
                            new GraphiteMetricEntity(
                                "metricName2",
                                Collections.emptyList(),
                                Collections.emptyList()
                            )
                        ),
                        null,
                        null
                    ),
                    null
                ),
                new ClickphiteConfigEntity(
                    new TableEntity(
                        null,
                        "table2"
                    ),
                    "filter1",
                    Arrays.asList(MetricPeriod.ONE_MIN, MetricPeriod.FIVE_MIN),
                    42.0,
                    5L,
                    new SubAggregateEntity(
                        Collections.singletonList("key1"),
                        Collections.singletonList(new SubAggregateExpressionEntity(
                            "name1",
                            "expression1"
                        ))
                    ),
                    new GraphiteMetricsAndSolomonSensorsEntity(
                        null,
                        "metricExpression1",
                        MetricType.SIMPLE,
                        Collections.singletonList("quantile1"),
                        null,
                        null,
                        Arrays.asList(
                            new SolomonSensorEntity(Collections.singletonList(new SolomonLabelEntity(
                                "label2", "value2"
                            ))),
                            new SolomonSensorEntity(Collections.singletonList(new SolomonLabelEntity(
                                "label3", "value3"
                            )))
                        )
                    ),
                    null
                ),
                new ClickphiteConfigEntity(
                    new TableEntity(
                        null,
                        "table3"
                    ),
                    "filter1",
                    Arrays.asList(MetricPeriod.DAY, MetricPeriod.WEEK),
                    42.0,
                    5L,
                    new SubAggregateEntity(
                        Collections.singletonList("key1"),
                        Collections.singletonList(new SubAggregateExpressionEntity(
                            "name1",
                            "expression1"
                        ))
                    ),
                    null,
                    new StatfaceReportEntity(
                        "title1",
                        "report1",
                        Collections.singletonList(new StatfaceSplitOrFieldEntity(
                            "name1",
                            "expression1",
                            "title1",
                            true,
                            StatfaceField.ViewType.String,
                            42,
                            null
                        )),
                        Collections.singletonList(new StatfaceSplitOrFieldEntity(
                            "name2",
                            "expression2",
                            "title2",
                            false,
                            StatfaceField.ViewType.Number,
                            24,
                            null
                        )),
                        Collections.singletonList(new StatfaceGraphEntity(
                            "title1",
                            Collections.singletonList("field1"),
                            "type1"
                        ))
                    )
                )
            )
        ));
    }

    private static void check(String dir, ClickphiteConfigGroupVersionEntity expected) {
        String configDir = ResourceUtils.getResourcePath(dir);
        assertThat(new ClickphiteConfigFileJsonLoader(configDir).load())
            .hasSize(1)
            .first().isEqualToComparingFieldByFieldRecursively(expected);
    }
}
