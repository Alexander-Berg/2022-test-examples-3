package ru.yandex.market.logshatter.rotation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickhouse.ddl.TableName;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.rotation.DataRotationService;
import ru.yandex.market.rotation.DataRotationTask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LogshatterDataRotationServiceTest {
    @Mock
    private ConfigurationService configurationService;
    @Mock
    private DataRotationService internalDataRotationService;
    @Mock
    private ExternalDataRotationServiceFactory externalDataRotationServiceFactory;
    @Mock
    private DataRotationService externalDataRotationService;

    private LogshatterDataRotationService logshatterDataRotationService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        logshatterDataRotationService = spy(new LogshatterDataRotationService(configurationService,
            internalDataRotationService, externalDataRotationServiceFactory));
    }

    @Test
    public void deleteObsoletePartitionsWithoutErrors() throws Exception {
        DataRotationTask internalDataRotationTask = mock(DataRotationTask.class);
        DataRotationTask externalDataRotationTask = mock(DataRotationTask.class);
        when(configurationService.getConfigWithPresentRotationDays()).thenReturn(createLogShatterConfigs());
        when(externalDataRotationServiceFactory.create("clusterId_2"))
            .thenReturn(Optional.of(externalDataRotationService));
        when(externalDataRotationServiceFactory.create("clusterId_5")).thenReturn(Optional.empty());
        when(internalDataRotationService.findObsoletePartitions(new TableName("health.test_1"), 30))
            .thenReturn(internalDataRotationTask);
        when(externalDataRotationService.findObsoletePartitions(new TableName("health.test_2"), 30))
            .thenReturn(externalDataRotationTask);

        logshatterDataRotationService.deleteObsoletePartitions();

        verifyFindObsoletePartitions();

        verify(internalDataRotationService).deleteObsoletePartitions(internalDataRotationTask);
        verify(externalDataRotationService).deleteObsoletePartitions(externalDataRotationTask);
    }

    private void verifyFindObsoletePartitions() {
        verify(internalDataRotationService).findObsoletePartitions(new TableName("health.test_1"), 30);
        verify(externalDataRotationService).findObsoletePartitions(new TableName("health.test_2"), 30);
        verify(internalDataRotationService).findObsoletePartitions(new TableName("health.test_3"), 30);
        verify(internalDataRotationService).findObsoletePartitions(new TableName("health.test_4"), 30);
        verify(externalDataRotationService, times(0)).findObsoletePartitions(new TableName("health.test_5"), 30);
    }

    @Test
    public void deleteObsoletePartitionsWithErrors() throws Exception {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Rotation process of 'test_config_id_3' config failed");

        DataRotationTask internalDataRotationTask = mock(DataRotationTask.class);
        DataRotationTask failedInternalDataRotationTask = mock(DataRotationTask.class);
        DataRotationTask externalDataRotationTask = mock(DataRotationTask.class);
        when(configurationService.getConfigWithPresentRotationDays()).thenReturn(createLogShatterConfigs());
        when(externalDataRotationServiceFactory.create("clusterId_2"))
            .thenReturn(Optional.of(externalDataRotationService));
        when(externalDataRotationServiceFactory.create("clusterId_5")).thenReturn(Optional.empty());
        when(internalDataRotationService.findObsoletePartitions(new TableName("health.test_1"), 30))
            .thenReturn(internalDataRotationTask);
        when(internalDataRotationService.findObsoletePartitions(new TableName("health.test_3"), 30))
            .thenReturn(failedInternalDataRotationTask);
        when(externalDataRotationService.findObsoletePartitions(new TableName("health.test_2"), 30))
            .thenReturn(externalDataRotationTask);
        when(internalDataRotationService.deleteObsoletePartitions(failedInternalDataRotationTask))
            .thenThrow(new RuntimeException());

        logshatterDataRotationService.deleteObsoletePartitions();

        verifyFindObsoletePartitions();

        verify(internalDataRotationService, times(2)).deleteObsoletePartitions(any(DataRotationTask.class));
        verify(externalDataRotationService).deleteObsoletePartitions(externalDataRotationTask);
    }

    @Test
    public void deleteObsoletePartitionsForLostTables() throws Exception {
        DataRotationTask internalDataRotationTask1 = mock(DataRotationTask.class);
        DataRotationTask internalDataRotationTask2 = mock(DataRotationTask.class);

        String databaseName = "health";
        TableDescription tableDescription = TableDescription.createDefault(
            Arrays.asList(
                new Column("host", ColumnType.String),
                new Column("column1", ColumnType.String),
                new Column("column2", ColumnType.Int32)
            ));

        Collection<LogShatterConfig> configsWithLostTablesFromDb = List.of(
            LogShatterConfig.newBuilder()
                .setConfigId("test_config_id")
                .setDataClickHouseTable(
                    new ClickHouseTableDefinitionImpl(databaseName, "test_1", tableDescription.getColumns(),
                        tableDescription.getEngine())
                )
                .setDataRotationDays(30).build(),
            LogShatterConfig.newBuilder()
                .setConfigId("test_config_id")
                .setDataClickHouseTable(
                    new ClickHouseTableDefinitionImpl(databaseName, "test_2", tableDescription.getColumns(),
                        tableDescription.getEngine())
                )
                .setDataRotationDays(30).build()
        );

        when(configurationService.getConfigWithPresentRotationDays()).thenReturn(Collections.emptyList());
        when(configurationService.getConfigsWithLostTablesFromDb()).thenReturn(configsWithLostTablesFromDb);
        when(internalDataRotationService.findObsoletePartitions(new TableName("health.test_1"), 30))
            .thenReturn(internalDataRotationTask1);
        when(internalDataRotationService.findObsoletePartitions(new TableName("health.test_2"), 30))
            .thenReturn(internalDataRotationTask2);

        logshatterDataRotationService.deleteObsoletePartitions();

        verify(internalDataRotationService).findObsoletePartitions(new TableName("health.test_1"), 30);
        verify(internalDataRotationService).findObsoletePartitions(new TableName("health.test_2"), 30);

        verify(internalDataRotationService).deleteObsoletePartitions(internalDataRotationTask1);
        verify(internalDataRotationService).deleteObsoletePartitions(internalDataRotationTask2);
    }

    private Collection<LogShatterConfig> createLogShatterConfigs() {
        List<ClickHouseTableDefinition> tableDefinitions = createTableDefinitions();

        return Arrays.asList(
            LogShatterConfig.newBuilder()
                .setConfigId("test_config_id_1")
                .setDataClickHouseTable(tableDefinitions.get(0))
                .setDataRotationDays(30).build(),
            LogShatterConfig.newBuilder()
                .setConfigId("test_config_id_2")
                .setDataClickHouseTable(tableDefinitions.get(1))
                .setDataRotationDays(30)
                .setClickHouseClusterId("clusterId_2").build(),
            LogShatterConfig.newBuilder()
                .setConfigId("test_config_id_3")
                .setDataClickHouseTable(tableDefinitions.get(2))
                .setDataRotationDays(30).build(),
            LogShatterConfig.newBuilder()
                .setConfigId("test_config_id_4")
                .setDataClickHouseTable(tableDefinitions.get(3))
                .setDataRotationDays(30).build(),
            LogShatterConfig.newBuilder()
                .setConfigId("test_config_id_5")
                .setDataClickHouseTable(tableDefinitions.get(4))
                .setDataRotationDays(30)
                .setClickHouseClusterId("clusterId_5").build()
        );
    }

    private List<ClickHouseTableDefinition> createTableDefinitions() {
        String databaseName = "health";
        TableDescription tableDescription = TableDescription.createDefault(
            Arrays.asList(
                new Column("host", ColumnType.String),
                new Column("column1", ColumnType.String),
                new Column("column2", ColumnType.Int32)
            ));

        return Arrays.asList(
            new ClickHouseTableDefinitionImpl(databaseName, "test_1", tableDescription.getColumns(),
                tableDescription.getEngine()),
            new ClickHouseTableDefinitionImpl(databaseName, "test_2", tableDescription.getColumns(),
                tableDescription.getEngine()),
            new ClickHouseTableDefinitionImpl(databaseName, "test_3", tableDescription.getColumns(),
                tableDescription.getEngine()),
            new ClickHouseTableDefinitionImpl(databaseName, "test_4", tableDescription.getColumns(),
                tableDescription.getEngine()),
            new ClickHouseTableDefinitionImpl(databaseName, "test_5", tableDescription.getColumns(),
                tableDescription.getEngine())
        );
    }
}
