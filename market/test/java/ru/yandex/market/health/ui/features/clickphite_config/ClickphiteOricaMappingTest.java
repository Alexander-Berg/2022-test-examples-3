package ru.yandex.market.health.ui.features.clickphite_config;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.MetricType;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GrafanaDashboardEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricsAndSolomonSensorsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.JugglerMonitoringEntity;
import ru.yandex.market.health.configs.clickphite.mongo.JugglerMonitoringRangeEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonLabelEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonSensorEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistAutoUpdateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistSettingsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceGraphEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceReportEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceSplitOrFieldEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SubAggregateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SubAggregateExpressionEntity;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.ui.config.internal.OrikaMappingConfig;
import ru.yandex.market.health.ui.features.clickphite_config.view_model.ClickphiteConfigGroupViewModel;
import ru.yandex.market.statface.StatfaceField;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OrikaMappingConfig.class)
public class ClickphiteOricaMappingTest {
    @Autowired
    private MapperFacade oricaMapperFacade;

    @Test
    public void roundTrip() {
        ClickphiteConfigEntity configEntity = new ClickphiteConfigEntity(
            new TableEntity(
                "database",
                "table"
            ),
            "defaultFilter",
            Collections.singletonList(MetricPeriod.ONE_MIN),
            123.0,
            4L,
            new SubAggregateEntity(
                Collections.singletonList("key"),
                Collections.singletonList(new SubAggregateExpressionEntity(
                    "name",
                    "expression"
                ))
            ),
            new GraphiteMetricsAndSolomonSensorsEntity(
                Collections.singletonList(new SplitEntity(
                    "split",
                    "expression",
                    new SplitWhitelistSettingsEntity(
                        Arrays.asList("1", "2", "3"),
                        new SplitWhitelistAutoUpdateEntity(
                            7,
                            500
                        ),
                        true
                    )
                )),
                "expression",
                MetricType.SIMPLE,
                Collections.singletonList("0.5"),
                Collections.singletonList(new GraphiteMetricEntity(
                    "name",
                    Collections.singletonList(new JugglerMonitoringEntity(
                        "name",
                        "title",
                        "metric",
                        null,
                        new JugglerMonitoringRangeEntity(1.0, 2.0, 3.0, 4.0),
                        Collections.singletonList("group"),
                        "0.5",
                        6L,
                        7L,
                        8L
                    )),
                    Collections.singletonList(new GrafanaDashboardEntity(
                        "id",
                        "title",
                        Collections.singletonList("sort"),
                        3,
                        Collections.singletonList("tag")
                    ))
                )),
                Collections.singletonList(new SolomonLabelEntity(
                    "label", "value"
                )),
                Collections.singletonList(new SolomonSensorEntity(
                    Collections.singletonList(new SolomonLabelEntity(
                        "name",
                        "expression"
                    ))
                ))
            ),
            new StatfaceReportEntity(
                "title",
                "report",
                Collections.singletonList(new StatfaceSplitOrFieldEntity(
                    "name", "expression", "title", true, StatfaceField.ViewType.String, 4,
                    new SplitWhitelistSettingsEntity(Arrays.asList("A", "B"), null, false)
                )),
                Collections.singletonList(new StatfaceSplitOrFieldEntity(
                    "name", "expression", "title", true, StatfaceField.ViewType.Number, 4, null
                )),
                Collections.singletonList(new StatfaceGraphEntity(
                    "title",
                    Collections.singletonList("field"),
                    "type"
                ))
            )
        );

        ClickphiteConfigGroupEntity original = new ClickphiteConfigGroupEntity(
            "id",
            "test",
            "title",
            "description",
            Instant.now(),
            Instant.now(),
            new ClickphiteConfigGroupVersionEntity(
                new VersionedConfigEntity.VersionEntity.Id("id", -1L),
                VersionedConfigSource.CODE,
                VersionStatus.PUBLIC,
                "owner",
                configEntity,
                Collections.singletonList(configEntity)
            ),
            null
        );

        ClickphiteConfigGroupEntity mapped = oricaMapperFacade.map(
            oricaMapperFacade.map(original, ClickphiteConfigGroupViewModel.class),
            original.getClass()
        );

        assertThat(mapped).usingRecursiveComparison().isEqualTo(original);
    }
}
