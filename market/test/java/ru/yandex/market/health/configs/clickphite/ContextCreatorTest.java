package ru.yandex.market.health.configs.clickphite;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.clickphite.config.metric.MetricField;
import ru.yandex.market.health.configs.clickphite.metric.MetricContext;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;
import ru.yandex.market.health.configs.clickphite.metric.graphite.SplitNotFoundException;
import ru.yandex.market.health.configs.clickphite.metric.solomon.SolomonSensorContext;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricsAndSolomonSensorsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonLabelEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonSensorEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistAutoUpdateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistSettingsEntity;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextCreatorTest {

    @Test
    public void testExtractExpressionsUnderArrayJoin() {
        assertThat(
            ContextCreator.extractExpressionsUnderArrayJoin("sumIf(parse_time_ms, status = 'SUCCESS') / sumIf" +
                "(line_count, status = 'SUCCESS') * 1000")
        ).isEqualTo(Collections.emptyList());

        assertThat(
            ContextCreator.extractExpressionsUnderArrayJoin("arrayJoin(source_names)")
        ).isEqualTo(Collections.singletonList("source_names"));

        assertThat(
            ContextCreator.extractExpressionsUnderArrayJoin("dictGetString('category', 'full_name', arrayJoin" +
                "(dictGetHierarchy('category', toUInt64(category_id))))")
        ).isEqualTo(
            Collections.singletonList("dictGetHierarchy('category', toUInt64(category_id))")
        );

        assertThat(
            ContextCreator.extractExpressionsUnderArrayJoin("send_time_millis_per_id[arrayJoin(arrayEnumerate" +
                "(send_time_millis_per_id)) AS i]")
        ).isEqualTo(
            Collections.singletonList("arrayEnumerate(send_time_millis_per_id)")
        );

        assertThat(
            ContextCreator.extractExpressionsUnderArrayJoin("arrayJoin(source_names) / arrayJoin(category_ids)")
        ).isEqualTo(
            Arrays.asList("source_names", "category_ids")
        );
    }

    @Test
    public void groupMetricContextsWhenNoSplitWhitelistSettings() throws SplitNotFoundException {
        groupMetricContextsWhenSplitWhitelistSettingsEqual(() -> null);
    }

    @Test
    public void groupMetricContextsWhenSplitWhitelistSettingsEqual() throws SplitNotFoundException {
        groupMetricContextsWhenSplitWhitelistSettingsEqual(
            () -> createSplitWhitelistSettingsEntity("some_manual_entry"));
    }

    @Test
    public void groupMetricContextsWhenSplitWhitelistSettingsDifferent() throws SplitNotFoundException {
        final String manualEntry1 = "some_manual_entry_1";
        final String manualEntry2 = "some_manual_entry_2";
        final List<MetricContextGroup> metricContextGroups = ContextCreator.groupMetricContexts(Arrays.asList(
            createMetricContext(createSplitWhitelistSettingsEntity(manualEntry1)),
            createMetricContext(createSplitWhitelistSettingsEntity(manualEntry2))
        ));
        assertThat(metricContextGroups.size()).isEqualTo(2);
        final Set<SplitWhitelistSettingsEntity> actualSplitWhitelistSettings = metricContextGroups.stream()
            .map(MetricContextGroup::getSplits)
            .peek(splits -> assertThat(splits.size()).isEqualTo(1))
            .map(splits -> splits.get(0))
            .map(MetricField::getSplitWhitelistSettingsEntity)
            .collect(Collectors.toSet());
        assertThat(
            actualSplitWhitelistSettings
        ).isEqualTo(
            ImmutableSet.of(
                createSplitWhitelistSettingsEntity(manualEntry1),
                createSplitWhitelistSettingsEntity(manualEntry2)
            )
        );
    }

    private void groupMetricContextsWhenSplitWhitelistSettingsEqual(
        Supplier<SplitWhitelistSettingsEntity> settingsEntityFactory) throws SplitNotFoundException {
        final List<MetricContextGroup> metricContextGroups = ContextCreator.groupMetricContexts(Arrays.asList(
            createMetricContext(settingsEntityFactory.get()),
            createMetricContext(settingsEntityFactory.get())
        ));
        assertThat(metricContextGroups.size()).isEqualTo(1);
        final MetricContextGroup metricContextGroup = metricContextGroups.get(0);
        assertThat(metricContextGroup.getSplits().size()).isEqualTo(1);
        assertThat(metricContextGroup.getSplits().get(0).getSplitWhitelistSettingsEntity())
            .isEqualTo(settingsEntityFactory.get());
    }

    private SplitWhitelistSettingsEntity createSplitWhitelistSettingsEntity(String manualEntry) {
        return new SplitWhitelistSettingsEntity(
            Collections.singletonList(manualEntry),
            new SplitWhitelistAutoUpdateEntity(
                1,
                2
            ),
            true
        );
    }

    private MetricContext createMetricContext(
        SplitWhitelistSettingsEntity splitWhitelistSettingsEntity
    ) throws SplitNotFoundException {
        final String splitName = "some_split_name";
        return new SolomonSensorContext(
            new ClickphiteConfigGroupVersionEntity(
                new VersionedConfigEntity.VersionEntity.Id(
                    "some_config",
                    null
                ),
                null,
                null,
                null,
                null,
                null
            ),
            new ClickphiteConfigEntity(
                new TableEntity(
                    null,
                    null
                ),
                null,
                null,
                0.0,
                0L,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    Collections.singletonList(
                        new SplitEntity(
                            splitName,
                            splitName,
                            splitWhitelistSettingsEntity
                        )
                    ),
                    "some_metric_expression",
                    MetricType.SIMPLE,
                    null,
                    null,
                    null,
                    null
                ),
                null
            ),
            null,
            MetricPeriod.ONE_MIN,
            new SolomonSensorEntity(
                Arrays.asList(
                    new SolomonLabelEntity(
                        "project",
                        "some_project"
                    ),
                    new SolomonLabelEntity(
                        "service",
                        "some_service"
                    ),
                    new SolomonLabelEntity(
                        "cluster",
                        "some_cluster"
                    ),
                    new SolomonLabelEntity(
                        splitName,
                        "${" + splitName + "}"
                    )
                )
            ),
            null,
            false,
            false,
            null,
            null
        );
    }

}
