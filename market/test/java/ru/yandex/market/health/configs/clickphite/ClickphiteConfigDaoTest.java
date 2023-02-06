package ru.yandex.market.health.configs.clickphite;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.clickhouse.ShortTableDefinition;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricsAndSolomonSensorsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonLabelEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SolomonSensorEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceReportEntity;
import ru.yandex.market.health.configs.clickphite.spring.HealthConfigUtilsClickphiteInternalSpringConfig;
import ru.yandex.market.health.configs.common.ConfigDaoTestBase;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {
    HealthConfigUtilsClickphiteInternalSpringConfig.class,
    ClickphiteConfigDaoValidationTest.SpringConfiguration.class
})
@TestPropertySource(properties = "clickphite.clickhouse.db=test")
public class ClickphiteConfigDaoTest extends ConfigDaoTestBase<ClickphiteConfigGroupEntity,
    ClickphiteConfigGroupVersionEntity> {
    private static final String ENTITY_WITH_DEFAULT_TABLE_ID = "entityWithDefaultTableId";
    private static final String SEARCHING_LABEL_1 = "search_1";
    private static final String SEARCHING_LABEL_2 = "search_2";
    private static final String SEARCHING_GRAPHITE_METRIC = "test_metric";
    private static final String SEARCHING_STATFACE_REPORT = "test_report";

    private ClickphiteConfigGroupEntity searchingEntity = new ClickphiteConfigGroupEntity(
        searchingId,
        null,
        searchingTitle,
        null,
        Instant.now(),
        Instant.now(),
        null,
        null
    );

    private ClickphiteConfigGroupVersionEntity searchingEntityVersion = new ClickphiteConfigGroupVersionEntity(
        new VersionedConfigEntity.VersionEntity.Id(searchingId, 0L),
        VersionedConfigSource.UI,
        VersionStatus.PUBLIC,
        "test_owner",
        new ClickphiteConfigEntity(
            new TableEntity("test", searchingTable),
            null,
            Collections.emptyList(),
            null,
            null,
            null,
            new GraphiteMetricsAndSolomonSensorsEntity(
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Arrays.asList(
                    new SolomonLabelEntity(SEARCHING_LABEL_1, "test_1"),
                    new SolomonLabelEntity("project", "test"),
                    new SolomonLabelEntity("cluster", "test"),
                    new SolomonLabelEntity("service", "test")
                ),
                Collections.emptyList()
            ),
            null
        ),
        Arrays.asList(
            new ClickphiteConfigEntity(
                null,
                null,
                Collections.singletonList(MetricPeriod.ONE_MIN),
                null,
                0L,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    Collections.emptyList(),
                    "count()",
                    MetricType.SIMPLE,
                    null,
                    Collections.emptyList(),
                    null,
                    Collections.singletonList(
                        new SolomonSensorEntity(Collections.singletonList(
                            new SolomonLabelEntity(SEARCHING_LABEL_2, "test_2")
                        ))
                    )
                ),
                null
            ),
            new ClickphiteConfigEntity(
                null,
                null,
                Collections.singletonList(MetricPeriod.ONE_MIN),
                null,
                0L,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    null,
                    "count()",
                    MetricType.SIMPLE,
                    Collections.emptyList(),
                    Collections.singletonList(new GraphiteMetricEntity(
                        SEARCHING_GRAPHITE_METRIC,
                        Collections.emptyList(),
                        Collections.emptyList()
                    )),
                    null,
                    Collections.emptyList()
                ),
                null
            ),
            new ClickphiteConfigEntity(
                null,
                null,
                Collections.singletonList(MetricPeriod.ONE_MIN),
                null,
                0L,
                null,
                null,
                new StatfaceReportEntity(
                    "test",
                    SEARCHING_STATFACE_REPORT,
                    Collections.emptyList(),
                    null,
                    Collections.emptyList()
                )
            )
        )
    );

    private ClickphiteConfigGroupEntity notSearchingEntity = new ClickphiteConfigGroupEntity(
        notSearchingId,
        null,
        "wrongTitle",
        null,
        Instant.now(),
        Instant.now(),
        null,
        null
    );

    private ClickphiteConfigGroupVersionEntity notSearchingVersion = new ClickphiteConfigGroupVersionEntity(
        new VersionedConfigEntity.VersionEntity.Id(notSearchingId, 0L),
        VersionedConfigSource.UI,
        VersionStatus.PUBLIC,
        "test_owner",
        new ClickphiteConfigEntity(
            new TableEntity("test", "wrongTable"),
            null,
            Collections.emptyList(),
            null,
            null,
            null,
            new GraphiteMetricsAndSolomonSensorsEntity(
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Arrays.asList(
                    new SolomonLabelEntity("wrongLabel_1", "test_3"),
                    new SolomonLabelEntity("project", "test"),
                    new SolomonLabelEntity("cluster", "test"),
                    new SolomonLabelEntity("service", "test")
                ),
                null
            ),
            null
        ),
        Arrays.asList(
            new ClickphiteConfigEntity(
                null,
                null,
                Collections.singletonList(MetricPeriod.ONE_MIN),
                null,
                0L,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    Collections.emptyList(),
                    "count()",
                    MetricType.SIMPLE,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    null,
                    Collections.singletonList(
                        new SolomonSensorEntity(Collections.singletonList(
                            new SolomonLabelEntity("wrongLabel_2", "test_2")
                        ))
                    )
                ),
                null
            ),
            new ClickphiteConfigEntity(
                null,
                null,
                Collections.singletonList(MetricPeriod.ONE_MIN),
                null,
                0L,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    null,
                    "count()",
                    MetricType.SIMPLE,
                    null,
                    Collections.singletonList(new GraphiteMetricEntity(
                        "wrongMetric",
                        Collections.emptyList(),
                        Collections.emptyList()
                    )),
                    null,
                    Collections.emptyList()
                ),
                null
            ),
            new ClickphiteConfigEntity(
                null,
                null,
                Collections.singletonList(MetricPeriod.ONE_MIN),
                null,
                0L,
                null,
                null,
                new StatfaceReportEntity(
                    "test",
                    "wrongReport",
                    Collections.emptyList(),
                    null,
                    Collections.emptyList()
                )
            )
        )
    );

    private ClickphiteConfigGroupEntity entityWithDefaultTable = new ClickphiteConfigGroupEntity(
        ENTITY_WITH_DEFAULT_TABLE_ID,
        null,
        null,
        null,
        Instant.now(),
        Instant.now(),
        null,
        null
    );

    private ClickphiteConfigGroupVersionEntity versionWithDefaultTable = new ClickphiteConfigGroupVersionEntity(
        new VersionedConfigEntity.VersionEntity.Id(ENTITY_WITH_DEFAULT_TABLE_ID, 0L),
        VersionedConfigSource.UI,
        VersionStatus.PUBLIC,
        "test_owner",
        new ClickphiteConfigEntity(
            new TableEntity(null, searchingTable),
            null,
            Collections.emptyList(),
            null,
            null,
            null,
            new GraphiteMetricsAndSolomonSensorsEntity(
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Arrays.asList(
                    new SolomonLabelEntity("wrongLabel_1", "test_3"),
                    new SolomonLabelEntity("project", "test"),
                    new SolomonLabelEntity("cluster", "test"),
                    new SolomonLabelEntity("service", "test")
                ),
                null
            ),
            null
        ),
        Collections.emptyList()
    );

    @BeforeAll
    public void setUp() {
        setUp(searchingEntity, searchingEntityVersion, notSearchingEntity, notSearchingVersion);
    }

    @Test
    public void findByMetricName() {
        testFindConfigs("graphiteMetric", SEARCHING_GRAPHITE_METRIC);
    }

    @Test
    public void findByMetricLabel() {
        testFindConfigs("solomonLabelName", SEARCHING_LABEL_1);
        testFindConfigs("solomonLabelName", SEARCHING_LABEL_2);
    }

    @Test
    public void findByStatfaceReport() {
        testFindConfigs("statface", SEARCHING_STATFACE_REPORT);
    }

    @Test
    public void getActiveConfigsByTable() {
        dao.createConfig(entityWithDefaultTable);
        dao.createValidVersion(versionWithDefaultTable, "user42");
        dao.publishVersion(new VersionedConfigEntity.VersionEntity.Id(ENTITY_WITH_DEFAULT_TABLE_ID, 0L));
        dao.activateVersion(new VersionedConfigEntity.VersionEntity.Id(ENTITY_WITH_DEFAULT_TABLE_ID, 0L), null);

        List<ClickphiteConfigGroupEntity> activeConfigsByTable = ((ClickphiteConfigDao) dao).getActiveConfigsByTable(
            new ShortTableDefinition(
                searchingTable, "test"
            )
        );

        assertThat(activeConfigsByTable.size()).isEqualTo(2);
        assertThat(activeConfigsByTable.stream().map(VersionedConfigEntity::getId)).contains(searchingId,
            ENTITY_WITH_DEFAULT_TABLE_ID);

        dao.deactivate(ENTITY_WITH_DEFAULT_TABLE_ID);
    }
}
