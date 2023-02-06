package ru.yandex.market.clickhouse.dealer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.clickhouse.ddl.ClickHouseCluster;
import ru.yandex.market.clickhouse.ddl.ClickHouseDdlService;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickhouse.ddl.TableName;
import ru.yandex.market.clickhouse.ddl.engine.DistributedEngine;
import ru.yandex.market.clickhouse.ddl.engine.MergeTree;
import ru.yandex.market.clickhouse.ddl.engine.ReplicatedMergeTree;
import ru.yandex.market.clickhouse.dealer.config.DealerClusterConfig;
import ru.yandex.market.clickhouse.dealer.config.DealerConfig;
import ru.yandex.market.clickhouse.dealer.config.DealerGlobalConfig;
import ru.yandex.market.clickhouse.dealer.state.DataMismatch;
import ru.yandex.market.clickhouse.dealer.state.DealerDao;
import ru.yandex.market.clickhouse.dealer.state.DealerState;
import ru.yandex.market.clickhouse.dealer.state.PartitionClickHouseState;
import ru.yandex.market.clickhouse.dealer.state.PartitionState;
import ru.yandex.market.clickhouse.dealer.state.PartitionYtState;
import ru.yandex.market.clickhouse.dealer.tm.TmYt2ClickHouseCopyTask;
import ru.yandex.market.rotation.DataRotationService;
import ru.yandex.market.rotation.DataRotationTask;
import ru.yandex.market.rotation.ObsoletePartition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 07/06/2018
 */
public class DealerWorkerTest {

    private List<Column> columns = Arrays.asList(
        new Column("field1", ColumnType.Int32, "-1"),
        new Column("field2", ColumnType.String, null),
        new Column("date", ColumnType.Date, null)
    );

    private String clickHouseCluster = "market_health";
    private String clickHouseClusterNext = "market_health_next";

    private DealerConfig config = DealerConfig.newBuilder()
        .withGlobalConfig(
            DealerGlobalConfig.newBuilder()
                .withTempDatabase(null)
                .withTmQueueName("tmQueueName")
                .withClusterConfigs(
                    ImmutableSortedMap.of(
                        clickHouseCluster,
                        DealerClusterConfig.newBuilder()
                            .withClusterId(clickHouseCluster)
                            .withClusterForDdlApply(clickHouseClusterNext)
                            .build()
                    )
                ).build()
        )
        .withYtCluster("hahn")
        .withYtPath("//home/market/production/mstat/dictionaries/market_offers_ch")
        .withClickHouseTmCluster("market-clickhouse-testing", "user42", "secret")
        .withTableName("db.table")
        .withColumns(columns)
        .withOrderBy("field1", "field2")
        .withShardingKeys("field2")
        .withYtPartitionNameColumn("date")
        .withPartitionBy("toYYYYMM(date)")
        .withTmQueueName("special-market-tm-queue")
        .withRotationPeriodDays(14)
        .build();

    private DealerWorker getDealerWorker() {
        return getDealerWorker(getState());
    }

    private DealerWorker getDealerWorker(DealerState dealerState) {
        return getDealerWorker(dealerState, null);
    }

    private DealerWorker getDealerWorker(DealerState dealerState, DealerDao dealerDao) {
        ClickHouseDdlService ddlService = Mockito.mock(ClickHouseDdlService.class);
        ClickHouseCluster cluster = Mockito.mock(ClickHouseCluster.class);
        Mockito.when(ddlService.getCluster()).thenReturn(cluster);

        DealerWorker worker = new DealerWorker(config, null, null, dealerDao, dealerState);
        worker.setDdlService(ddlService);
        return worker;
    }

    @Test
    public void testTmTask() {

        TmYt2ClickHouseCopyTask actualTask = getDealerWorker().createCopyTask("201701");

        TmYt2ClickHouseCopyTask expectedTask = TmYt2ClickHouseCopyTask.newBuilder()
            .withClickHouseCluster("market-clickhouse-testing")
            .withClickHouseTable("db.table_tmp_lr") //Inserts to replicated temp table
            .withCredentials("user42", "secret")
            .withShardingKey("field2")
            .withYtCluster("hahn")
            .withDateColumn("date")
            .withYtPath("//home/market/production/mstat/dictionaries/market_offers_ch/201701")
            .withTmQueueName("special-market-tm-queue")
            .withLabel("market-clickhouse-testing")
            .withResetState(true)
            .build();

        Assert.assertEquals(expectedTask, actualTask);
    }

