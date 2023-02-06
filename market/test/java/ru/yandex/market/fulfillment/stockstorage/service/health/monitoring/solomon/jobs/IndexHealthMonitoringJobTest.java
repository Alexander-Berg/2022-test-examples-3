package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs;

import java.util.List;

import io.github.mfvanek.pg.model.index.DuplicatedIndexes;
import io.github.mfvanek.pg.model.index.ForeignKey;
import io.github.mfvanek.pg.model.index.Index;
import io.github.mfvanek.pg.model.index.IndexWithBloat;
import io.github.mfvanek.pg.model.index.IndexWithNulls;
import io.github.mfvanek.pg.model.index.IndexWithSize;
import io.github.mfvanek.pg.model.index.UnusedIndex;
import io.github.mfvanek.pg.model.table.Table;
import io.github.mfvanek.pg.model.table.TableWithBloat;
import io.github.mfvanek.pg.model.table.TableWithMissingIndex;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.pghealth.CloseableDatabaseHealth;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.pghealth.CloseableDatabaseHealthFactory;
import ru.yandex.solomon.sensors.labels.Labels;
import ru.yandex.solomon.sensors.registry.SensorsRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IndexHealthMonitoringJobTest {

    private static final CloseableDatabaseHealth HEALTH = mock(CloseableDatabaseHealth.class);
    private static final CloseableDatabaseHealthFactory HEALTH_FACTORY = mock(CloseableDatabaseHealthFactory.class);
    private static final SensorsRegistry REGISTRY = spy(SensorsRegistry.class);
    private static final IndexHealthMonitoringJob JOB = new IndexHealthMonitoringJob(null, null, null, HEALTH_FACTORY);

    static {
        when(HEALTH_FACTORY.create()).thenReturn(HEALTH);
    }

    @Test
    void checkHealthMonitoringDoesNotReceiveAnyValuesToSentIfEmptyResultSetsForChecks() {
        when(HEALTH.getDuplicatedIndexes()).thenReturn(List.of());
        when(HEALTH.getIntersectedIndexes()).thenReturn(List.of());
        when(HEALTH.getInvalidIndexes()).thenReturn(List.of());
        when(HEALTH.getUnusedIndexes()).thenReturn(List.of());

        when(HEALTH.getIndexesWithBloat()).thenReturn(List.of());
        when(HEALTH.getIndexesWithNullValues()).thenReturn(List.of());

        when(HEALTH.getTablesWithBloat()).thenReturn(List.of());
        when(HEALTH.getTablesWithMissingIndexes()).thenReturn(List.of());
        when(HEALTH.getForeignKeysNotCoveredWithIndex()).thenReturn(List.of());
        when(HEALTH.getTablesWithoutPrimaryKey()).thenReturn(List.of());

        JOB.fillSensors(REGISTRY);

        verifyNoMoreInteractions(REGISTRY);
    }

    @Test
    void checkHealthMonitoringReceivesAllValuesToSentIfNonEmptyResultSetsForChecks() {
        IndexWithSize ind1 = IndexWithSize.of("sample1", "dup1", 1);
        IndexWithSize ind2 = IndexWithSize.of("sample1", "dup2", 1);
        DuplicatedIndexes duplicatedIndexes = DuplicatedIndexes.of(ind1, ind2);
        when(HEALTH.getDuplicatedIndexes()).thenReturn(List.of(duplicatedIndexes));
        when(HEALTH.getIntersectedIndexes()).thenReturn(List.of(duplicatedIndexes));

        Index ind3 = IndexWithSize.of("sample2", "dup1");
        Index ind4 = IndexWithSize.of("sample2", "dup2");
        when(HEALTH.getInvalidIndexes()).thenReturn(List.of(ind3, ind4));

        UnusedIndex ind5 = UnusedIndex.of("sample3", "dup1", 1, 1);
        UnusedIndex ind6 = UnusedIndex.of("sample3", "dup2", 1, 1);
        when(HEALTH.getUnusedIndexes()).thenReturn(List.of(ind5, ind6));

        IndexWithBloat ind7 = IndexWithBloat.of("sample4", "dup1", 1, 1, 5);
        IndexWithBloat ind8 = IndexWithBloat.of("sample4", "dup2", 1, 2, 10);
        when(HEALTH.getIndexesWithBloat()).thenReturn(List.of(ind7, ind8));

        IndexWithNulls ind9 = IndexWithNulls.of("sample5", "dup1", 1, "nullableFieldName1");
        IndexWithNulls ind10 = IndexWithNulls.of("sample5", "dup2", 1, "nullableFieldName2");
        when(HEALTH.getIndexesWithNullValues()).thenReturn(List.of(ind9, ind10));

        TableWithBloat table1 = TableWithBloat.of("sample1", 1, 1, 10);
        TableWithBloat table2 = TableWithBloat.of("sample1", 1, 3, 20);
        when(HEALTH.getTablesWithBloat()).thenReturn(List.of(table1, table2));

        TableWithMissingIndex table3 = TableWithMissingIndex.of("sample2", 1, 1, 1);
        TableWithMissingIndex table4 = TableWithMissingIndex.of("sample2", 1, 1, 1);
        when(HEALTH.getTablesWithMissingIndexes()).thenReturn(List.of(table3, table4));

        ForeignKey key1 = ForeignKey.of("sample1", "constraint1", List.of("one", "second"));
        ForeignKey key2 = ForeignKey.of("sample1", "constraint2", List.of("one", "second"));
        when(HEALTH.getForeignKeysNotCoveredWithIndex()).thenReturn(List.of(key1, key2));

        Table table5 = Table.of("sample3.1", 10);
        Table table6 = Table.of("sample3.2", 15);
        when(HEALTH.getTablesWithoutPrimaryKey()).thenReturn(List.of(table5, table6));

        JOB.fillSensors(REGISTRY);

        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_DUPLICATED_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_INTERSECTED_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_UNUSED_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_INVALID_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_BLOAT_INDEXES_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_NULL_CONTAIN_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_BLOAT_TABLES_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_TABLES_MISSING_INDEXES_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_NOT_COVERED_FK_SENSOR_NAME),
                        any(Labels.class));
        verify(REGISTRY, times(1))
                .gaugeInt64(eq(IndexHealthMonitoringJob.INDEX_HEALTH_MONITOR_NO_PRIMARY_KEY_SENSOR_NAME),
                        any(Labels.class));
    }
}
