package ru.yandex.market.rotation;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import ru.yandex.market.clickhouse.ddl.ClickHouseCluster;
import ru.yandex.market.clickhouse.ddl.ClickHouseDdlService;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickhouse.ddl.TableName;
import ru.yandex.market.clickhouse.ddl.engine.ReplicatedMergeTree;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 2019-03-19
 */
public class DataRotationServiceTest {

    private DataRotationService dataRotationService;
    ClickHouseDdlService ddlService;
    TableName dataTableName = new TableName("mbi.temptable_lr");

    @Before
    public void setup() {
        ddlService = Mockito.mock(ClickHouseDdlService.class);
        dataRotationService = Mockito.spy(new DataRotationService(ddlService));
    }

    @Test
    public void findObsoletePartitions() {
        checkThrowingExceptionOnIncorrectRotationDays(-1);
        checkThrowingExceptionOnIncorrectRotationDays(0);


        Mockito.doReturn("201905").when(dataRotationService).getPivotPartition(dataTableName, 60);
        ClickHouseCluster cluster = Mockito.mock(ClickHouseCluster.class);
        Mockito.when(ddlService.getCluster()).thenReturn(cluster);
        Mockito.when(cluster.getAllHosts()).thenReturn(Collections.singletonList("host1"));
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(ddlService.getHostJdbcTemplate(Mockito.anyString())).thenReturn(jdbcTemplate);
        Mockito.doAnswer(invocation -> {
            String query = (String) invocation.getArguments()[0];
            RowCallbackHandler rc = (RowCallbackHandler) invocation.getArguments()[1];

            String pivotPartition = "201905";
            Assert.assertTrue(query.contains(pivotPartition));
            ResultSet rs = Mockito.mock(ResultSet.class);
            Mockito.when(rs.getString("p")).thenReturn("201904", "201903", "201902", "201901");

            rc.processRow(rs);
            rc.processRow(rs);
            rc.processRow(rs);
            rc.processRow(rs);

            return null;
        })
            .when(jdbcTemplate).query(Mockito.anyString(), Mockito.any(RowCallbackHandler.class));

        List<ObsoletePartition> expectedPartitions = getObsoletePartitions("201904", "201903", "201902", "201901");
        findObsoletePartitions(dataTableName, 60, expectedPartitions);
    }

    @Test
    public void noFoundObsoletePartitionsOnTuplePartition() {
        Mockito.doReturn("tuple()").when(dataRotationService).getPivotPartition(dataTableName, 60);
        findObsoletePartitions(dataTableName, 60, Collections.emptyList());
    }

    private void checkThrowingExceptionOnIncorrectRotationDays(int rotationPerioDays) {
        Assertions.assertThatThrownBy(() -> findObsoletePartitions(dataTableName, rotationPerioDays, Collections.emptyList()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(String.format("'%d' <= 0", rotationPerioDays));
    }

    private List<ObsoletePartition> getObsoletePartitions(String... partitions) {
        List<ObsoletePartition> expectedPartitions = new ArrayList<>();
        for (int i = 0; i < partitions.length; i++) {
            expectedPartitions.add(new ObsoletePartition("host1", dataTableName, partitions[i]));
        }
        return expectedPartitions;
    }

    private void findObsoletePartitions(TableName tableName,
                                        int rotationPeriodDays,
                                        Collection<ObsoletePartition> expectedPartitions) {

        Assert.assertArrayEquals(
            expectedPartitions.toArray(),
            dataRotationService.findObsoletePartitions(tableName, rotationPeriodDays).getObsoletePartitions().toArray()
        );
    }

    @Test
    public void deleteObsoletePartitionsTest() throws Exception {
        List<ObsoletePartition> obsoletePartitions = getObsoletePartitions(getPartition(5), getPartition(6));
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.doReturn(jdbcTemplate).when(ddlService).getHostJdbcTemplate(Mockito.anyString());
        int updateCountStub = 1;
        Mockito.doReturn(updateCountStub).when(jdbcTemplate).update(Mockito.anyString());

        Collection<ObsoletePartition> deletedObsoletePartitions = dataRotationService.deleteObsoletePartitions(
            new DataRotationTask(obsoletePartitions, 14, getPartition(14))
        );

        Assert.assertEquals(obsoletePartitions.size(), deletedObsoletePartitions.size());


        Mockito.doThrow(new RuntimeException()).when(jdbcTemplate).update(Mockito.anyString());
        Assertions.assertThatThrownBy(() -> dataRotationService.deleteObsoletePartitions(
            new DataRotationTask(obsoletePartitions, 14, getPartition(14))
        )).isInstanceOf(Exception.class).hasMessageContaining("Rotation process: cannot drop partition");
    }

    private String getPartition(int daysAgo) {
        return ClickhousePartitionType.DATE.format(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
    }


    @Test
    public void getPivotPartition() {
        ClickHouseTableDefinition clickHouseTableDefinition = Mockito.mock(ClickHouseTableDefinition.class);
        Mockito.when(dataRotationService.getClickHouseTableDefinition(dataTableName)).thenReturn(clickHouseTableDefinition);
        ReplicatedMergeTree replicatedMergeTree = Mockito.mock(ReplicatedMergeTree.class);
        Mockito.when(clickHouseTableDefinition.getEngine()).thenReturn(replicatedMergeTree);

        Mockito.when(replicatedMergeTree.getPartitionBy()).thenReturn("tuple()");
        Assert.assertEquals("tuple()", dataRotationService.getPivotPartition(dataTableName, 60));

        Mockito.when(replicatedMergeTree.getPartitionBy()).thenReturn("vtuple( )");
        Assertions.assertThatThrownBy(() -> dataRotationService.getPivotPartition(dataTableName, 60))
            .hasMessageContaining("Unsupported clickhouse partition expression");

        Mockito.when(clickHouseTableDefinition.getColumn("date")).thenReturn(new Column("date", ColumnType.Date));
        Mockito.when(replicatedMergeTree.getPartitionBy()).thenReturn("toYYYYMM(date)");
        Instant pivotPartition = Instant.now().minus(60, ChronoUnit.DAYS);
        String expectedResult = ClickhousePartitionType.TO_YYYYMM.format(pivotPartition);
        Assert.assertEquals(expectedResult, dataRotationService.getPivotPartition(dataTableName, 60));

        Mockito.when(clickHouseTableDefinition.getColumn("username")).thenReturn(new Column("username", ColumnType.String));
        Mockito.when(replicatedMergeTree.getPartitionBy()).thenReturn("toYYYYMM(username)");
        Assertions.assertThatThrownBy(() -> dataRotationService.getPivotPartition(dataTableName, 60))
            .hasMessageContaining("Unsupported column type for rotation.");
    }
}