    @Test
    public void getQuorumTest() {
        ClickHouseDdlService ddlService = Mockito.mock(ClickHouseDdlService.class);
        ClickHouseCluster cluster = Mockito.mock(ClickHouseCluster.class);
        Mockito.when(ddlService.getCluster()).thenReturn(cluster);

        DealerWorker worker = getDealerWorker();
        worker.setDdlService(ddlService);

        List<ClickHouseCluster.Server> twoReplicas = ImmutableList.of(
            getServer(2, 1),
            getServer(2, 2),
            getServer(3, 1),
            getServer(3, 2),
            getServer(3, 3)
        );

        List<ClickHouseCluster.Server> threeReplicas = new ArrayList<>(twoReplicas);
        threeReplicas.add(getServer(2, 3));

        List<ClickHouseCluster.Server> oneReplica = new ArrayList<>(twoReplicas);
        oneReplica.add(getServer(1, 1));


        Mockito.when(cluster.getServers()).thenReturn(
            oneReplica,
            twoReplicas,
            threeReplicas,
            Collections.emptyList()
        );

        Assert.assertEquals(TmYt2ClickHouseCopyTask.Quorum.AT_LEAST_ONE, worker.getQuorum());
        Assert.assertEquals(TmYt2ClickHouseCopyTask.Quorum.AT_LEAST_ONE, worker.getQuorum());
        Assert.assertEquals(TmYt2ClickHouseCopyTask.Quorum.MAJORITY, worker.getQuorum());
        Assert.assertEquals(TmYt2ClickHouseCopyTask.Quorum.MAJORITY, worker.getQuorum());
    }

    private ClickHouseCluster.Server getServer(int shardNumber, int replicaNumber) {
        return new ClickHouseCluster.Server("host", shardNumber, replicaNumber);
    }

    @Test
    public void testDdl() {
        DealerWorker worker = getDealerWorker();
        worker.setRuntimeOptions(new DealerWorker.ClickHouseRuntimeParams(Collections.emptyList(), clickHouseCluster, 42, false));
        Set<ClickHouseTableDefinition> actualTableDefinitions = new HashSet<>(worker.createTableDefinitions());


        MergeTree mergeTree = MergeTree.fromOldDefinition("date", Arrays.asList("field1", "field2"));
        ClickHouseTableDefinition expectedDataTable = new ClickHouseTableDefinitionImpl(
            "db.table_lr", columns, new ReplicatedMergeTree(mergeTree, "db", "table_lr", config.getZookeeperPrefix())
        );
        ClickHouseTableDefinition expectedTmpDataTable = new ClickHouseTableDefinitionImpl(
            "db.table_tmp_lr", columns, new ReplicatedMergeTree(mergeTree, "db", "table_tmp_lr", config.getZookeeperPrefix())
        );
        ClickHouseTableDefinition expectedDistTable = new ClickHouseTableDefinitionImpl(
            "db.table", columns, new DistributedEngine(clickHouseCluster, "db", "table_lr", null)
        );
        ClickHouseTableDefinition expectedDistTmpTable = new ClickHouseTableDefinitionImpl(
            "db.table_tmp", columns, new DistributedEngine(clickHouseCluster, "db", "table_tmp_lr", null)
        );

        Set<ClickHouseTableDefinition> expectedTableDefinitions = new HashSet<>(Arrays.asList(
            expectedDataTable, expectedTmpDataTable, expectedDistTable, expectedDistTmpTable
        ));

        Assert.assertEquals(expectedTableDefinitions, actualTableDefinitions);
    }

    @Test
    public void correctClusterForTableDefinition() {
        DealerWorker worker = getDealerWorker();
        worker.setRuntimeOptions(new DealerWorker.ClickHouseRuntimeParams(Collections.emptyList(), clickHouseCluster, 42, false));
        List<ClickHouseTableDefinition> tableDefinitions = worker.createTableDefinitions();

        Assert.assertFalse(doesDefinitionsContainCluster(tableDefinitions, clickHouseClusterNext));
        Assert.assertTrue(doesDefinitionsContainCluster(tableDefinitions, clickHouseCluster));
    }

    private boolean doesDefinitionsContainCluster(List<ClickHouseTableDefinition> tableDefinitions, String cluster) {
        return tableDefinitions.stream()
            .anyMatch(tb -> (
                tb.getEngine().createEngineDDL().contains(cluster)
            ));
    }

    @Test
    public void testStatusesLimit() {
        /* test expects newest 3 CH partitions which suitable for copy or update operations */
        Multimap<String, PartitionState> states = getDealerWorker().getTransferStates();
        Assert.assertSame(0, states.get("1801").size());
        Assert.assertSame(0, states.get("1802").size());
        Assert.assertSame(3, states.get("1803").size());
        Assert.assertSame(4, states.get("1805").size());
        Assert.assertSame(0, states.get("1806").size());
        Assert.assertSame(3, states.get("1807").size());
        Assert.assertSame(0, states.get("1808").size());
    }

