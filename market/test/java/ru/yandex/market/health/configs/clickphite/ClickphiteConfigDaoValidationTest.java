package ru.yandex.market.health.configs.clickphite;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GrafanaDashboardEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricsAndSolomonSensorsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonLabelEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonSensorEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceReportEntity;
import ru.yandex.market.health.configs.clickphite.spring.HealthConfigUtilsClickphiteInternalSpringConfig;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.validation.DaoActionValidationException;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ContextConfiguration(classes = {
    HealthConfigUtilsClickphiteInternalSpringConfig.class,
    ClickphiteConfigDaoValidationTest.SpringConfiguration.class
})
@TestPropertySource(properties = "clickphite.configs.dao.existingMetricIdsCacheDurationSeconds=0")
public class ClickphiteConfigDaoValidationTest {
    @Autowired
    private ClickphiteConfigDao dao;

    private static ClickphiteConfigGroupEntity config(String id) {
        return new ClickphiteConfigGroupEntity(
            id,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private static ClickphiteConfigGroupVersionEntity configVersion(String id, ClickphiteConfigEntity... configs) {
        return configVersion(
            new VersionedConfigEntity.VersionEntity.Id(id, 0L),
            configs
        );
    }

    private static ClickphiteConfigGroupVersionEntity configVersion(
        VersionedConfigEntity.VersionEntity.Id id,
        ClickphiteConfigEntity... configs
    ) {
        return new ClickphiteConfigGroupVersionEntity(
            id,
            null,
            VersionStatus.DRAFT,
            "owner1",
            null,
            Arrays.asList(configs)
        );
    }

    private static ClickphiteConfigEntity configGraphite(String metricName, String... dashboards) {
        return configGraphite(metricName, Collections.singletonList(MetricPeriod.ONE_MIN), dashboards);
    }

    private static ClickphiteConfigEntity configGraphite(String metricName, List<MetricPeriod> periods) {
        return configGraphite(metricName, periods, new String[]{});
    }

    private static ClickphiteConfigEntity configGraphite(
        String metricName, List<MetricPeriod> periods, String... dashboards
    ) {
        return new ClickphiteConfigEntity(
            new TableEntity(null, "table1"),
            null,
            periods,
            null,
            null,
            null,
            new GraphiteMetricsAndSolomonSensorsEntity(
                null,
                "metricExpression1",
                MetricType.SIMPLE,
                null,
                Collections.singletonList(new GraphiteMetricEntity(
                    metricName,
                    null,
                    Stream.of(dashboards)
                        .map(dashboardId -> new GrafanaDashboardEntity(
                            dashboardId,
                            "title",
                            null,
                            null,
                            null
                        ))
                        .collect(Collectors.toList())
                )),
                null,
                null
            ),
            null
        );
    }

    private static ClickphiteConfigEntity configSolomon(SolomonLabelEntity... labels) {
        return new ClickphiteConfigEntity(
            new TableEntity(null, "table1"),
            null,
            Collections.singletonList(MetricPeriod.ONE_MIN),
            null,
            null,
            null,
            new GraphiteMetricsAndSolomonSensorsEntity(
                null,
                "metricExpression1",
                MetricType.SIMPLE,
                null,
                null,
                null,
                Collections.singletonList(new SolomonSensorEntity(Arrays.asList(labels)))
            ),
            null
        );
    }

    private static ClickphiteConfigEntity configSolomon(MetricPeriod period, SolomonLabelEntity... labels) {
        return new ClickphiteConfigEntity(
            new TableEntity(null, "table1"),
            null,
            Collections.singletonList(period),
            null,
            null,
            null,
            new GraphiteMetricsAndSolomonSensorsEntity(
                null,
                "metricExpression1",
                MetricType.SIMPLE,
                null,
                null,
                null,
                Collections.singletonList(new SolomonSensorEntity(Arrays.asList(labels)))
            ),
            null
        );
    }

    private static ClickphiteConfigEntity configSolomon(MetricType type, SolomonLabelEntity... labels) {
        return new ClickphiteConfigEntity(
            new TableEntity(null, "table1"),
            null,
            Collections.singletonList(MetricPeriod.ONE_MIN),
            null,
            null,
            null,
            new GraphiteMetricsAndSolomonSensorsEntity(
                null,
                "metricExpression1",
                type,
                null,
                null,
                null,
                Collections.singletonList(new SolomonSensorEntity(Arrays.asList(labels)))
            ),
            null
        );
    }

    private static ClickphiteConfigEntity configStatface(String reportName) {
        return new ClickphiteConfigEntity(
            new TableEntity(null, "table1"),
            null,
            Collections.singletonList(MetricPeriod.ONE_MIN),
            null,
            null,
            null,
            null,
            new StatfaceReportEntity(
                "title",
                reportName,
                null,
                null,
                null
            )
        );
    }

    private static SolomonLabelEntity label(String name, String value) {
        return new SolomonLabelEntity(name, value);
    }

    private static SolomonLabelEntity[] projectServiceClusterSensorLabels() {
        return new SolomonLabelEntity[]{
            label("project", "a"),
            label("service", "b"),
            label("cluster", "c"),
            label("sensor", "e")
        };
    }

    @Test
    void createConfig() {
        dao.createConfig(config("id1"));
        assertThatThrownBy(() -> dao.createConfig(config("id1")))
            .isInstanceOf(DaoActionValidationException.class);
    }

    @Test
    void createVersionConfigExists() {
        dao.createConfig(config("id1"));
        dao.createValidVersion(configVersion("id1"), null);
    }

    @Test
    void createVersionConfigDoesNotExist() {
        assertThatThrownBy(() -> dao.createValidVersion(configVersion("id1"), null))
            .isInstanceOf(DaoActionValidationException.class);
    }

    @Test
    void createVersionCorrectSolomonLabels() {
        dao.createConfig(config("id1"));

        assertThatCode(
            () -> dao.createValidVersion(configVersion(
                "id1",
                configSolomon(
                    label("project", "a"),
                    label("service", "b"),
                    label("cluster", "c"),
                    label("sensor", "e")
                )
            ), null)
        ).doesNotThrowAnyException();
    }

    @Test
    void createVersionRequiredSolomonLabels() {
        dao.createConfig(config("id1"));

        assertThatThrownBy(
            () -> dao.createValidVersion(configVersion("id1", configSolomon()), null)
        )
            .isInstanceOf(DaoActionValidationException.class);
    }

    @Test
    void createVersionForbiddenSolomonLabels() {
        dao.createConfig(config("id1"));

        assertThatThrownBy(
            () -> dao.createValidVersion(configVersion(
                "id1",
                configSolomon(
                    label("project", "a"),
                    label("service", "b"),
                    label("cluster", "c"),
                    label("period", "d"),
                    label("quantile", "e")
                )
            ), null)
        )
            .isInstanceOf(DaoActionValidationException.class);
    }

    @Test
    void createVersionTooLongServiceInTesting() {
        dao.createConfig(config("id1"));

        assertThatThrownBy(
            () -> dao.createValidVersion(configVersion(
                "id1",
                configSolomon(
                    label("project", "project-project-project-project"),
                    label("service", "service-service-service-service"),
                    label("cluster", "c")
                )
            ), null)
        )
            .isInstanceOf(DaoActionValidationException.class);
    }

    @Test
    void createVersionGrafanaDashboardIdNotUnique() {
        testUniqueIdsValidation(
            "configs[0].graphiteSolomon.graphiteMetrics[0].grafanaDashboards[0].id",
            configGraphite("metric1", "dashboard1"),
            configGraphite("metric2", "dashboard1")
        );
    }

    @Test
    void createVersionGraphiteMetricNameNotUnique() {
        testUniqueIdsValidation(
            "configs[0].graphiteSolomon.graphiteMetrics[0].name",
            configGraphite("metric1")
        );
    }

    @Test
    void createVersionGraphiteMetricNameNotUniqueInSameConfigByPeriod() {
        dao.createConfig(config("id1"));
        ClickphiteConfigEntity configEntity = configGraphite("metric1", Arrays.asList(MetricPeriod.DAY,
            MetricPeriod.MONTH));

        VersionedConfigEntity.VersionEntity.Id version1 = dao.createDraft("id1", null);
        dao.saveDraft(configVersion(version1, configEntity), null);

        assertThatThrownBy(() -> dao.publishVersion(version1))
            .isInstanceOf(DaoActionValidationException.class)
            .hasMessageContaining("configs[0].graphiteSolomon.graphiteMetrics[0].name")
            .hasMessageContaining("configs[0].periods");
    }

    @Test
    void createVersionGraphiteMetricNameNotUniqueInSameConfigByPeriodInDifferentConfigGroups() {
        dao.createConfig(config("id1"));
        ClickphiteConfigEntity metric1 = configGraphite("metric1", Collections.singletonList(MetricPeriod.DAY));
        ClickphiteConfigEntity metric11 = configGraphite("metric1", Collections.singletonList(MetricPeriod.WEEK));

        VersionedConfigEntity.VersionEntity.Id version1 = dao.createDraft("id1", null);
        dao.saveDraft(configVersion(version1, metric1, metric11), null);

        assertThatThrownBy(() -> dao.publishVersion(version1))
            .isInstanceOf(DaoActionValidationException.class)
            .hasMessageContaining("configs[1].graphiteSolomon.graphiteMetrics[0].name");
    }

    @Test
    void createVersionGraphiteMetricNameNotUniqueInSameConfigByName() {
        dao.createConfig(config("id1"));
        ClickphiteConfigEntity metric1 = configGraphite("metric1", Collections.singletonList(MetricPeriod.ONE_MIN));

        VersionedConfigEntity.VersionEntity.Id version1 = dao.createDraft("id1", null);
        dao.saveDraft(configVersion(version1, metric1, metric1), null);

        assertThatThrownBy(() -> dao.publishVersion(version1))
            .isInstanceOf(DaoActionValidationException.class)
            .hasMessageContaining("configs[1].graphiteSolomon.graphiteMetrics[0].name");
    }

    @Test
    void createVersionSolomonLabelsNotUniqueInSameConfigByName() {
        dao.createConfig(config("id1"));
        ClickphiteConfigEntity metric1 = configSolomon(
            label("project", "a"),
            label("service", "b"),
            label("cluster", "c")
        );

        VersionedConfigEntity.VersionEntity.Id version1 = dao.createDraft("id1", null);
        dao.saveDraft(configVersion(version1, metric1, metric1), null);

        assertThatThrownBy(() -> dao.publishVersion(version1))
            .isInstanceOf(DaoActionValidationException.class)
            .hasMessageContaining("configs[1].graphiteSolomon.solomonSensors[0].labels");
    }

    @Test
    void createVersionSolomonLabelsNotUniqueInSameConfigPeriodDiffers() {
        dao.createConfig(config("id1"));
        ClickphiteConfigEntity metric1 = configSolomon(
            MetricPeriod.FIVE_MIN,
            projectServiceClusterSensorLabels()
        );
        ClickphiteConfigEntity metric2 = configSolomon(
            MetricPeriod.ONE_MIN,
            projectServiceClusterSensorLabels()
        );

        VersionedConfigEntity.VersionEntity.Id version1 = dao.createDraft("id1", null);
        dao.saveDraft(configVersion(version1, metric1, metric2), null);
        dao.publishVersion(version1);
    }

    @Test
    void createVersionSolomonLabelsNotUniqueInSameConfigTypeDiffers() {
        dao.createConfig(config("id1"));
        ClickphiteConfigEntity metric1 = configSolomon(
            MetricType.SIMPLE,
            projectServiceClusterSensorLabels()
        );
        ClickphiteConfigEntity metric2 = configSolomon(
            MetricType.QUANTILE_TIMING,
            projectServiceClusterSensorLabels()
        );

        VersionedConfigEntity.VersionEntity.Id version1 = dao.createDraft("id1", null);
        dao.saveDraft(configVersion(version1, metric1, metric2), null);
        dao.publishVersion(version1);
    }

    @Test
    void createVersionSolomonSensorLabelsNotUnique() {
        testUniqueIdsValidation(
            "configs[0].graphiteSolomon.solomonSensors[0].labels",
            configSolomon(
                projectServiceClusterSensorLabels()
            )
        );
    }

    @Test
    void createVersionStatfaceReportNamesNotUnique() {
        testUniqueIdsValidation(
            "configs[0].statface.report",
            configStatface("report1")
        );
    }

    private void testUniqueIdsValidation(
        String fieldName,
        ClickphiteConfigEntity config
    ) {
        testUniqueIdsValidation(fieldName, config, config);
    }

    private void testUniqueIdsValidation(
        String fieldName,
        ClickphiteConfigEntity config1,
        ClickphiteConfigEntity config2
    ) {
        dao.createConfig(config("id1"));
        dao.createConfig(config("id2"));
        dao.createConfig(config("id3"));

        // Два черновика с одинаковыми id для разных конфигов создаются без проблем потому что это черновики
        VersionedConfigEntity.VersionEntity.Id version11 = dao.createDraft("id1", null);
        VersionedConfigEntity.VersionEntity.Id version21 = dao.createDraft("id2", null);
        dao.saveDraft(configVersion(version11, config1), null);
        dao.saveDraft(configVersion(version21, config2), null);

        // Черновики публикуются без проблем, потому что ещё нет активного конфига, который занимает этот id
        dao.publishVersion(version11);
        dao.publishVersion(version21);

        // Одна опубликованная версия активируется без проблем
        dao.activateVersion(version11, null);

        // Другие версии уже не могут активироваться, потому что id занят первым конфигом
        assertThatThrownBy(() -> dao.activateVersion(version21, null))
            .isInstanceOf(DaoActionValidationException.class)
            .hasMessageContaining(fieldName);

        // Новый черновик с таким же id создать можно
        VersionedConfigEntity.VersionEntity.Id version22 = dao.createDraft("id2", null);
        dao.saveDraft(configVersion(version22, config2), null);

        // Но опубликовать уже нельзя
        assertThatThrownBy(() -> dao.publishVersion(version22))
            .isInstanceOf(DaoActionValidationException.class)
            .hasMessageContaining(fieldName);

        // Исключение - если делаем новую версию того конфига, который сейчас занимает id
        VersionedConfigEntity.VersionEntity.Id version12 = dao.createDraft("id1", null);
        dao.saveDraft(configVersion(version12, config1), null);
        dao.publishVersion(version12);
        dao.activateVersion(version12, null);
    }

    @PropertySource("classpath:/test.properties")
    static class SpringConfiguration {
        @Bean
        public MongoTemplate clickphiteConfigsMongoTemplate() {
            return new MongoTemplate(
                new MongoClient(
                    new ServerAddress(
                        new MongoServer(new MemoryBackend()).bind())
                ),
                "db"
            );
        }
    }
}