    private DealerState getState() {
        DealerState state = Mockito.mock(DealerState.class);
        Mockito.when(state.getPartitionStates()).thenReturn(new ArrayList<>(getPartitions()));
        return state;
    }

    private List<PartitionState> getPartitions() {
        return Arrays.asList(
            getPartitionState("180101", 100_000, "1801", PartitionState.Status.TRANSFERRED_NEED_UPDATE),
            getPartitionState("180102", 100_000, "1801", PartitionState.Status.NEW),
            getPartitionState("180301", 100_000, "1803", PartitionState.Status.NEW),
            getPartitionState("180302", 100_000, "1803", PartitionState.Status.NEW),
            getPartitionState("180303", 100_000, "1803", PartitionState.Status.TRANSFERRED),
            getPartitionState("180201", 200_000, "1802", PartitionState.Status.TRANSFERRED_DATA_MISMATCH),
            getPartitionState("180601", 100_000, "1806", PartitionState.Status.TRANSFERRED),
            getPartitionState("180501", 200_000, "1805", PartitionState.Status.TRANSFERRED_DATA_MISMATCH),
            getPartitionState("180502", 100_000, "1805", PartitionState.Status.TRANSFERRED_NEED_UPDATE),
            getPartitionState("180503", 100_000, "1805", PartitionState.Status.NEW),
            getPartitionState("180504", 100_000, "1805", PartitionState.Status.TRANSFERRED),
            getPartitionState("180701", 100_000, "1807", PartitionState.Status.TRANSFERRED_NEED_UPDATE),
            getPartitionState("180702", 100_000, "1807", PartitionState.Status.SKIPPED),
            getPartitionState("180703", 100_000, "1807", PartitionState.Status.YT_DELETED),
            getPartitionState("180801", 100_000, "1808", PartitionState.Status.YT_DELETED),
            getPartitionState("2019-01-02", 100_000, "'2019-01-02'", PartitionState.Status.ROTATING),
            getPartitionState("2019-01-05", 100_000, "'2019-01-05'", PartitionState.Status.ROTATING)
        );
    }

    private PartitionState getPartitionState(
        String ytPartition, long rowCount, String clickHousePartition, PartitionState.Status status
    ) {
        return new PartitionState(
            ytPartition, clickHousePartition,
            new PartitionClickHouseState(
                new PartitionYtState(null, rowCount, null, 0), null, null
            ),
            new PartitionYtState(null, rowCount, null, 0), status
        );
    }

    @Test
    public void validateTransferredPartitionsFromClickHouse() {
        DealerState dealerState = getState();
        DealerDao dealerDao = Mockito.mock(DealerDao.class);

        Collection<DataMismatch> savedDataMismatches = new ArrayList<>();
        Mockito.doAnswer((e) -> {
            savedDataMismatches.add(e.getArgument(0));
            return null;
        }).when(dealerDao).insert(Mockito.any());

        DealerWorker dealerWorker = Mockito.spy(getDealerWorker(dealerState, dealerDao));

        Collection<PartitionState> states = dealerState.getPartitionStates();
        Supplier<Stream<PartitionState>> stateStream = () -> states.stream()
            .filter(st -> st.getStatus() == PartitionState.Status.TRANSFERRED_DATA_MISMATCH);

        /* Yt partitions from clickHouse */
        Map<String, Long> ytPartitionRowsInCh = ImmutableMap.of(
            "180504", 100_002L,
            "180601", 100_001L,
            "424242", 424_424L,
            "180503", 100_000_000L,
            "100501", 200_001L
        );

        long numberOfInvalidStates = stateStream.get().count();

        dealerWorker.validateTransferredPartitionsFromClickHouse(
            ytPartitionRowsInCh,
            states.stream()
                .filter(st -> st.getStatus() == PartitionState.Status.TRANSFERRED).collect(Collectors.toList())
        );
        /* Excepted invalid states count is 3, because 2 states have different rowCount,
           and 1 is absent in clickHouse, but available in dealer states as  TRANSFERRED (180303) */
        Assert.assertEquals(3, stateStream.get().count() - numberOfInvalidStates);

        boolean areStatesInvalid = stateStream.get()
            .filter(st -> "180504".equals(st.getYtPartition()) || "180601".equals(st.getYtPartition()))
            .allMatch(st -> st.getStatus() == PartitionState.Status.TRANSFERRED_DATA_MISMATCH);

        Assert.assertTrue(areStatesInvalid);
        Assert.assertEquals(3, savedDataMismatches.size());
    }

    @Test
    public void validateRotatingPartitionsFromClickHouse() {
        DealerState dealerState = getState();
        DealerDao dealerDao = Mockito.mock(DealerDao.class);
        DealerWorker dealerWorker = Mockito.spy(getDealerWorker(dealerState, dealerDao));
        Collection<PartitionState> states = dealerState.getPartitionStates();

        /* Yt partitions from clickHouse */
        Map<String, Long> ytPartitionRowsInCh = ImmutableMap.of(
            "2019-07-10", 100_002L,
            "2019-07-11", 100_001L,
            "2019-07-12", 424_424L
        );

        List<PartitionState> rotatingPartitions = states.stream()
            .filter(st -> st.getStatus() == PartitionState.Status.ROTATING).collect(Collectors.toList());
        dealerWorker.validateRotatingPartitionsFromClickHouse(
            ytPartitionRowsInCh,
            rotatingPartitions
        );

        Assert.assertTrue(rotatingPartitions.stream().noneMatch(st -> st.getStatus() == PartitionState.Status.ROTATING));
        Assert.assertTrue(rotatingPartitions.stream().allMatch(st -> st.getStatus() == PartitionState.Status.ROTATED));
    }

    @Test
    public void testRunUpdateProcess() throws Exception {
        DealerState state = new DealerState(null);
        getPartitions().forEach(state::putPartitionState);

        DealerDao dao = Mockito.mock(DealerDao.class);
        Mockito.doNothing().when(dao).save(Mockito.any());

        DealerWorker dealerWorker = Mockito.spy(getDealerWorker(state, dao));
        Mockito.doNothing().when(dealerWorker).doOperation(Mockito.any());

        Multimap<String, PartitionState> states = dealerWorker.getTransferStates();
        Collection<PartitionState> partitionStates = states.get("1807");
        long partitionStateSize = partitionStates.size();

        Assert.assertTrue(partitionStates.stream().anyMatch(s -> s.getStatus() == PartitionState.Status.YT_DELETED));
        dealerWorker.runUpdateProcess("1807", partitionStates);

        /* YT_DELETED state should be gone */
        Collection<PartitionState> actualStates = state.getPartitionStates("1807");
        Assert.assertEquals(partitionStateSize - 1, actualStates.size());
        Assert.assertFalse(actualStates.stream().anyMatch(s -> s.getStatus() == PartitionState.Status.YT_DELETED));
    }

    @Test
    public void runRotationMarkingTest() throws Exception {
        DealerState state = Mockito.mock(DealerState.class);
        DealerDao dealerDao = Mockito.mock(DealerDao.class);
        Mockito.doNothing().when(dealerDao).save(Mockito.any());
        DealerWorker dealerWorker = getDealerWorker(state, dealerDao);
        DataRotationService dataRotationService = Mockito.mock(DataRotationService.class);
        Collection<ObsoletePartition> obsoletePartitions = Arrays.asList(
            new ObsoletePartition("h1", new TableName("d1.t1"), "2019-01-01"),
            new ObsoletePartition("h1", new TableName("d1.t1"), "2019-01-02")
        );
        DataRotationTask dataRotationTask = new DataRotationTask(obsoletePartitions, 14, "2019-01-03");
        Mockito.doReturn(dataRotationTask).when(dataRotationService).findObsoletePartitions(config.getDataTable(), config.getRotationPeriodDays());
        dealerWorker.setDataRotationService(dataRotationService);

        PartitionState partition1 = new PartitionState("2019-01-01", "2019-01-01", null, null, PartitionState.Status.NEW);
        PartitionState partition2 = new PartitionState("2019-01-02", "2019-01-02", null, null, PartitionState.Status.TRANSFERRED_NEED_UPDATE);
        List<PartitionState> partitions = Arrays.asList(partition1, partition2);

        Mockito.when(state.getPartitionStates()).thenReturn(partitions);
        Mockito.when(state.getPartitionStates("2019-01-01")).thenReturn(Collections.singletonList(partition1));
        Mockito.when(state.getPartitionStates("2019-01-02")).thenReturn(Collections.singletonList(partition2));
        Mockito.doReturn(Collections.emptyList()).when(dataRotationService).deleteObsoletePartitions(dataRotationTask);


        Assertions.assertThatCode(() -> {
            dealerWorker.runRotation();
            Assert.assertTrue(partitions.stream().allMatch(p -> p.getStatus() == PartitionState.Status.ROTATED));
        }).doesNotThrowAnyException();
    }
}